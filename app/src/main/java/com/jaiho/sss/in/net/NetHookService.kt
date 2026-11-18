package com.jaiho.sss.`in`.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.jaiho.sss.`in`.MainActivity
import com.jaiho.sss.`in`.R
import com.jaiho.sss.`in`.data.BotRuntime
import com.jaiho.sss.`in`.data.TargetResolver
import com.jaiho.sss.`in`.util.L

class NetHookService : VpnService() {

    private var engine: NetEngine? = null
    private var tun: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_STOP -> {
                    shutdown()
                    stopSelf()
                    return START_NOT_STICKY
                }
                else -> {
                    foregroundSafe()
                    startHook()
                    return START_STICKY
                }
            }
        } catch (t: Throwable) {
            L.e("net hook command failed", t)
        }
        return START_STICKY
    }

    private fun startHook() {
        if (engine != null) return
        try {
            val target = TargetResolver.effectiveTarget(
                this,
                BotRuntime.settings.value.targetPackage
            )
            val builder = Builder()
                .setSession("Sanjoy Money net hook")
                .setMtu(1500)
                .setBlocking(true)
                .addAddress("10.26.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)
            try {
                if (target.isNotEmpty() && target != packageName) {
                    builder.addAllowedApplication(target)
                }
            } catch (t: Throwable) {
                L.e("allowed app failed, routing everything", t)
            }

            val descriptor = builder.establish()
            if (descriptor == null) {
                L.e("tunnel not established", null)
                return
            }
            tun = descriptor
            val fresh = NetEngine(this, descriptor) { BotRuntime.settings.value.multiplier }
            engine = fresh
            fresh.start()
            running = true
            L.d("net hook running for ${target.ifEmpty { "all apps" }}")
        } catch (t: Throwable) {
            L.e("net hook start failed", t)
            shutdown()
        }
    }

    private fun shutdown() {
        running = false
        try {
            engine?.stop()
        } catch (t: Throwable) {
            L.e("engine stop failed", t)
        }
        engine = null
        try {
            tun?.close()
        } catch (_: Throwable) {
        }
        tun = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
    }

    override fun onRevoke() {
        try {
            BotRuntime.update { it.copy(netHook = false) }
        } catch (_: Throwable) {
        }
        shutdown()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun createChannel() {
        try {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL,
                "Sanjoy net hook",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            channel.description = "Watches the game's spin replies and rewrites wins."
            manager.createNotificationChannel(channel)
        } catch (t: Throwable) {
            L.e("net channel failed", t)
        }
    }

    private fun foregroundSafe() {
        try {
            val intent = PendingIntent.getActivity(
                this,
                11,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopIntent = PendingIntent.getService(
                this,
                12,
                Intent(this, NetHookService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_bot)
                .setContentTitle("Net hook active")
                .setContentText("Rewriting spin replies for Jaiho 777")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(intent)
                .addAction(0, "Stop", stopIntent)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (t: Throwable) {
            L.e("net foreground failed", t)
        }
    }

    companion object {
        const val ACTION_STOP = "com.jaiho.sss.in.NETSTOP"
        private const val CHANNEL = "sanjoy_net"
        private const val NOTIF_ID = 7712

        @Volatile
        var running: Boolean = false
            private set

        fun start(context: Context) {
            try {
                val intent = Intent(context, NetHookService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                L.e("net hook start request failed", t)
            }
        }

        fun requestStop(context: Context) {
            if (!running) return
            try {
                context.startService(
                    Intent(context, NetHookService::class.java).setAction(ACTION_STOP)
                )
            } catch (t: Throwable) {
                L.e("net hook stop request failed", t)
            }
        }
    }
}
