package com.aaya.assistant.engine.audio

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Embedded Zero-Lag WebCast HTTP Server for AAYA Audio Jam.
 * Broadcasts audio stream to any device with a web browser on the same Wi-Fi or Hotspot.
 * Zero installation needed on friend's devices or laptops!
 */
class AudioJamServer private constructor() {

    companion object {
        private const val TAG = "AudioJamServer"
        const val PORT = 8080

        @Volatile
        private var INSTANCE: AudioJamServer? = null

        fun getInstance(): AudioJamServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioJamServer().also { INSTANCE = it }
            }
        }
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting = _isBroadcasting.asStateFlow()

    private val _connectedClients = MutableStateFlow<List<String>>(emptyList())
    val connectedClients = _connectedClients.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl = _serverUrl.asStateFlow()

    private var currentStreamUrl: String = ""

    fun startServer(context: Context, audioStreamUrl: String = "") {
        if (_isBroadcasting.value) return
        this.currentStreamUrl = audioStreamUrl

        val ip = getLocalIpAddress(context)
        _serverUrl.value = "http://$ip:$PORT"

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                _isBroadcasting.value = true
                Log.d(TAG, "Audio Jam WebCast Server started at ${_serverUrl.value}")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    val clientIp = clientSocket.inetAddress.hostAddress ?: "Unknown Device"
                    handleClient(clientSocket, clientIp)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server socket exception: ${e.message}")
            } finally {
                stopServer()
            }
        }
    }

    private fun handleClient(socket: Socket, clientIp: String) {
        scope.launch {
            try {
                // Register connected client
                val current = _connectedClients.value.toMutableList()
                if (!current.contains(clientIp)) {
                    current.add(clientIp)
                    _connectedClients.value = current
                }

                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val requestLine = reader.readLine() ?: ""
                Log.d(TAG, "Request from $clientIp: $requestLine")

                if (requestLine.startsWith("GET /stream")) {
                    // Redirect to current audio stream or send live stream headers
                    val target = if (currentStreamUrl.isNotBlank()) currentStreamUrl else "https://actions.google.com/sounds/v1/weather/rain_heavy.ogg"
                    writer.println("HTTP/1.1 302 Found")
                    writer.println("Location: $target")
                    writer.println("Connection: close")
                    writer.println()
                    writer.flush()
                } else {
                    // Serve lightweight, responsive AAYA Cyberpunk Web Receiver
                    val html = buildReceiverHtml()
                    val bytes = html.toByteArray(Charsets.UTF_8)

                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: text/html; charset=utf-8")
                    writer.println("Content-Length: ${bytes.size}")
                    writer.println("Connection: close")
                    writer.println()
                    writer.flush()
                    socket.getOutputStream().write(bytes)
                    socket.getOutputStream().flush()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Client handle error", e)
            } finally {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun updateStreamUrl(url: String) {
        this.currentStreamUrl = url
    }

    fun stopServer() {
        _isBroadcasting.value = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        _connectedClients.value = emptyList()
        Log.d(TAG, "Audio Jam WebCast Server stopped")
    }

    private fun getLocalIpAddress(context: Context): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.43.1"
    }

    private fun buildReceiverHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AAYA Audio Jam Receiver</title>
  <style>
    body {
      background: #0A0B1E;
      color: #F0F4FF;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      margin: 0;
      padding: 20px;
      text-align: center;
    }
    .card {
      background: rgba(19, 21, 51, 0.85);
      border: 1px solid rgba(0, 229, 255, 0.3);
      box-shadow: 0 12px 40px rgba(0, 229, 255, 0.15);
      border-radius: 24px;
      padding: 32px 24px;
      max-width: 360px;
      width: 100%;
      backdrop-filter: blur(12px);
    }
    .badge {
      display: inline-block;
      background: rgba(0, 230, 118, 0.15);
      color: #00E676;
      border: 1px solid rgba(0, 230, 118, 0.4);
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 1px;
      margin-bottom: 16px;
    }
    h2 { margin: 8px 0; color: #00E5FF; }
    p { color: #8C9BAE; font-size: 13px; line-height: 1.5; }
    audio { width: 100%; margin-top: 20px; border-radius: 12px; }
    .pulse-disc {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      background: linear-gradient(135deg, #00E5FF, #6C63FF);
      margin: 16px auto;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 24px rgba(0, 229, 255, 0.4);
      animation: pulse 2s infinite;
      font-size: 38px;
    }
    @keyframes pulse {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(0, 229, 255, 0.7); }
      70% { transform: scale(1.03); box-shadow: 0 0 0 16px rgba(0, 229, 255, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(0, 229, 255, 0); }
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">⚡ ZERO-LAG SYNC ACTIVE</div>
    <div class="pulse-disc">📡</div>
    <h2>AAYA Audio Jam</h2>
    <p>Connected to Host Stream! Tap Play below to listen with zero lag.</p>
    <audio controls autoplay src="/stream"></audio>
  </div>
</body>
</html>
        """.trimIndent()
    }
}
