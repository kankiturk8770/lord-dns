package com.lord.dns.net

import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.random.Random

object DnsClient {

    /** Builds a minimal "A record" query. Used for latency tests. */
    fun buildQuery(host: String): ByteArray {
        val out = ByteArrayOutputStream()
        val id = Random.nextInt(0, 0xFFFF)
        out.write(id ushr 8); out.write(id and 0xFF)
        out.write(0x01); out.write(0x00)          // recursion desired
        out.write(0); out.write(1)                // QDCOUNT = 1
        repeat(6) { out.write(0) }                // AN/NS/AR = 0
        host.split('.').forEach { label ->
            out.write(label.length)
            out.write(label.toByteArray())
        }
        out.write(0)
        out.write(0); out.write(1)                // QTYPE A
        out.write(0); out.write(1)                // QCLASS IN
        return out.toByteArray()
    }

    fun udp(
        server: String,
        query: ByteArray,
        timeoutMs: Int,
        protect: ((DatagramSocket) -> Unit)? = null,
    ): ByteArray? = try {
        DatagramSocket().use { socket ->
            protect?.invoke(socket)
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(server) // numeric literal, no lookup
            socket.send(DatagramPacket(query, query.size, address, 53))
            val buf = ByteArray(4096)
            val packet = DatagramPacket(buf, buf.size)
            socket.receive(packet)
            buf.copyOf(packet.length)
        }
    } catch (e: Exception) {
        null
    }

    fun doh(url: String, query: ByteArray, timeoutMs: Int): ByteArray? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.connectTimeout = timeoutMs
        c.readTimeout = timeoutMs
        c.setRequestProperty("Content-Type", "application/dns-message")
        c.setRequestProperty("Accept", "application/dns-message")
        c.outputStream.use { it.write(query) }
        if (c.responseCode == 200) c.inputStream.use { it.readBytes() } else null
    } catch (e: Exception) {
        null
    }

    /** Real DNS round-trip time in ms, or -1 on timeout. */
    fun ping(server: String): Int {
        val q = buildQuery("google.com")
        val start = System.nanoTime()
        val r = udp(server, q, 2000) ?: return -1
        if (r.size < 12) return -1
        return ((System.nanoTime() - start) / 1_000_000).toInt().coerceAtLeast(1)
    }
}
