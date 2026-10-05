@file:OptIn(ExperimentalMaterial3Api::class)

package com.lord.dns.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lord.dns.data.Category
import com.lord.dns.data.DnsServer

private enum class Filter(val label: String) {
    ALL("All"), FASTEST("Fastest"), ADBLOCK("Ad-block"), IRAN("Iran"), GLOBAL("Global"), CUSTOM("Custom")
}

@Composable
fun ServersScreen(vm: MainViewModel) {
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val pings by vm.pings.collectAsStateWithLifecycle()
    val pinging by vm.pinging.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }

    val shown = remember(servers, query, filter, pings) {
        servers
            .filter { s ->
                query.isBlank() || s.name.contains(query, ignoreCase = true) ||
                    s.primary.contains(query) || (s.secondary?.contains(query) == true)
            }
            .filter { s ->
                when (filter) {
                    Filter.ALL, Filter.FASTEST -> true
                    Filter.ADBLOCK -> s.adBlock
                    Filter.IRAN -> s.category == Category.IRAN
                    Filter.GLOBAL -> s.category == Category.GLOBAL
                    Filter.CUSTOM -> s.isCustom
                }
            }
            .let { list ->
                if (filter == Filter.FASTEST) list.sortedBy { s -> pings[s.id]?.takeIf { it > 0 } ?: Int.MAX_VALUE }
                else list
            }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Choose DNS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Search DNS providers") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cs.surface,
                focusedContainerColor = cs.surface,
                unfocusedBorderColor = cs.outlineVariant,
            ),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(Filter.entries) { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cs.primary,
                        selectedLabelColor = cs.onPrimary,
                    ),
                )
            }
        }

        if (shown.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (filter == Filter.CUSTOM && query.isBlank()) "No custom DNS yet. Add one from Settings."
                    else "Nothing matches “$query”.",
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.id }) { s ->
                    ServerRow(s, s.id == selectedId, pings[s.id]) { vm.select(s.id) }
                }
            }
        }

        Button(
            onClick = { vm.pingAll() },
            enabled = !pinging,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            if (pinging) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = cs.onPrimary)
                Spacer(Modifier.width(10.dp))
                Text("Testing…")
            } else {
                Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ping Test", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ServerRow(s: DnsServer, selected: Boolean, ping: Int?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) cs.primary.copy(alpha = 0.09f).compositeOver(cs.surface) else cs.surface,
        border = BorderStroke(1.dp, if (selected) cs.primary.copy(alpha = 0.5f) else cs.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ServerIcon(s, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                    )
                    if (s.adBlock) SmallTag("Ad-block")
                    if (s.doh != null) SmallTag("DoH")
                }
                Text(
                    listOfNotNull(s.primary, s.secondary).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                    color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            PingText(ping)
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = cs.primary))
        }
    }
}
