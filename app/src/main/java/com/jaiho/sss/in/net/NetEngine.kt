package com.jaiho.sss.in.net

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.jaiho.sss.in.data.BotRuntime
import com.jaiho.sss.in.util.L
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val FLAG_FIN = 0x01
private const val FLAG_SYN = 0x02
private const val FLAG_RST = 0x04
private const val FLAG_PSH = 0x08
private const val FLAG_ACK = 0x10

private val EMPTY = ByteArray(0)
private const val MASK = 0xFFFFFFFFL

/**
 * Reads raw IP packets from the tunnel, relays TCP over real sockets and inspects the
 * WebSocket text frames coming back from the game server. Everything is wrapped so a
 * broken packet can never take the tunnel down.
 */
class NetEngine(
    private val vpn: VpnService,
    private val tun: ParcelFileDescriptor,
    private val multiplier: () -> Float
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val input = FileInputStream(tun.fileDescriptor)
    private val output = FileOutputStream(tun.fileDescriptor)
    private val writeLock = Any()
    private val conns = ConcurrentHashMap<Int, TcpConn>()

    fun start() {
        scope.launch { readLoop() }
    }

    fun stop() {
        try {
            scope.cancel()
        } catch (_: Throwable) {
        }
        conns.values.toList().forEach { conn ->
            try {
                conn.close()
            } catch (_: Throwable) {
            }
        }
        conns.clear()
        try {
            input.close()
        } catch (_: Throwable) {
        }
        try {
            output.close()
        } catch (_: Throwable) {
        }
        try {
            tun.close()
        } catch (_: Throwable) {
        }
    }

    private fun readLoop() {
        val buffer = ByteArray(32767)
        while (scope.isActive) {
            val length = try {
                input.read(buffer)
            } catch (t: Throwable) {
                if (scope.isActive) L.e("tun read ended", t)
                break
            }
            if (length <= 0) continue
            try {
                handle(buffer, length)
            } catch (t: Throwable) {
                L.e("packet handling failed", t)
            }
        }
    }

    private fun handle(buf: ByteArray, length: Int) {
        if (length < 20) return
        if (((buf[0].toInt() and 0xF0) shr 4) != 4) return
        val ihl = (buf[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8) return
        val total = readShort(buf, 2).let { if (it <= 0 || it > length) length else it }
        when (buf[9].toInt() and 0xFF) {
            6 -> handleTcp(buf, ihl, total)
            17 -> handleUdp(buf, ihl, total)
            else -> Unit
        }
    }

    private fun handleTcp(buf: ByteArray, ihl: Int, total: Int) {
        if (total < ihl + 20) return
        val srcIp = buf.copyOfRange(12, 16)
        val dstIp = buf.copyOfRange(16, 20)
        val srcPort = readShort(buf, ihl)
        val dstPort = readShort(buf, ihl + 2)
        val seq = readInt(buf, ihl + 4)
        val flags = buf[ihl + 13].toInt() and 0xFF
        val tcpHeader = ((buf[ihl + 12].toInt() and 0xF0) shr 4) * 4
        val payloadLength = total - ihl - tcpHeader
        val payload = if (payloadLength > 0 && ihl + tcpHeader + payloadLength <= total) {
            buf.copyOfRange(ihl + tcpHeader, ihl + tcpHeader + payloadLength)
        } else {
            EMPTY
        }

        val existing = conns[srcPort]

        if (flags and FLAG_SYN != 0) {
            existing?.close()
            val conn = TcpConn(srcIp, srcPort, dstIp, dstPort, seq)
            conns[srcPort] = conn
            conn.open()
            return
        }

        if (existing == null) {
            sendReset(srcIp, srcPort, dstIp, dstPort, seq, flags)
            return
        }

        if (flags and (FLAG_FIN or FLAG_RST) != 0) {
            existing.close()
            conns.remove(srcPort, existing)
            return
        }

        if (payload.isNotEmpty()) existing.clientData(seq, payload)
    }

    private fun handleUdp(buf: ByteArray, ihl: Int, total: Int) {
        if (total < ihl + 8) return
        val srcIp = buf.copyOfRange(12, 16)
        val dstIp = buf.copyOfRange(16, 20)
        val srcPort = readShort(buf, ihl)
        val dstPort = readShort(buf, ihl + 2)
        if (total <= ihl + 8) return
        val payload = buf.copyOfRange(ihl + 8, total)
        if (payload.isEmpty()) return

        scope.launch {
            var socket: DatagramSocket? = null
            try {
                val socket1 = DatagramSocket()
                socket = socket1
                vpn.protect(socket1)
                socket1.soTimeout = 5000
                val target = InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort)
                socket1.send(DatagramPacket(payload, payload.size, target))
                val replyBuffer = ByteArray(4096)
                val reply = DatagramPacket(replyBuffer, replyBuffer.size)
                socket1.receive(reply)
                if (reply.length > 0) {
                    val data = replyBuffer.copyOfRange(0, reply.length)
                    BotRuntime.bumpNet(data.size.toLong(), 0L)
                    writePacket(Packets.udp(dstIp, dstPort, srcIp, srcPort, data))
                }
            } catch (t: Throwable) {
                L.e("udp forward failed", t)
            } finally {
                try {
                    socket?.close()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun sendReset(
        clientIp: ByteArray,
        clientPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        seq: Int,
        flags: Int
    ) {
        try {
            val ack = if (flags and FLAG_ACK != 0) 0 else (seq + 1)
            val packet = Packets.tcp(
                dstIp, dstPort, clientIp, clientPort,
                0, ack, FLAG_RST or FLAG_ACK, EMPTY
            )
            writePacket(packet)
        } catch (t: Throwable) {
            L.e("reset send failed", t)
        }
    }

    private fun writePacket(packet: ByteArray) {
        try {
            synchronized(writeLock) { output.write(packet) }
        } catch (t: Throwable) {
            L.e("tun write failed", t)
        }
    }

    private fun readShort(b: ByteArray, at: Int): Int =
        (((b[at].toInt() and 0xFF) shl 8) or (b[at + 1].toInt() and 0xFF))

    private fun readInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or
            ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or
            (b[at + 3].toInt() and 0xFF)

    private inner class TcpConn(
        private val clientIp: ByteArray,
        private val clientPort: Int,
        private val dstIp: ByteArray,
        private val dstPort: Int,
        startSeq: Int
    ) {
        private var clientSeq: Long = ((startSeq.toLong() and MASK) + 1L) and MASK
        private var serverSeq: Long = (Random.nextInt(1, Int.MAX_VALUE).toLong()) and MASK
        private var socket: Socket? = null
        private var outStream: OutputStream? = null
        private var readJob: Job? = null
        private var headerBuffer: ByteArrayOutputStream? = null
        private var upgrading = false
        private var wsActive = false
        private val wsFilter = WsFilter()

        @Volatile
        private var closed = false

        fun open() {
            readJob = scope.launch {
                try {
                    val candidate = Socket()
                    candidate.tcpNoDelay = true
                    vpn.protect(candidate)
                    candidate.connect(InetSocketAddress(InetAddress.getByAddress(dstIp), dstPort), 9000)
                    if (closed) {
                        try {
                            candidate.close()
                        } catch (_: Throwable) {
                        }
                        return@launch
                    }
                    socket = candidate
                    outStream = candidate.getOutputStream()
                    sendPacket(FLAG_SYN or FLAG_ACK, EMPTY, serverSeq, clientSeq)
                    serverSeq = (serverSeq + 1L) and MASK
                    readFromServer(candidate)
                } catch (t: Throwable) {
                    L.e("tcp connect failed", t)
                    sendPacket(FLAG_RST or FLAG_ACK, EMPTY, serverSeq, clientSeq)
                    close()
                }
            }
        }

        private suspend fun readFromServer(candidate: Socket) {
            val stream = candidate.getInputStream()
            val buffer = ByteArray(16384)
            while (!closed) {
                val read = try {
                    stream.read(buffer)
                } catch (t: Throwable) {
                    if (!closed) L.e("tcp read failed", t)
                    -1
                }
                if (read < 0) break
                if (read == 0) continue
                BotRuntime.bumpNet(read.toLong(), 0L)
                val chunk = buffer.copyOfRange(0, read)
                val outgoing = processServerBytes(chunk)
                if (outgoing.isNotEmpty()) sendToClient(outgoing)
            }
            sendPacket(FLAG_FIN or FLAG_ACK, EMPTY, serverSeq, clientSeq)
            close()
        }

        fun clientData(seq: Int, payload: ByteArray) {
            val stream = outStream
            if (stream == null || closed) return
            try {
                stream.write(payload)
                stream.flush()
                BotRuntime.bumpNet(payload.size.toLong(), 0L)
                clientSeq = (((seq.toLong() and MASK) + payload.size.toLong()) and MASK)
                if (!upgrading && !wsActive) {
                    val text = String(payload, Charsets.UTF_8).lowercase()
                    if (text.contains("upgrade: websocket") || text.contains("sec-websocket-key")) {
                        upgrading = true
                    }
                }
                sendPacket(FLAG_ACK, EMPTY, serverSeq, clientSeq)
            } catch (t: Throwable) {
                L.e("tcp write failed", t)
            }
        }

        /**
         * Feeds the header block through untouched until the 101 answer is complete, then
         * pushes every later byte through the frame filter.
         */
        private fun processServerBytes(chunk: ByteArray): ByteArray {
            if (wsActive) return wsFilter.feed(chunk)
            if (!upgrading) return chunk
            val holder = headerBuffer ?: ByteArrayOutputStream().also { headerBuffer = it }
            holder.write(chunk)
            val all = holder.toByteArray()
            val end = headerEnd(all)
            if (end < 0) {
                if (all.size > 16384) {
                    upgrading = false
                    headerBuffer = null
                    return all
                }
                return EMPTY
            }
            wsActive = true
            headerBuffer = null
            val header = all.copyOfRange(0, end)
            val rest = all.copyOfRange(end, all.size)
            val filtered = if (rest.isEmpty()) EMPTY else wsFilter.feed(rest)
            if (filtered.isEmpty()) return header
            val merged = ByteArray(header.size + filtered.size)
            System.arraycopy(header, 0, merged, 0, header.size)
            System.arraycopy(filtered, 0, merged, header.size, filtered.size)
            return merged
        }

        private fun headerEnd(data: ByteArray): Int {
            var i = 0
            while (i + 3 < data.size) {
                if (data[i] == 13.toByte() && data[i + 1] == 10.toByte() &&
                    data[i + 2] == 13.toByte() && data[i + 3] == 10.toByte()
                ) {
                    return i + 4
                }
                i++
            }
            return -1
        }

        private fun sendToClient(bytes: ByteArray) {
            var position = 0
            while (position < bytes.size) {
                val size = minOf(1200, bytes.size - position)
                val chunk = bytes.copyOfRange(position, position + size)
                sendPacket(FLAG_ACK or FLAG_PSH, chunk, serverSeq, clientSeq)
                serverSeq = (serverSeq + size.toLong()) and MASK
                position += size
            }
        }

        private fun sendPacket(flags: Int, payload: ByteArray, seq: Long, ack: Long) {
            try {
                val packet = Packets.tcp(
                    dstIp, dstPort, clientIp, clientPort,
                    (seq and MASK).toInt(), (ack and MASK).toInt(), flags, payload
                )
                writePacket(packet)
            } catch (t: Throwable) {
                L.e("tcp send failed", t)
            }
        }

        fun close() {
            if (closed) return
            closed = true
            try {
                readJob?.cancel()
            } catch (_: Throwable) {
            }
            readJob = null
            try {
                socket?.close()
            } catch (_: Throwable) {
            }
            socket = null
            outStream = null
            conns.remove(clientPort, this)
        }

        /** Reassembles WebSocket frames out of the byte stream and rewrites text frames. */
        private inner class WsFilter {
            private var pending = ByteArrayOutputStream()

            fun feed(chunk: ByteArray): ByteArray {
                pending.write(chunk)
                val data = pending.toByteArray()
                val result = ByteArrayOutputStream()
                var position = 0
                while (true) {
                    if (data.size - position < 2) break
                    val first = data[position].toInt() and 0xFF
                    val second = data[position + 1].toInt() and 0xFF
                    val opcode = first and 0x0F
                    var headerLength = 2
                    var length = (second and 0x7F).toLong()
                    if (length == 126L) {
                        if (data.size - position < 4) break
                        length = (((data[position + 2].toInt() and 0xFF) shl 8) or
                            (data[position + 3].toInt() and 0xFF)).toLong()
                        headerLength = 4
                    } else if (length == 127L) {
                        if (data.size - position < 10) break
                        var wide = 0L
                        for (i in 0 until 8) {
                            wide = (wide shl 8) or (data[position + 2 + i].toLong() and 0xFF)
                        }
                        length = wide
                        headerLength = 10
                    }
                    if (length > 1_000_000L) {
                        result.write(data, position, data.size - position)
                        position = data.size
                        break
                    }
                    if (data.size - position < headerLength + length.toInt()) break
                    val start = position + headerLength
                    val end = start + length.toInt()
                    if (opcode == 0x1) {
                        val text = String(data, start, length.toInt(), Charsets.UTF_8)
                        val rewritten = FrameRewriter.rewrite(text, multiplier())
                        if (rewritten != null) {
                            val body = rewritten.toByteArray(Charsets.UTF_8)
                            result.write(encodeFrame(first, body))
                            BotRuntime.bumpNet(0L, 1L)
                        } else {
                            result.write(data, position, headerLength + length.toInt())
                        }
                    } else {
                        result.write(data, position, headerLength + length.toInt())
                    }
                    position = end
                }
                pending = ByteArrayOutputStream()
                if (position < data.size) pending.write(data, position, data.size - position)
                return result.toByteArray()
            }

            private fun encodeFrame(first: Int, body: ByteArray): ByteArray {
                val head = ByteArrayOutputStream()
                head.write(first or 0x80)
                when {
                    body.size < 126 -> head.write(body.size)
                    body.size <= 0xFFFF -> {
                        head.write(126)
                        head.write((body.size shr 8) and 0xFF)
                        head.write(body.size and 0xFF)
                    }
                    else -> {
                        head.write(127)
                        for (i in 7 downTo 0) {
                            head.write(((body.size.toLong() shr (8 * i)) and 0xFF).toInt())
                        }
                    }
                }
                val header = head.toByteArray()
                val frame = ByteArray(header.size + body.size)
                System.arraycopy(header, 0, frame, 0, header.size)
                System.arraycopy(body, 0, frame, header.size, body.size)
                return frame
            }
        }
    }
}
