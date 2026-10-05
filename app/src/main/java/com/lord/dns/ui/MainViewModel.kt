package com.lord.dns.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lord.dns.data.Category
import com.lord.dns.data.DnsServer
import com.lord.dns.data.Glyph
import com.lord.dns.data.Prefs
import com.lord.dns.net.DnsClient
import com.lord.dns.ui.theme.AppTheme
import com.lord.dns.vpn.LordVpnService
import com.lord.dns.vpn.VpnState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConnSettings(
    val autoConnect: Boolean,
    val pickFastest: Boolean,
    val ipv6: Boolean,
    val doh: Boolean,
)

private val IPV4 = Regex("""^((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)$""")
private val IPV6 = Regex("""^[0-9a-fA-F:]{2,39}$""")
private fun validIp(s: String) = IPV4.matches(s) || (s.contains(':') && IPV6.matches(s))

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    val prefs = Prefs(app)

    private val _servers = MutableStateFlow(prefs.allServers())
    val servers: StateFlow<List<DnsServer>> = _servers

    private val _selectedId = MutableStateFlow(prefs.selectedId)
    val selectedId: StateFlow<String> = _selectedId

    /** server id -> ms; -1 means timeout; missing means not tested yet */
    private val _pings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val pings: StateFlow<Map<String, Int>> = _pings

    private val _pinging = MutableStateFlow(false)
    val pinging: StateFlow<Boolean> = _pinging

    private val _settings = MutableStateFlow(ConnSettings(prefs.autoConnect, prefs.pickFastest, prefs.ipv6, prefs.doh))
    val settings: StateFlow<ConnSettings> = _settings

    private val _theme = MutableStateFlow(AppTheme.from(prefs.theme))
    val theme: StateFlow<AppTheme> = _theme

    private fun isUp() = VpnState.status.value == VpnState.Status.CONNECTED

    private fun setSelected(id: String) {
        prefs.selectedId = id
        _selectedId.value = id
    }

    fun select(id: String) {
        if (id == _selectedId.value) return
        setSelected(id)
        if (isUp()) LordVpnService.start(app)   // hot-swap the upstream
        pingOne(id)
    }

    fun pingOne(id: String) {
        val s = _servers.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            val p = withContext(Dispatchers.IO) { DnsClient.ping(s.primary) }
            _pings.update { it + (id to p) }
        }
    }

    fun pingAll() {
        if (_pinging.value) return
        viewModelScope.launch { runPingAll() }
    }

    private suspend fun runPingAll() {
        _pinging.value = true
        try {
            val list = _servers.value
            val results = withContext(Dispatchers.IO) {
                list.map { s -> async { s.id to DnsClient.ping(s.primary) } }.awaitAll().toMap()
            }
            _pings.value = results
        } finally {
            _pinging.value = false
        }
    }

    /** Call only after VpnService.prepare() returned null / the user approved. */
    fun connectPrepared() {
        viewModelScope.launch {
            if (_settings.value.pickFastest) {
                VpnState.status.value = VpnState.Status.CONNECTING
                runPingAll()
                _pings.value.filterValues { it > 0 }.minByOrNull { it.value }?.let { setSelected(it.key) }
            }
            LordVpnService.start(app)
        }
    }

    fun disconnect() = LordVpnService.stop(app)

    fun updateSettings(change: (ConnSettings) -> ConnSettings) {
        val old = _settings.value
        val new = change(old)
        prefs.autoConnect = new.autoConnect
        prefs.pickFastest = new.pickFastest
        prefs.ipv6 = new.ipv6
        prefs.doh = new.doh
        _settings.value = new
        if (isUp() && (old.ipv6 != new.ipv6 || old.doh != new.doh)) LordVpnService.start(app)
    }

    fun setTheme(t: AppTheme) {
        prefs.theme = t.name
        _theme.value = t
    }

    /** Returns an error message, or null when saved. */
    fun addCustom(name: String, primary: String, secondary: String): String? {
        val n = name.trim()
        val p = primary.trim()
        val s = secondary.trim()
        if (n.isEmpty()) return "Give it a name first."
        if (!validIp(p)) return "Primary DNS isn't a valid IP address."
        if (s.isNotEmpty() && !validIp(s)) return "Secondary DNS isn't a valid IP address."
        val server = DnsServer(
            id = "custom_${System.currentTimeMillis()}", name = n, primary = p,
            secondary = s.ifEmpty { null }, category = Category.CUSTOM, glyph = Glyph.CUSTOM,
        )
        prefs.saveCustom(prefs.customServers() + server)
        _servers.value = prefs.allServers()
        pingOne(server.id)
        return null
    }

    fun deleteCustom(id: String) {
        prefs.saveCustom(prefs.customServers().filterNot { it.id == id })
        _servers.value = prefs.allServers()
        if (_selectedId.value == id) select("cloudflare")
    }
}
