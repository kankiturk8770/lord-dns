package com.lord.dns.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.lord.dns.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs(context).autoConnect) return
        if (VpnService.prepare(context) != null) return
        try { LordVpnService.start(context) } catch (_: Exception) {}
    }
}
