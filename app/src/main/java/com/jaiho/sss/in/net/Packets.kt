=== ABOUT: Builds the raw internet parcels the phone sends back into the game connection ===
package com.jaiho.sss.in.net

object Packets {

    private fun putShort(p: ByteArray, at: Int, value: Int) {
        p[at] = ((value shr 8) and 0xFF).toByte()
        p[at + 1] = (value and 0xFF).toByte()
    }

    private fun putInt(p: ByteArray, at: Int, value: Int) {
        p[at] = ((value shr 24) and 0xFF).toByte()
        p[at + 1] = ((value shr 16) and 0xFF).toByte()
        p[at + 2] = ((value shr 8) and 0xFF).toByte()
        p[at + 3] = (value and 0xFF).toByte()
    }

    private fun sumOf(buf: ByteArray, off: Int, len: Int, start: Long): Long {
        var sum = start
        var i = off
        val end = off + len
        while (i + 1 < end) {
            sum += (((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < end) sum += ((buf[i].toInt() and 0xFF) shl 8).toLong()
        return sum
    }

    private fun fold(start: Long): Int {
        var sum = start
        while ((sum shr 16) != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv().toInt()) and 0xFFFF
    }

    private fun pseudo(src: ByteArray, dst: ByteArray, proto: Int, length: Int): ByteArray {
        val head = ByteArray(12)
        System.arraycopy(src, 0, head, 0, 4)
        System.arraycopy(dst, 0, head, 4, 4)
        head[8] = 0
        head[9] = proto.toByte()
        head[10] = ((length shr 8) and 0xFF).toByte()
        head[11] = (length and 0xFF).toByte()
        return head
    }

    private fun ipHeader(
        p: ByteArray,
        total: Int,
        proto: Int,
        src: ByteArray,
        dst: ByteArray
    ) {
        p[0] = 0x45
        p[1] = 0
        putShort(p, 2, total)
        putShort(p, 4, 0x1234)
        putShort(p, 6, 0x4000)
        p[8] = 64
        p[9] = proto.toByte()
        putShort(p, 10, 0)
        System.arraycopy(src, 0, p, 12, 4)
        System.arraycopy(dst, 0, p, 16, 4)
        putShort(p, 10, fold(sumOf(p, 0, 20, 0L)))
    }

    fun tcp(
        srcIp: ByteArray,
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        seq: Int,
        ack: Int,
        flags: Int,
        payload: ByteArray
    ): ByteArray {
        val total = 40 + payload.size
        val p = ByteArray(total)
        ipHeader(p, total, 6, srcIp, dstIp)
        putShort(p, 20, srcPort)
        putShort(p, 22, dstPort)
        putInt(p, 24, seq)
        putInt(p, 28, ack)
        p[32] = 0x50
        p[33] = flags.toByte()
        putShort(p, 34, 65535)
        putShort(p, 36, 0)
        putShort(p, 38, 0)
        if (payload.isNotEmpty()) System.arraycopy(payload, 0, p, 40, payload.size)
        val head = pseudo(srcIp, dstIp, 6, total - 20)
        var sum = sumOf(head, 0, 12, 0L)
        sum = sumOf(p, 20, total - 20, sum)
        putShort(p, 36, fold(sum))
        return p
    }

    fun udp(
        srcIp: ByteArray,
        srcPort: Int,
        dstIp: ByteArray,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val total = 28 + payload.size
        val p = ByteArray(total)
        ipHeader(p, total, 17, srcIp, dstIp)
        putShort(p, 20, srcPort)
        putShort(p, 22, dstPort)
        putShort(p, 24, 8 + payload.size)
        putShort(p, 26, 0)
        if (payload.isNotEmpty()) System.arraycopy(payload, 0, p, 28, payload.size)
        return p
    }
}
