=== ABOUT: The app's only screen host: it opens the home screen inside the dark theme ===
package com.jaiho.sss.in

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jaiho.sss.in.data.BotRuntime
import com.jaiho.sss.in.ui.HomeRoute
import com.jaiho.sss.in.ui.theme.SanjoyTheme
import com.jaiho.sss.in.util.L

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val app = application as? BotApp
            if (app != null) BotRuntime.attach(app.container.settings)
        } catch (t: Throwable) {
            L.e("runtime attach failed", t)
        }
        setContent {
            SanjoyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeRoute()
                }
            }
        }
    }
}
