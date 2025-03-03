package com.example.whychat.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.whychat.model.Message
import com.example.whychat.model.TextMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var shouldReconnect = true
    private val _messages = MutableSharedFlow<Message>(replay = 1) // Ensures last message is available
    val messages = _messages.asSharedFlow()
    fun connect(chatGroupId: String) {
        val request = Request.Builder()
            .url("ws://192.168.1.42:8080/ws?chat_group_id=$chatGroupId")
            .build()
        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected successfully")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocket", "Received raw message: $text")
                    val message = Json.decodeFromString<Message>(text)
                    _messages.tryEmit(message) // Emit message to UI
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error decoding message: ${e.message}")
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed: ${t.message}")
                if (shouldReconnect) {
                    Handler(Looper.getMainLooper()).postDelayed({ connect(chatGroupId) }, 3000) // Retry after 3s
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
            }
        })
    }

    fun sendMessage(message: TextMessage) {
        val jsonMessage = Json.encodeToString(message)
        Log.d("WebSocket", "Sending message: $jsonMessage")
        webSocket?.send(jsonMessage)
    }

    fun disconnect() {
        shouldReconnect = false
        webSocket?.close(1000, "Closing WebSocket")
        webSocket = null
    }

    private fun reconnect(chatGroupId: String) {
        Log.d("WebSocket", "Reconnecting...")
        disconnect()
        connect(chatGroupId)
    }
}
