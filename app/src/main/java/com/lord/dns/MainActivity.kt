package com.lord.dns

import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lord.dns.ui.HomeScreen
import com.lord.dns.ui.MainViewModel
import com.lord.dns.ui.ServersScreen
import com.lord.dns.ui.SettingsScreen
import com.lord.dns.ui.theme.LordTheme
import com.lord.dns.ui.theme.scheme
import com.lord.dns.vpn.VpnState

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) vm.connectPrepared()
        else Toast.makeText(this, "Lord DNS needs VPN permission to change your DNS", Toast.LENGTH_LONG).show()
    }

    private fun requestConnect() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermission.launch(intent) else vm.connectPrepared()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null && vm.prefs.autoConnect &&
            VpnState.status.value == VpnState.Status.DISCONNECTED && VpnService.prepare(this) == null
        ) vm.connectPrepared()

        setContent {
            val theme by vm.theme.collectAsStateWithLifecycle()
            val scheme = theme.scheme(isSystemInDarkTheme())
            val dark = scheme.background.luminance() < 0.5f
            LaunchedEffect(dark) {
                val t = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            LordTheme(scheme) { LordApp(vm, ::requestConnect) }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Rounded.Home),
    Tab("Servers", Icons.Rounded.Public),
    Tab("Settings", Icons.Rounded.Settings),
)

@Composable
private fun LordApp(vm: MainViewModel, onConnect: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme
    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            NavigationBar(containerColor = cs.surface, tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary,
                            indicatorColor = cs.primary.copy(alpha = 0.12f),
                            unselectedIconColor = cs.onSurfaceVariant,
                            unselectedTextColor = cs.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                0 -> HomeScreen(vm, onConnect) { tab = 1 }
                1 -> ServersScreen(vm)
                else -> SettingsScreen(vm)
            }
        }
    }
}
