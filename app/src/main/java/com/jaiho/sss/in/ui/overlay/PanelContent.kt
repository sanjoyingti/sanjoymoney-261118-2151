package com.jaiho.sss.`in`.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaiho.sss.`in`.data.BotRuntime
import com.jaiho.sss.`in`.model.TAP_POINTS
import com.jaiho.sss.`in`.model.TapPoint
import com.jaiho.sss.`in`.model.intervalFor
import com.jaiho.sss.`in`.model.label
import com.jaiho.sss.`in`.ui.theme.Accent
import com.jaiho.sss.`in`.ui.theme.Outline
import com.jaiho.sss.`in`.ui.theme.Surface1
import com.jaiho.sss.`in`.ui.theme.Surface2
import com.jaiho.sss.`in`.ui.theme.TextDim
import com.jaiho.sss.`in`.ui.theme.TextMain

private val PANEL_WIDTH = 236.dp
private val SHAPE = RoundedCornerShape(18.dp)

@Composable
fun PanelContent(
    onToggleWin: (Boolean) -> Unit,
    onToggleJackpot: (Boolean) -> Unit,
    onBet: (TapPoint) -> Unit,
    onSpeed: (Int) -> Unit,
    onTap: () -> Unit,
    onAim: () -> Unit,
    onCollapse: (Boolean) -> Unit,
    onHide: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val settings by BotRuntime.settings.collectAsState()
    val stats by BotRuntime.stats.collectAsState()
    val net by BotRuntime.net.collectAsState()
    val aimStep by BotRuntime.aimStep.collectAsState()

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragEnd = { onDragEnd() },
            onDragCancel = { onDragEnd() }
        ) { change, delta ->
            change.consume()
            onDrag(delta.x, delta.y)
        }
    }

    if (settings.collapsed) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = settings.opacity.coerceIn(0.4f, 1f)))
                .clickable { onCollapse(false) }
                .then(dragModifier),
            contentAlignment = Alignment.Center
        ) {
            Text("SM", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .width(PANEL_WIDTH)
            .clip(SHAPE)
            .background(Surface1.copy(alpha = settings.opacity.coerceIn(0.4f, 1f)))
            .border(1.dp, Outline, SHAPE)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SANJOY MONEY",
                color = Accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .then(dragModifier)
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Collapse",
                tint = TextDim,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onCollapse(true) }
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Hide panel",
                tint = TextDim,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onHide() }
            )
        }

        Spacer(Modifier.height(6.dp))

        ToggleRow(
            title = "ALWAYS WIN",
            checked = settings.alwaysWin,
            needAim = !settings.has(TapPoint.SPIN),
            onChecked = onToggleWin
        )
        ToggleRow(
            title = "JACKPOT",
            checked = settings.jackpot,
            needAim = !settings.has(TapPoint.JACKPOT),
            onChecked = onToggleJackpot
        )

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallButton(
                text = "BET +",
                enabled = settings.has(TapPoint.BET_UP),
                modifier = Modifier.weight(1f)
            ) { onBet(TapPoint.BET_UP) }
            SmallButton(
                text = "BET \u2212",
                enabled = settings.has(TapPoint.BET_DOWN),
                modifier = Modifier.weight(1f)
            ) { onBet(TapPoint.BET_DOWN) }
        }
        if (!settings.has(TapPoint.BET_UP) || !settings.has(TapPoint.BET_DOWN)) {
            Text("AIM needed for BET + / \u2212", color = TextDim, fontSize = 9.sp)
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SPEED", color = TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
            StepButton("\u2212") { onSpeed(-1) }
            Text(
                "${settings.speed}  ${intervalFor(settings.speed)}ms",
                color = TextMain,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            StepButton("+") { onSpeed(1) }
        }

        Spacer(Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallButton(
                text = "TAP",
                enabled = settings.has(TapPoint.SPIN),
                modifier = Modifier.weight(1f)
            ) { onTap() }
            SmallButton(
                text = "AIM",
                enabled = true,
                highlight = true,
                modifier = Modifier.weight(1f)
            ) { onAim() }
        }
        if (!settings.has(TapPoint.SPIN)) {
            Text("AIM needed for TAP", color = TextDim, fontSize = 9.sp)
        }

        Spacer(Modifier.height(8.dp))

        val minutes = ((System.currentTimeMillis() - stats.startedAt) / 60000L).coerceAtLeast(0L)
        Text(
            "Spins ${stats.spins} · ${minutes}m · Streak ${stats.streak}",
            color = TextDim,
            fontSize = 10.sp
        )
        if (settings.netHook) {
            val kb = net.bytes / 1024L
            Text("Net ${kb}KB · rewritten ${net.frames}", color = Accent, fontSize = 10.sp)
        }
        if (aimStep >= 0) {
            val kind = TAP_POINTS.getOrNull(aimStep)
            Text(
                "AIM ${aimStep + 1}/4 — ${kind?.label() ?: ""}",
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    needAim: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = if (needAim) TextDim else TextMain, fontSize = 11.sp)
            if (needAim) Text("AIM needed", color = TextDim, fontSize = 9.sp)
        }
        MiniSwitch(checked = checked, enabled = !needAim, onChecked = onChecked)
    }
}

@Composable
private fun MiniSwitch(checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 20.dp)
            .clip(CircleShape)
            .background(if (checked) Accent else Surface2)
            .clickable(enabled = enabled) { onChecked(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else TextDim)
        )
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Surface2)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallButton(
    text: String,
    enabled: Boolean,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (highlight) Accent.copy(alpha = 0.22f) else Surface2)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = when {
                !enabled -> TextDim
                highlight -> Accent
                else -> TextMain
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
