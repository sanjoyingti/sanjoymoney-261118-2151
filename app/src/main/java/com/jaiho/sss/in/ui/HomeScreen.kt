=== ABOUT: The home screen: the big open button, the three permission rows, the quick chips and the stop button ===
package com.jaiho.sss.in.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaiho.sss.in.model.intervalFor
import com.jaiho.sss.in.net.NetHookService
import com.jaiho.sss.in.ui.theme.Accent
import com.jaiho.sss.in.ui.theme.Danger
import com.jaiho.sss.in.ui.theme.Outline
import com.jaiho.sss.in.ui.theme.Surface1
import com.jaiho.sss.in.ui.theme.Surface2
import com.jaiho.sss.in.ui.theme.TextDim
import com.jaiho.sss.in.ui.theme.TextMain
import com.jaiho.sss.in.util.L

@Composable
fun HomeRoute(vm: HomeViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val net by vm.net.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.refresh() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.setNetHook(true)
            NetHookService.start(ctx)
        } else {
            vm.setNetHook(false)
            vm.showMessage("VPN permission denied")
        }
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
        vm.clearMessage()
    }

    fun openOverlaySettings() {
        try {
            overlayLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.packageName)
                )
            )
        } catch (t: Throwable) {
            L.e("overlay settings failed", t)
            vm.showMessage("Open Settings › Apps › Special access")
        }
    }

    fun openTapSettings() {
        try {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (t: Throwable) {
            L.e("accessibility settings failed", t)
            vm.showMessage("Open Settings › Accessibility")
        }
    }

    fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                )
            } catch (t: Throwable) {
                L.e("notification settings failed", t)
            }
        }
    }

    fun toggleNetHook(on: Boolean) {
        if (!on) {
            vm.setNetHook(false)
            NetHookService.requestStop(ctx)
            return
        }
        val consent = try {
            VpnService.prepare(ctx)
        } catch (t: Throwable) {
            null
        }
        if (consent != null) {
            vpnLauncher.launch(consent)
        } else {
            vm.setNetHook(true)
            NetHookService.start(ctx)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sanjoy Money",
                    color = TextMain,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Floating bot for Jaiho 777",
                    color = TextDim,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Surface1)
                    .clickable { showSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextDim)
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Accent)
                .clickable { vm.openJaiho() }
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "OPEN JAIHO 777",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Starts the floating panel and the game",
                        color = Color.Black.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (!state.installed) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Danger.copy(alpha = 0.14f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Jaiho 777 not installed",
                    color = Danger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Store",
                    color = Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { vm.openStore() }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        StatusRow("Display over other apps", state.overlay) { openOverlaySettings() }
        Spacer(Modifier.height(8.dp))
        StatusRow("Tap service", state.tapService) { openTapSettings() }
        Spacer(Modifier.height(8.dp))
        StatusRow("Notifications", state.notifications) { openNotificationSettings() }

        Spacer(Modifier.height(20.dp))
        Text("QUICK SETTINGS", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (speed in 1..6) {
                Chip(
                    text = speed.toString(),
                    active = settings.speed == speed,
                    modifier = Modifier.weight(1f)
                ) { vm.setSpeed(speed) }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(
                text = "OPACITY ${(settings.opacity * 100).toInt()}%",
                active = settings.opacity > 0.7f,
                modifier = Modifier.weight(1f)
            ) { showSheet = true }
            Chip(
                text = if (settings.netHook) "NET HOOK ON" else "NET HOOK OFF",
                active = settings.netHook,
                modifier = Modifier.weight(1f)
            ) { toggleNetHook(!settings.netHook) }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Speed ${settings.speed} · ${intervalFor(settings.speed)} ms base · spins ${stats.spins} · streak ${stats.streak}",
            color = TextDim,
            fontSize = 11.sp
        )
        if (settings.netHook) {
            Text(
                "Net ${net.bytes / 1024} KB seen · ${net.frames} frames rewritten",
                color = Accent,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface2)
                    .clickable { vm.startAim() },
                contentAlignment = Alignment.Center
            ) {
                Text("AIM POINTS", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Outline, RoundedCornerShape(14.dp))
                    .clickable { vm.stopBot() },
                contentAlignment = Alignment.Center
            ) {
                Text("STOP BOT", color = Danger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "About: Sanjoy Money 1.0 — floats over Jaiho 777, presses the buttons and keeps the taps human. " +
                "No root, no patching of the game.",
            color = TextDim,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showSheet) {
        SettingsSheet(
            speed = settings.speed,
            opacity = settings.opacity,
            multiplier = settings.multiplier,
            onSpeed = { vm.setSpeed(it) },
            onOpacity = { vm.setOpacity(it) },
            onMultiplier = { vm.setMultiplier(it) },
            onReset = { vm.resetMarkers() },
            onAim = { showSheet = false; vm.startAim() },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun StatusRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface1)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (granted) Accent else Danger)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextMain, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            if (granted) "ON" else "SET UP",
            color = if (granted) Accent else TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Chip(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Accent.copy(alpha = 0.18f) else Surface1)
            .border(1.dp, if (active) Accent else Outline, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) Accent else TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private val unusedTheme = MaterialTheme
