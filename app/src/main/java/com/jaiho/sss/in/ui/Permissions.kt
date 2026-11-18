package com.jaiho.sss.`in`.ui

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.jaiho.sss.`in`.service.TapAccessibilityService

object Permissions {

    fun canDrawOverlays(context: Context): Boolean = try {
        Settings.canDrawOverlays(context)
    } catch (t: Throwable) {
        false
    }

    /** True when our tap service is switched on in the system accessibility list. */
    fun tapServiceEnabled(context: Context): Boolean {
        return try {
            val component = ComponentName(context, TapAccessibilityService::class.java)
            val full = component.flattenToString()
            val short = component.flattenToShortString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabled.split(':').any { it.equals(full, true) || it.equals(short, true) }
        } catch (t: Throwable) {
            false
        }
    }

    fun notificationsEnabled(context: Context): Boolean = try {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (t: Throwable) {
        true
    }
}
