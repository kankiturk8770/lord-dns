package com.lord.dns.vpn

import kotlinx.coroutines.flow.MutableStateFlow

object VpnState {
    enum class Status { DISCONNECTED, CONNECTING, CONNECTED }

    val status = MutableStateFlow(Status.DISCONNECTED)
    val connectedAt = MutableStateFlow(0L)
    val queries = MutableStateFlow(0)
    val activeServer = MutableStateFlow<String?>(null)
}
