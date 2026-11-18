package com.jaiho.sss.in.service

import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.jaiho.sss.in.util.L

object Screen {

    @Volatile
    private var w = 1080

    @Volatile
    private var h = 1920

    fun refresh(context: Context) {
        try {
            val metrics = context.resources.displayMetrics
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = context.getSystemService(WindowManager::class.java)
                val bounds = wm?.currentWindowMetrics?.bounds
                if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                    w = bounds.width()
                    h = bounds.height()
                    return
                }
            }
            if (metrics.widthPixels > 0) w = metrics.widthPixels
            if (metrics.heightPixels > 0) h = metrics.heightPixels
        } catch (t: Throwable) {
            L.e("screen refresh failed", t)
        }
    }

    val width: Int get() = w
    val height: Int get() = h

    fun px(nx: Float, ny: Float): Pair<Float, Float> =
        (nx.coerceIn(0f, 1f) * w) to (ny.coerceIn(0f, 1f) * h)
}
