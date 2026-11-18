=== ABOUT: Holds the app's single settings store, passed around by hand instead of with a framework ===
package com.jaiho.sss.in.data

import android.content.Context

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val settings = SettingsStore(appContext)
}
