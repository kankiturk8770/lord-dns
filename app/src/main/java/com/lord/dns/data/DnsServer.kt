package com.lord.dns.data

enum class Category { IRAN, GLOBAL, CUSTOM }

enum class Glyph { CLOUD, GOOGLE, BOLT, SHECAN, SHIELD, QUAD9, OPEN, GLOBE, ROCKET, CUSTOM }

data class DnsServer(
    val id: String,
    val name: String,
    val primary: String,
    val secondary: String? = null,
    val doh: String? = null,
    val category: Category,
    val adBlock: Boolean = false,
    val glyph: Glyph = Glyph.CUSTOM,
    val color: Long = 0xFF64748B,
) {
    val isCustom: Boolean get() = category == Category.CUSTOM
    val addresses: List<String> get() = listOfNotNull(primary, secondary)
}

object Defaults {
    val servers = listOf(
        DnsServer("cloudflare", "Cloudflare", "1.1.1.1", "1.0.0.1",
            doh = "https://cloudflare-dns.com/dns-query", category = Category.GLOBAL,
            glyph = Glyph.CLOUD, color = 0xFFF48120),
        DnsServer("google", "Google", "8.8.8.8", "8.8.4.4",
            doh = "https://dns.google/dns-query", category = Category.GLOBAL,
            glyph = Glyph.GOOGLE, color = 0xFF4285F4),
        DnsServer("electro", "Electro", "78.157.42.100", "78.157.42.101",
            category = Category.IRAN, glyph = Glyph.BOLT, color = 0xFF6D4AFF),
        DnsServer("shecan", "Shecan", "178.22.122.100", "185.51.200.2",
            doh = "https://free.shecan.ir/dns-query", category = Category.IRAN,
            glyph = Glyph.SHECAN, color = 0xFF1E9BD7),
        DnsServer("403", "403.online", "10.202.10.202", "10.202.10.102",
            category = Category.IRAN, glyph = Glyph.ROCKET, color = 0xFF0F766E),
        DnsServer("begzar", "Begzar", "185.55.226.26", "185.55.225.25",
            category = Category.IRAN, glyph = Glyph.GLOBE, color = 0xFFDB2777),
        DnsServer("adguard", "AdGuard", "94.140.14.14", "94.140.15.15",
            doh = "https://dns.adguard-dns.com/dns-query", category = Category.GLOBAL,
            adBlock = true, glyph = Glyph.SHIELD, color = 0xFF4FA05A),
        DnsServer("quad9", "Quad9", "9.9.9.9", "149.112.112.112",
            doh = "https://dns.quad9.net/dns-query", category = Category.GLOBAL,
            glyph = Glyph.QUAD9, color = 0xFFD6246E),
        DnsServer("opendns", "OpenDNS", "208.67.222.222", "208.67.220.220",
            doh = "https://doh.opendns.com/dns-query", category = Category.GLOBAL,
            glyph = Glyph.OPEN, color = 0xFFE89A1C),
    )
}
