package com.example.vpnmvp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MVP API client. Uses HttpURLConnection + org.json only, so no Retrofit setup is needed.
 * TODO (production): switch API_BASE_URL to https://your-domain, add certificate pinning
 * and proper error handling for expired tokens.
 */
object ApiClient {
    // TODO: Replace with your real backend URL before testing on a phone.
    const val API_BASE_URL = "http://YOUR_SERVER_IP:8000"

    private fun readAll(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return BufferedReader(InputStreamReader(stream)).use { it.readText() }
    }

    suspend fun getServers(): List<VpnServer> = withContext(Dispatchers.IO) {
        val conn = URL("$API_BASE_URL/api/servers").openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        val body = readAll(conn)
        if (conn.responseCode != 200) throw Exception("Server list failed: ${conn.responseCode}")
        val servers = JSONObject(body).getJSONArray("servers")
        List(servers.length()) { i ->
            val s = servers.getJSONObject(i)
            VpnServer(
                id = s.getString("id"),
                name = s.optString("name", s.getString("id")),
                country = s.optString("country", ""),
                city = s.optString("city", ""),
                load = s.optInt("load", 0),
                status = s.optString("status", "online"),
                type = s.optString("type", ""),
            )
        }
    }

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val conn = URL("$API_BASE_URL/api/login").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        val payload = JSONObject().put("username", username).put("password", password).toString()
        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        val body = readAll(conn)
        if (conn.responseCode != 200) throw Exception(JSONObject(body).optString("error", "Login failed"))
        val json = JSONObject(body)
        LoginResult(token = json.getString("token"), username = json.getString("username"))
    }

    suspend fun getConfig(token: String, serverId: String): String = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(serverId, "UTF-8")
        val conn = URL("$API_BASE_URL/api/config?server_id=$encoded").openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        val body = readAll(conn)
        if (conn.responseCode != 200) throw Exception(JSONObject(body).optString("error", "Config failed"))
        JSONObject(body).getString("config")
    }
}
