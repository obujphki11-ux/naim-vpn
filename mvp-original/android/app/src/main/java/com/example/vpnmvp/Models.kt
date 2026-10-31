package com.example.vpnmvp

data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val load: Int,
    val status: String,
    val type: String = "",
)

enum class VpnState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class LoginResult(val token: String, val username: String)
