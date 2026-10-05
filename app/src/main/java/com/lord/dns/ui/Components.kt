package com.lord.dns.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lord.dns.data.DnsServer
import com.lord.dns.data.Glyph
import com.lord.dns.ui.theme.PingBad
import com.lord.dns.ui.theme.PingGood
import com.lord.dns.ui.theme.PingMid

private val IconInk = Color(0xFFFDFEFE)

@Composable
fun ServerIcon(server: DnsServer, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color(server.color)),
        contentAlignment = Alignment.Center,
    ) {
        val icon: ImageVector? = when (server.glyph) {
            Glyph.CLOUD -> Icons.Rounded.Cloud
            Glyph.BOLT -> Icons.Rounded.Bolt
            Glyph.SHIELD -> Icons.Rounded.Shield
            Glyph.GLOBE -> Icons.Rounded.Public
            Glyph.ROCKET -> Icons.Rounded.RocketLaunch
            Glyph.CUSTOM -> Icons.Rounded.Dns
            else -> null
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = IconInk, modifier = Modifier.size(size * 0.55f))
        } else {
            val letter = when (server.glyph) {
                Glyph.GOOGLE -> "G"
                Glyph.SHECAN -> "S"
                Glyph.QUAD9 -> "9"
                Glyph.OPEN -> "O"
                else -> server.name.take(1)
            }
            Text(letter, color = IconInk, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
    }
}

@Composable
fun PingText(ping: Int?, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (label, color) = when {
        ping == null -> "--" to muted
        ping < 0 -> "timeout" to PingBad
        ping < 40 -> "${ping}ms" to PingGood
        ping < 100 -> "${ping}ms" to PingMid
        else -> "${ping}ms" to PingBad
    }
    Text(
        label, color = color, modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SmallTag(text: String) {
    val cs = MaterialTheme.colorScheme
    Text(
        text,
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(cs.primary.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color = cs.primary,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

fun formatDuration(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}
