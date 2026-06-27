package com.nexus.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object NexusTailscaleConnector {
    private const val PC_NODE = "http://100.107.24.67:8081"
    private const val SECURITY_CSRF = "STABLE_TOKEN_v40.44"

    suspend fun executeActionOnPc(action: String, entityId: String): Boolean = withContext(Dispatchers.IO) {
        var urlConnection: HttpURLConnection? = null
        try {
            val targetUrl = URL("$PC_NODE/api/communication/decision")
            urlConnection = targetUrl.openConnection() as HttpURLConnection
            urlConnection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Nexus-CSRF", SECURITY_CSRF)
                connectTimeout = 3000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("action", action)
                put("id", entityId)
                put("timestamp", System.currentTimeMillis())
            }
            urlConnection.outputStream.use { os ->
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                os.write(bytes, 0, bytes.size)
                os.flush()
            }
            return@withContext urlConnection.responseCode == 200
        } catch (e: Exception) {
            return@withContext false
        } finally {
            urlConnection?.disconnect()
        }
    }
}
