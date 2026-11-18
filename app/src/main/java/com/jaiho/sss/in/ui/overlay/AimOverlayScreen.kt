package com.jaiho.sss.`in`.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaiho.sss.`in`.model.TAP_POINTS
import com.jaiho.sss.`in`.model.label
import com.jaiho.sss.`in`.ui.theme.Accent
import com.jaiho.sss.`in`.ui.theme.Bg
import com.jaiho.sss.`in`.ui.theme.Surface1
import com.jaiho.sss.`in`.ui.theme.TextMain
import kotlin.math.roundToInt

@Composable
fun AimOverlayScreen(
    step: Int,
    marker: Offset,
    onMarker: (Offset) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val currentMarker by rememberUpdatedState(marker)
    val currentSize by rememberUpdatedState(size)
    val kind = TAP_POINTS.getOrNull(step)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg.copy(alpha = 0.18f))
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = currentSize
                    if (s.width > 0 && s.height > 0) {
                        onMarker(
                            Offset(
                                (offset.x / s.width).coerceIn(0f, 1f),
                                (offset.y / s.height).coerceIn(0f, 1f)
                            )
                        )
                    }
                }
            }
    ) {
        val s = size
        val px = currentMarker.x * s.width
        val py = currentMarker.y * s.height

        Box(
            modifier = Modifier
                .offset { IntOffset((px - 34.dp.toPx()).roundToInt(), (py - 34.dp.toPx()).roundToInt()) }
                .size(68.dp)
                .border(2.dp, Color.White, CircleShape)
                .padding(6.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.55f))
                .pointerInput(Unit) {
                    detectDragGestures { change, delta ->
                        change.consume()
                        val sz = currentSize
                        if (sz.width > 0 && sz.height > 0) {
                            onMarker(
                                Offset(
                                    (currentMarker.x + delta.x / sz.width).coerceIn(0f, 1f),
                                    (currentMarker.y + delta.y / sz.height).coerceIn(0f, 1f)
                                )
                            )
                        }
                    }
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface1.copy(alpha = 0.94f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "AIM ${step + 1} of 4",
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        "Drag the green circle onto the ${kind?.label() ?: ""} button",
                        color = TextMain,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1.copy(alpha = 0.94f))
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("CANCEL", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Accent)
                        .clickable { onConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE POINT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
