package com.lord.dns.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("lord_dns", Context.MODE_PRIVATE)

    var selectedId: String
        get() = sp.getString("selected", "cloudflare") ?: "cloudflare"
        set(v) { sp.edit().putString("selected", v).apply() }

    var autoConnect: Boolean
        get() = sp.getBoolean("auto_connect", false)
        set(v) { sp.edit().putBoolean("auto_connect", v).apply() }

    var pickFastest: Boolean
        get() = sp.getBoolean("pick_fastest", false)
        set(v) { sp.edit().putBoolean("pick_fastest", v).apply() }

    var ipv6: Boolean
        get() = sp.getBoolean("ipv6", false)
        set(v) { sp.edit().putBoolean("ipv6", v).apply() }

    var doh: Boolean
        get() = sp.getBoolean("doh", false)
        set(v) { sp.edit().putBoolean("doh", v).apply() }

    var theme: String
        get() = sp.getString("theme", "LIGHT") ?: "LIGHT"
        set(v) { sp.edit().putString("theme", v).apply() }

    fun customServers(): List<DnsServer> {
        val raw = sp.getString("custom", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DnsServer(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    primary = o.getString("p"),
                    secondary = o.optString("s").ifBlank { null },
                    category = Category.CUSTOM,
                    glyph = Glyph.CUSTOM,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustom(list: List<DnsServer>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().put("id", s.id).put("name", s.name).put("p", s.primary).put("s", s.secondary ?: ""))
        }
        sp.edit().putString("custom", arr.toString()).apply()
    }

    fun allServers(): List<DnsServer> = Defaults.servers + customServers()

    fun selected(): DnsServer = allServers().firstOrNull { it.id == selectedId } ?: Defaults.servers.first()
}
