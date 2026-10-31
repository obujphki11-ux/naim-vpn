package com.example.vpnmvp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * MVP UI: login -> server list -> Connect / Disconnect.
 * This is intentionally small. Production needs: token storage (EncryptedSharedPreferences),
 * auto-reconnect, live speed, split tunneling, and a proper settings screen.
 */
class MainActivity : ComponentActivity() {

    private lateinit var vpnManager: VpnManager
    private var pendingServer: VpnServer? = null
    private var onPrepared: (() -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User answered the system VPN dialog. If they allowed it, continue the pending connect.
        if (vpnManager.isPrepared()) {
            onPrepared?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vpnManager = VpnManager(this)
        setContent { VpnMvpScreen() }
    }

    @Composable
    private fun VpnMvpScreen() {
        var username by remember { mutableStateOf("demo") }
        var password by remember { mutableStateOf("demo123") }
        var token by remember { mutableStateOf<String?>(null) }
        var servers by remember { mutableStateOf<List<VpnServer>>(emptyList()) }
        var selected by remember { mutableStateOf<VpnServer?>(null) }
        var state by remember { mutableStateOf(VpnState.DISCONNECTED) }
        var message by remember { mutableStateOf("MVP — প্রথমে Backend-এর আসল IP বসাও (ApiClient.kt)") }
        val scope = rememberCoroutineScope()

        fun connectTo(server: VpnServer, authToken: String) {
            scope.launch {
                state = VpnState.CONNECTING
                message = "${server.name}-এ কানেক্ট হচ্ছে..."
                try {
                    val config = ApiClient.getConfig(authToken, server.id)
                    vpnManager.connect(config)
                    state = VpnState.CONNECTED
                    message = "${server.name}-এ Connected"
                } catch (e: Exception) {
                    state = VpnState.ERROR
                    message = "Connect হয়নি: ${e.message}"
                }
            }
        }

        fun startConnect(server: VpnServer, authToken: String) {
            pendingServer = server
            if (vpnManager.isPrepared()) {
                connectTo(server, authToken)
            } else {
                onPrepared = { connectTo(server, authToken) }
                val intent = vpnManager.prepareIntent()
                if (intent != null) vpnPermissionLauncher.launch(intent)
            }
        }

        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VPN MVP", style = MaterialTheme.typography.headlineMedium)
                    Text("স্ট্যাটাস: $state", style = MaterialTheme.typography.bodyMedium)
                    Text(message, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))

                    if (token == null) {
                        OutlinedTextField(
                            value = username, onValueChange = { username = it },
                            label = { Text("Username") }, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text("Password") }, modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scope.launch {
                                try {
                                    val result = ApiClient.login(username, password)
                                    token = result.token
                                    servers = ApiClient.getServers()
                                    message = "লগইন হয়েছে, সার্ভার বেছে নাও"
                                } catch (e: Exception) {
                                    message = "লগইন হয়নি: ${e.message}"
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Login + Load Servers") }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(servers) { server ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    onClick = { selected = server }
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${server.name} — ${server.city}, ${server.country}")
                                        Text("Load: ${server.load}% • ${server.status} • ${server.type}")
                                        if (selected?.id == server.id) {
                                            Text("✓ Selected", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val server = selected ?: servers.firstOrNull()
                                val auth = token
                                if (server != null && auth != null) startConnect(server, auth)
                                else message = "আগে একটা সার্ভার বেছে নাও"
                            },
                            enabled = state != VpnState.CONNECTING,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (state == VpnState.CONNECTED) "Reconnect" else "Connect") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    vpnManager.disconnect()
                                    state = VpnState.DISCONNECTED
                                    message = "Disconnected"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Disconnect (Kill Switch: tunnel পুরো বন্ধ)") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "MVP সতর্কতা: আসল ইউজারের জন্য HTTPS, Play Billing আর Privacy Policy লাগবে।",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Deprecated("Kept only to avoid an unused-import warning in some toolchains")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}
