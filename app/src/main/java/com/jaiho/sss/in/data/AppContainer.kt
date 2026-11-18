package com.jaiho.sss.`in`.data

import android.content.Context

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsStore(appContext)
}
