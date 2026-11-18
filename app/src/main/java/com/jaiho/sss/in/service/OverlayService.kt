package com.jaiho.sss.in.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jaiho.sss.in.BotApp
import com.jaiho.sss.in.MainActivity
import com.jaiho.sss.in.R
import com.jaiho.sss.in.data.BotRuntime
import com.jaiho.sss.in.net.NetHookService
import com.jaiho.sss.in.util.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class OverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        try {
            OverlayHost.init(this)
            Screen.refresh(this)
            startForegroundSafe()
            watchSettings()
            registerScreenReceiver()
        } catch (t: Throwable) {
            L.e("service onCreate failed", t)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_STOP -> stopEverything()
                ACTION_HIDE -> {
                    BotRuntime.update { it.copy(panelHidden = true) }
                    OverlayHost.hidePanel(this)
                }
                ACTION_SHOW -> {
                    BotRuntime.update { it.copy(panelHidden = false) }
                    OverlayHost.showPanel(this)
                }
                ACTION_AIM -> {
                    BotRuntime.update { it.copy(panelHidden = false) }
                    OverlayHost.showPanel(this)
                    OverlayHost.startAim(this)
                }
                else -> {
                    BotRuntime.update { it.copy(panelHidden = false) }
                    OverlayHost.showPanel(this)
                }
            }
        } catch (t: Throwable) {
            L.e("onStartCommand failed", t)
        }
        return START_STICKY
    }

    private fun watchSettings() {
        watcher?.cancel()
        watcher = scope.launch {
            try {
                BotRuntime.settings.collect { settings ->
                    if (settings.panelHidden) OverlayHost.hidePanel(this@OverlayService)
                    else if (!OverlayHost.panelShown) OverlayHost.showPanel(this@OverlayService)
                    BotEngine.sync()
                    if (!settings.netHook) NetHookService.requestStop(this@OverlayService)
                }
            } catch (t: Throwable) {
                L.e("settings watcher failed", t)
            }
        }
    }

    private fun registerScreenReceiver() {
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        when (intent?.action) {
                            Intent.ACTION_SCREEN_OFF -> BotEngine.pause()
                            Intent.ACTION_SCREEN_ON -> BotEngine.resume()
                            Intent.ACTION_USER_PRESENT -> BotEngine.resume()
                        }
                    } catch (t: Throwable) {
                        L.e("screen receiver failed", t)
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
            screenReceiver = receiver
        } catch (t: Throwable) {
            L.e("screen receiver register failed", t)
        }
    }

    private fun startForegroundSafe() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (t: Throwable) {
            L.e("startForeground failed", t)
            try {
                startForeground(NOTIF_ID, buildNotification())
            } catch (inner: Throwable) {
                L.e("startForeground retry failed", inner)
            }
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, OverlayService::class.java).setAction(ACTION_SHOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, BotApp.CH_ONGOING)
            .setSmallIcon(R.drawable.ic_launcher_bot)
            .setContentTitle("Sanjoy Money is running")
            .setContentText("Floating bot ready for Jaiho 777")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .addAction(0, "Panel", showIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun stopEverything() {
        try {
            BotRuntime.update { it.copy(alwaysWin = false, jackpot = false) }
            BotEngine.stopAll()
            NetHookService.requestStop(this)
            OverlayHost.hideEverything(this)
        } catch (t: Throwable) {
            L.e("stop failed", t)
        }
        stopSelf()
    }

    override fun onDestroy() {
        try {
            watcher?.cancel()
            watcher = null
        } catch (t: Throwable) {
            L.e("watcher cleanup failed", t)
        }
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (t: Throwable) {
            L.e("receiver cleanup failed", t)
        }
        screenReceiver = null
        BotEngine.stopAll()
        OverlayHost.hideEverything(this)
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (t: Throwable) {
            L.e("stopForeground failed", t)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIF_ID = 7711
        const val ACTION_STOP = "com.jaiho.sss.in.STOP"
        const val ACTION_HIDE = "com.jaiho.sss.in.HIDE"
        const val ACTION_SHOW = "com.jaiho.sss.in.SHOW"
        const val ACTION_AIM = "com.jaiho.sss.in.AIM"

        fun start(context: Context) {
            try {
                val intent = Intent(context, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                L.e("service start failed", t)
            }
        }

        fun aim(context: Context) {
            try {
                context.startService(
                    Intent(context, OverlayService::class.java).setAction(ACTION_AIM)
                )
            } catch (t: Throwable) {
                L.e("service aim failed", t)
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(
                    Intent(context, OverlayService::class.java).setAction(ACTION_STOP)
                )
            } catch (t: Throwable) {
                L.e("service stop failed", t)
            }
        }
    }
}
