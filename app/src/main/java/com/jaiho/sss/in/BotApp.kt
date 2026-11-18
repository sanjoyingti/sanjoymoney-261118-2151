package com.jaiho.sss.`in`

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.jaiho.sss.`in`.data.AppContainer
import com.jaiho.sss.`in`.data.BotRuntime
import com.jaiho.sss.`in`.util.L

class BotApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            container = AppContainer(this)
            BotRuntime.attach(container.settings)
            createChannels()
        } catch (t: Throwable) {
            L.e("app init failed", t)
        }
    }

    private fun createChannels() {
        try {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CH_ONGOING,
                "Sanjoy Money bot",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            channel.description = "Keeps the floating bot alive while Jaiho 777 runs."
            nm.createNotificationChannel(channel)
        } catch (t: Throwable) {
            L.e("notification channel failed", t)
        }
    }

    companion object {
        const val CH_ONGOING = "sanjoy_ongoing"
    }
}
