package com.lord.dns.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lord.dns.ui.theme.AppTheme
import com.lord.dns.ui.theme.scheme

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val custom = servers.filter { it.isCustom }
    val cs = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current

    var name by rememberSaveable { mutableStateOf("") }
    var primary by rememberSaveable { mutableStateOf("") }
    var secondary by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )

        SectionLabel("THEME")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTheme.entries.forEach { t -> ThemeSwatch(t, t == theme) { vm.setTheme(t) } }
        }

        SectionLabel("CONNECTION")
        Surface(
            shape = RoundedCornerShape(20.dp), color = cs.surface,
            border = BorderStroke(1.dp, cs.outlineVariant), modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                SettingSwitch("Auto Connect", "Connect when the app opens or the phone boots", settings.autoConnect) { v ->
                    vm.updateSettings { it.copy(autoConnect = v) }
                }
                Divider()
                SettingSwitch("Pick Fastest DNS", "Ping every server and use the quickest one on connect", settings.pickFastest) { v ->
                    vm.updateSettings { it.copy(pickFastest = v) }
                }
                Divider()
                SettingSwitch("IPv6", "Also catch DNS queries sent over IPv6", settings.ipv6) { v ->
                    vm.updateSettings { it.copy(ipv6 = v) }
                }
                Divider()
                SettingSwitch("DNS over HTTPS", "Encrypt queries on servers marked DoH", settings.doh) { v ->
                    vm.updateSettings { it.copy(doh = v) }
                }
            }
        }

        SectionLabel("ADD CUSTOM DNS")
        LabeledField("Name", name, "e.g. Office DNS", mono = false) { name = it; error = null }
        LabeledField("Primary DNS", primary, "e.g. 1.1.1.1", mono = true) { primary = it; error = null }
        LabeledField("Secondary DNS (optional)", secondary, "e.g. 1.0.0.1", mono = true) { secondary = it; error = null }
        error?.let {
            Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }
        Button(
            onClick = {
                val e = vm.addCustom(name, primary, secondary)
                error = e
                if (e == null) {
                    name = ""; primary = ""; secondary = ""
                    focus.clearFocus()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }

        if (custom.isNotEmpty()) {
            SectionLabel("YOUR SERVERS")
            custom.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ServerIcon(s, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(s.primary, s.secondary).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                            color = cs.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.deleteCustom(s.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete ${s.name}", tint = cs.onSurfaceVariant)
                    }
                }
            }
        }

        Text(
            "Lord DNS 1.0 · no accounts, no logs",
            style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 28.dp, bottom = 10.dp, start = 4.dp),
    )
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = cs.primary, checkedThumbColor = cs.onPrimary),
        )
    }
}

@Composable
private fun LabeledField(label: String, value: String, hint: String, mono: Boolean, onChange: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(hint) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default),
            keyboardOptions = KeyboardOptions(keyboardType = if (mono) KeyboardType.Uri else KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cs.surface,
                focusedContainerColor = cs.surface,
                unfocusedBorderColor = cs.outlineVariant,
            ),
        )
    }
}

@Composable
private fun ThemeSwatch(t: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val preview = t.scheme(isSystemInDarkTheme())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(preview.background)
                .border(if (selected) 3.dp else 1.dp, if (selected) cs.primary else cs.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(preview.primary))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            t.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) cs.primary else cs.onSurfaceVariant,
        )
    }
}
