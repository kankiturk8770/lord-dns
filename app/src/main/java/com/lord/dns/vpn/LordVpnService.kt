package com.lord.dns.vpn

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.lord.dns.MainActivity
import com.lord.dns.data.DnsServer
import com.lord.dns.data.Prefs
import com.lord.dns.net.DnsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Local-only VPN: no traffic leaves through us except DNS.
 * We route a fake DNS address into the tunnel, catch every query sent to it
 * and forward it to the chosen provider (plain UDP or DNS-over-HTTPS).
 */
class LordVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.lord.dns.START"
        const val ACTION_STOP = "com.lord.dns.STOP"
        private const val V4_ADDR = "10.111.222.1"
        private const val V4_DNS = "10.111.222.2"
        private const val V6_ADDR = "fd00:6c6f:7264::1"
        private const val V6_DNS = "fd00:6c6f:7264::2"

        fun start(ctx: Context) {
            ctx.startService(Intent(ctx, LordVpnService::class.java).setAction(ACTION_START))
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, LordVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var session: Session? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            teardown()
            stopSelf()
            return START_NOT_STICKY
        }
        if (prepare(this) != null) {          // permission was revoked
            teardown()
            stopSelf()
            return START_NOT_STICKY
        }
        startTunnel()
        return START_STICKY
    }

    private fun startTunnel() {
        val prefs = Prefs(this)
        val server = prefs.selected()
        val wasUp = VpnState.status.value == VpnState.Status.CONNECTED
        if (!wasUp) VpnState.status.value = VpnState.Status.CONNECTING

        session?.end()
        session = null

        val builder = Builder()
            .setSession("Lord DNS · ${server.name}")
            .setMtu(1500)
            .addAddress(V4_ADDR, 32)
            .addDnsServer(V4_DNS)
            .addRoute(V4_DNS, 32)
        if (prefs.ipv6) {
            builder.addAddress(V6_ADDR, 128).addDnsServer(V6_DNS).addRoute(V6_DNS, 128)
        }
        // Our own sockets (upstream DNS, DoH, ping tests) bypass the tunnel.
        try { builder.addDisallowedApplication(packageName) } catch (_: PackageManager.NameNotFoundException) {}
        builder.setConfigureIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        )

        val fd = try { builder.establish() } catch (e: Exception) { null }
        if (fd == null) {
            teardown()
            stopSelf()
            return
        }

        session = Session(fd, server, prefs.doh && server.doh != null).also { it.begin() }
        if (!wasUp) {
            VpnState.connectedAt.value = System.currentTimeMillis()
            VpnState.queries.value = 0
        }
        VpnState.activeServer.value = server.id
        VpnState.status.value = VpnState.Status.CONNECTED
    }

    private fun teardown() {
        session?.end()
        session = null
        VpnState.status.value = VpnState.Status.DISCONNECTED
        VpnState.connectedAt.value = 0L
        VpnState.activeServer.value = null
    }

    override fun onRevoke() {
        teardown()
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        teardown()
        scope.cancel()
        super.onDestroy()
    }

    private inner class Session(
        private val fd: ParcelFileDescriptor,
        private val server: DnsServer,
        private val useDoh: Boolean,
    ) {
        @Volatile private var running = true
        private val out = FileOutputStream(fd.fileDescriptor)
        private val thread = Thread({ loop() }, "lord-dns-tun")

        fun begin() = thread.start()

        fun end() {
            running = false
            try { fd.close() } catch (_: Exception) {}
        }

        private fun loop() {
            val input = FileInputStream(fd.fileDescriptor)
            val buf = ByteArray(32767)
            val poll = StructPollfd()
            poll.fd = fd.fileDescriptor
            poll.events = OsConstants.POLLIN.toShort()
            val badMask = OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL

            while (running) {
                poll.revents = 0
                val ready = try {
                    Os.poll(arrayOf(poll), 500)
                } catch (e: ErrnoException) {
                    if (e.errno == OsConstants.EINTR) continue else break
                }
                if (!running) break
                if (ready == 0) continue
                if ((poll.revents.toInt() and badMask) != 0) break

                val n = try { input.read(buf) } catch (e: IOException) { if (running) continue else break }
                if (n <= 0) continue
                val query = Packets.parse(buf, n) ?: continue
                scope.launch { answer(query) }
            }
        }

        private fun answer(q: UdpQuery) {
            var resp: ByteArray? = null
            val dohUrl = server.doh
            if (useDoh && dohUrl != null) resp = DnsClient.doh(dohUrl, q.payload, 4000)
            if (resp == null) {
                for (addr in server.addresses) {
                    resp = DnsClient.udp(addr, q.payload, 3000) { s -> protect(s) }
                    if (resp != null) break
                }
            }
            val data = resp ?: return
            if (!running) return
            val packet = Packets.buildResponse(q, data)
            try {
                synchronized(out) { out.write(packet) }
                VpnState.queries.update { it + 1 }
            } catch (_: IOException) {
            }
        }
    }
}
