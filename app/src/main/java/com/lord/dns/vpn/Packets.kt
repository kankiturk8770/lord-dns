package com.lord.dns.vpn

class UdpQuery(
    val isV6: Boolean,
    val src: ByteArray,
    val dst: ByteArray,
    val srcPort: Int,
    val payload: ByteArray,
)

/** Tiny IPv4/IPv6 + UDP parser/builder. Only cares about DNS on port 53. */
object Packets {
    private fun u8(b: ByteArray, o: Int) = b[o].toInt() and 0xFF
    private fun u16(b: ByteArray, o: Int) = (u8(b, o) shl 8) or u8(b, o + 1)
    private fun put16(b: ByteArray, o: Int, v: Int) {
        b[o] = (v ushr 8).toByte()
        b[o + 1] = v.toByte()
    }

    fun parse(buf: ByteArray, len: Int): UdpQuery? {
        if (len < 28) return null
        return when (u8(buf, 0) ushr 4) {
            4 -> {
                val ihl = (u8(buf, 0) and 0x0F) * 4
                if (u8(buf, 9) != 17 || len < ihl + 8) null
                else udp(false, buf, len, ihl, buf.copyOfRange(12, 16), buf.copyOfRange(16, 20))
            }
            6 -> {
                if (len < 48 || u8(buf, 6) != 17) null
                else udp(true, buf, len, 40, buf.copyOfRange(8, 24), buf.copyOfRange(24, 40))
            }
            else -> null
        }
    }

    private fun udp(v6: Boolean, b: ByteArray, len: Int, off: Int, src: ByteArray, dst: ByteArray): UdpQuery? {
        val srcPort = u16(b, off)
        val dstPort = u16(b, off + 2)
        val udpLen = u16(b, off + 4)
        val end = minOf(off + udpLen, len)
        if (dstPort != 53 || end <= off + 8) return null
        return UdpQuery(v6, src, dst, srcPort, b.copyOfRange(off + 8, end))
    }

    fun buildResponse(q: UdpQuery, data: ByteArray): ByteArray {
        val udpLen = 8 + data.size
        if (!q.isV6) {
            val out = ByteArray(20 + udpLen)
            out[0] = 0x45.toByte()
            put16(out, 2, out.size)
            out[6] = 0x40.toByte()       // don't fragment
            out[8] = 64.toByte()         // TTL
            out[9] = 17.toByte()         // UDP
            System.arraycopy(q.dst, 0, out, 12, 4)
            System.arraycopy(q.src, 0, out, 16, 4)
            put16(out, 10, fold(sum(out, 0, 20, 0L)))
            put16(out, 20, 53)
            put16(out, 22, q.srcPort)
            put16(out, 24, udpLen)       // UDP checksum 0 = not used (valid on IPv4)
            System.arraycopy(data, 0, out, 28, data.size)
            return out
        }
        val out = ByteArray(40 + udpLen)
        out[0] = 0x60.toByte()
        put16(out, 4, udpLen)
        out[6] = 17.toByte()
        out[7] = 64.toByte()
        System.arraycopy(q.dst, 0, out, 8, 16)
        System.arraycopy(q.src, 0, out, 24, 16)
        put16(out, 40, 53)
        put16(out, 42, q.srcPort)
        put16(out, 44, udpLen)
        System.arraycopy(data, 0, out, 48, data.size)
        val pseudo = sum(out, 8, 32, 0L) + udpLen + 17
        var cs = fold(sum(out, 40, udpLen, pseudo))
        if (cs == 0) cs = 0xFFFF
        put16(out, 46, cs)
        return out
    }

    private fun sum(b: ByteArray, off: Int, len: Int, start: Long): Long {
        var s = start
        var i = off
        val end = off + len
        while (i + 1 < end) { s += u16(b, i); i += 2 }
        if (i < end) s += (u8(b, i) shl 8)
        return s
    }

    private fun fold(s0: Long): Int {
        var s = s0
        while ((s ushr 16) != 0L) s = (s and 0xFFFFL) + (s ushr 16)
        return (s.inv() and 0xFFFFL).toInt()
    }
}
