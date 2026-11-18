package com.jaiho.sss.`in`.util

import android.util.Log

object L {
    const val TAG = "SanjoyBot"

    fun d(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            // logging must never throw
        }
    }

    fun e(message: String, t: Throwable? = null) {
        try {
            Log.e(TAG, message, t)
        } catch (_: Throwable) {
            // logging must never throw
        }
    }
}
