package com.jaiho.sss.`in`.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.jaiho.sss.`in`.util.L

class TapAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        L.d("tap service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        L.d("tap service destroyed")
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: TapAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        /** Fires one finger press. Returns false when the service is off or the gesture failed. */
        fun tap(x: Float, y: Float, durationMs: Long = 60L): Boolean {
            val service = instance ?: return false
            return try {
                val path = Path().apply { moveTo(x, y) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                    .build()
                service.dispatchGesture(gesture, null, null)
                true
            } catch (t: Throwable) {
                L.e("tap at $x,$y failed", t)
                false
            }
        }
    }
}
