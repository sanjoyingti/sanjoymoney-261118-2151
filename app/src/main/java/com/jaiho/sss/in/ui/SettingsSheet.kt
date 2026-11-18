=== ABOUT: The settings drawer with the speed, transparency and reward multiplier sliders plus a reset button ===
package com.jaiho.sss.in.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaiho.sss.in.model.intervalFor
import com.jaiho.sss.in.ui.theme.Accent
import com.jaiho.sss.in.ui.theme.Bg
import com.jaiho.sss.in.ui.theme.Danger
import com.jaiho.sss.in.ui.theme.Surface1
import com.jaiho.sss.in.ui.theme.TextDim
import com.jaiho.sss.in.ui.theme.TextMain
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    speed: Int,
    opacity: Float,
    multiplier: Float,
    onSpeed: (Int) -> Unit,
    onOpacity: (Float) -> Unit,
    onMultiplier: (Float) -> Unit,
    onReset: () -> Unit,
    onAim: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1,
        contentColor = TextMain
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Settings", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Speed ${speed} · ${intervalFor(speed)} ms", color = TextDim, fontSize = 12.sp)
            Slider(
                value = speed.toFloat(),
                onValueChange = { onSpeed(it.roundToInt().coerceIn(1, 6)) },
                valueRange = 1f..6f,
                steps = 4,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
            )

            Spacer(Modifier.height(10.dp))
            Text("Panel transparency ${(opacity * 100).roundToInt()}%", color = TextDim, fontSize = 12.sp)
            Slider(
                value = opacity,
                onValueChange = onOpacity,
                valueRange = 0.3f..1f,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
            )

            Spacer(Modifier.height(10.dp))
            Text("Win multiplier ×${multiplier.roundToInt()}", color = TextDim, fontSize = 12.sp)
            Slider(
                value = multiplier,
                onValueChange = onMultiplier,
                valueRange = 1f..20f,
                steps = 18,
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
            )
            Text(
                "Only used when the net hook rewrites a spin reply.",
                color = TextDim,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent)
                        .clickable { onAim() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("AIM POINTS", color = Bg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Danger.copy(alpha = 0.16f))
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("RESET MARKERS", color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}
