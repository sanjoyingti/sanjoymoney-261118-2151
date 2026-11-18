=== ABOUT: Saves every switch and tap point to the phone's storage so the bot remembers them next time ===
package com.jaiho.sss.in.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jaiho.sss.in.model.BotSettings
import com.jaiho.sss.in.model.MISSING
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sanjoyStore by preferencesDataStore(name = "sanjoy_settings")

class SettingsStore(private val context: Context) {

    private object K {
        val spinX = floatPreferencesKey("m_spin_x")
        val spinY = floatPreferencesKey("m_spin_y")
        val jackpotX = floatPreferencesKey("m_jp_x")
        val jackpotY = floatPreferencesKey("m_jp_y")
        val betUpX = floatPreferencesKey("m_bet_up_x")
        val betUpY = floatPreferencesKey("m_bet_up_y")
        val betDownX = floatPreferencesKey("m_bet_down_x")
        val betDownY = floatPreferencesKey("m_bet_down_y")
        val speed = intPreferencesKey("speed")
        val opacity = floatPreferencesKey("opacity")
        val panelX = intPreferencesKey("panel_x")
        val panelY = intPreferencesKey("panel_y")
        val collapsed = booleanPreferencesKey("collapsed")
        val panelHidden = booleanPreferencesKey("panel_hidden")
        val alwaysWin = booleanPreferencesKey("always_win")
        val jackpot = booleanPreferencesKey("jackpot")
        val netHook = booleanPreferencesKey("net_hook")
        val multiplier = floatPreferencesKey("multiplier")
        val target = stringPreferencesKey("target_package")
    }

    val settings: Flow<BotSettings> = context.sanjoyStore.data.map { p ->
        BotSettings(
            spinX = p[K.spinX] ?: MISSING,
            spinY = p[K.spinY] ?: MISSING,
            jackpotX = p[K.jackpotX] ?: MISSING,
            jackpotY = p[K.jackpotY] ?: MISSING,
            betUpX = p[K.betUpX] ?: MISSING,
            betUpY = p[K.betUpY] ?: MISSING,
            betDownX = p[K.betDownX] ?: MISSING,
            betDownY = p[K.betDownY] ?: MISSING,
            speed = (p[K.speed] ?: 3).coerceIn(1, 6),
            opacity = (p[K.opacity] ?: 0.9f).coerceIn(0.3f, 1f),
            panelX = p[K.panelX] ?: -1,
            panelY = p[K.panelY] ?: -1,
            collapsed = p[K.collapsed] ?: false,
            panelHidden = p[K.panelHidden] ?: false,
            alwaysWin = p[K.alwaysWin] ?: false,
            jackpot = p[K.jackpot] ?: false,
            netHook = p[K.netHook] ?: false,
            multiplier = (p[K.multiplier] ?: 5f).coerceIn(1f, 20f),
            targetPackage = p[K.target] ?: "com.jaiho.sss.in"
        )
    }

    suspend fun save(s: BotSettings) {
        context.sanjoyStore.edit { p ->
            p[K.spinX] = s.spinX
            p[K.spinY] = s.spinY
            p[K.jackpotX] = s.jackpotX
            p[K.jackpotY] = s.jackpotY
            p[K.betUpX] = s.betUpX
            p[K.betUpY] = s.betUpY
            p[K.betDownX] = s.betDownX
            p[K.betDownY] = s.betDownY
            p[K.speed] = s.speed
            p[K.opacity] = s.opacity
            p[K.panelX] = s.panelX
            p[K.panelY] = s.panelY
            p[K.collapsed] = s.collapsed
            p[K.panelHidden] = s.panelHidden
            p[K.alwaysWin] = s.alwaysWin
            p[K.jackpot] = s.jackpot
            p[K.netHook] = s.netHook
            p[K.multiplier] = s.multiplier
            p[K.target] = s.targetPackage
        }
    }
}
