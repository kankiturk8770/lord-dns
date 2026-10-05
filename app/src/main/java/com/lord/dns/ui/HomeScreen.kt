package com.lord.dns.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lord.dns.data.DnsServer
import com.lord.dns.vpn.VpnState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: MainViewModel, onConnect: () -> Unit, onOpenServers: () -> Unit) {
    val status by VpnState.status.collectAsStateWithLifecycle()
    val connectedAt by VpnState.connectedAt.collectAsStateWithLifecycle()
    val queries by VpnState.queries.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val pings by vm.pings.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val server = servers.firstOrNull { it.id == selectedId } ?: servers.first()
    val connected = status == VpnState.Status.CONNECTED
    val cs = MaterialTheme.colorScheme

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(connected) {
        while (connected) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(selectedId, connected) { if (connected || pings[selectedId] == null) vm.pingOne(selectedId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = cs.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lord DNS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.primary)
        }

        Spacer(Modifier.height(36.dp))
        Text(
            text = when (status) {
                VpnState.Status.CONNECTED -> "Connected"
                VpnState.Status.CONNECTING -> "Connecting…"
                VpnState.Status.DISCONNECTED -> "Not connected"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (connected) cs.primary else cs.onBackground,
        )
        Text(
            text = if (connected) "Every DNS query goes through ${server.name}" else "Tap the button to switch your DNS",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
        PowerButton(status) {
            when (status) {
                VpnState.Status.DISCONNECTED -> onConnect()
                VpnState.Status.CONNECTED -> vm.disconnect()
                VpnState.Status.CONNECTING -> Unit
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (connected) formatDuration(now - connectedAt) else "00:00:00",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            color = if (connected) cs.primary else cs.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))
        val dohLabel = when {
            !settings.doh -> "Off"
            server.doh == null -> "N/A"
            else -> "On"
        }
        CurrentServerCard(server, pings[server.id], dohLabel, if (connected) queries.toString() else "0", onOpenServers)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PowerButton(status: VpnState.Status, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val connected = status == VpnState.Status.CONNECTED
    val connecting = status == VpnState.Status.CONNECTING

    val inf = rememberInfiniteTransition(label = "ring")
    val rotation by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rot")
    val pulse by inf.animateFloat(
        1f, 0.92f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    val coreColor by animateColorAsState(if (connected) cs.primary else cs.surface, tween(400), label = "core")
    val iconTint by animateColorAsState(if (connected) cs.onPrimary else cs.onSurfaceVariant, tween(400), label = "tint")
    val ringColor = if (connected || connecting) cs.primary else cs.outline

    Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = if (connected || connecting) rotation else 0f }
        ) {
            drawCircle(
                color = ringColor,
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f))),
            )
        }
        Box(
            Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = if (connected) 0.16f else 0.07f))
        )
        Box(
            modifier = Modifier
                .size(156.dp)
                .graphicsLayer {
                    val s = if (connecting) pulse else 1f
                    scaleX = s; scaleY = s
                }
                .shadow(if (connected) 18.dp else 6.dp, CircleShape, spotColor = cs.primary)
                .clip(CircleShape)
                .background(coreColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = if (connected) "Disconnect" else "Connect",
                tint = iconTint,
                modifier = Modifier.size(68.dp),
            )
        }
    }
}

@Composable
private fun CurrentServerCard(server: DnsServer, ping: Int?, doh: String, queries: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServerIcon(server, 48.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Primary DNS · ${server.primary}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = cs.onSurfaceVariant,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Change DNS", tint = cs.onSurfaceVariant)
            }
            HorizontalDivider(color = cs.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Stat(Icons.Rounded.Speed, "Ping", Modifier.weight(1f)) { PingText(ping) }
                VerticalDivider(Modifier.height(44.dp), color = cs.outlineVariant)
                Stat(Icons.Rounded.Lock, "DoH", Modifier.weight(1f)) { StatValue(doh) }
                VerticalDivider(Modifier.height(44.dp), color = cs.outlineVariant)
                Stat(Icons.Rounded.BarChart, "Queries", Modifier.weight(1f)) { StatValue(queries) }
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, label: String, modifier: Modifier, value: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        value()
    }
}

@Composable
private fun StatValue(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
    )
}
