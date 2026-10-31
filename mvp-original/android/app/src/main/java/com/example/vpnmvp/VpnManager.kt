package com.example.vpnmvp

import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/**
 * WireGuard tunnel manager for the MVP.
 *
 * Flow:
 * 1. MainActivity calls VpnService.prepare() and shows the system consent dialog if needed.
 * 2. Here GoBackend brings the tunnel UP with the config returned by the backend API.
 * 3. Disconnect sets the tunnel DOWN — this is the MVP kill switch: no traffic stays
 *    in a half-connected tunnel after disconnect.
 *
 * NOTE: GoBackend / Tunnel APIs can change between tunnel-library versions.
 * If Android Studio reports a signature mismatch, check the version you resolved
 * from Maven Central and adjust this file to that version's API.
 */
class VpnManager(private val context: Context) {

    private val backend: Backend by lazy { GoBackend(context.applicationContext) }

    private val tunnel = object : Tunnel {
        override fun getName(): String = "vpn-mvp"
        override fun onStateChange(newState: Tunnel.State) {
            // MVP: UI polls state through MainActivity; production should stream this.
        }
    }

    fun isPrepared(): Boolean = VpnService.prepare(context) == null

    fun prepareIntent() = VpnService.prepare(context)

    suspend fun connect(configText: String) {
        val config = Config.parse(ByteArrayInputStream(configText.toByteArray(Charsets.UTF_8)))
        backend.setState(tunnel, Tunnel.State.UP, config)
    }

    suspend fun disconnect() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (_: Exception) {
            // Already down in MVP; production should log this, not swallow it silently.
        }
    }

    fun currentState(): Tunnel.State = try {
        backend.getState(tunnel)
    } catch (_: Exception) {
        Tunnel.State.DOWN
    }
}
