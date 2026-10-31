package com.naimvpn.app

// Naim VPN MVP - UI skeleton (server list from backend API, Connect button).
// Real tunnel: Android VpnService + WireGuard - starts only after a real
// server config exists. Until then Connect shows "server not configured",
// exactly like the backend does. No fake connection.
import android.app.Activity
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48,48,48,48) }
        root.addView(TextView(this).apply { text = "Naim VPN"; textSize = 28f })
        root.addView(TextView(this).apply { text = "Servers: Singapore (not configured) • India Mumbai (not configured)" })
        root.addView(Button(this).apply {
            text = "Connect"
            setOnClickListener {
                Toast.makeText(this@MainActivity,
                    "Server not configured yet - deploy a VPS first", Toast.LENGTH_LONG).show()
            }
        })
        root.addView(Button(this).apply { text = "Disconnect" })
        root.addView(CheckBox(this).apply { text = "Kill Switch" })
        setContentView(root)
    }
}
