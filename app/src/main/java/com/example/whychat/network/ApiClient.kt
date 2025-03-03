package com.example.whychat.network

import android.util.Log
import com.example.whychat.model.Message
import com.example.whychat.model.MessagesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    suspend fun fetchMessages(chatGroupId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.1.42:8080/messages?chat_group_id=$chatGroupId")
                Log.d("DEBUG", "Fetching messages from: $url")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DEBUG", "Raw API Response: $response")

                    val messagesResponse = Json.decodeFromString<MessagesResponse>(response)
                    messagesResponse.messages
                } else {
                    Log.e("ERROR", "Failed to fetch messages. Response Code: $responseCode")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("ERROR", "Exception in fetchMessages: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun deleteMessage(messageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://192.168.1.42:8080/delete-message") // Ensure the backend has this route
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val jsonBody = Json.encodeToString(mapOf("message_id" to messageId))
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(jsonBody)
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("DEBUG", "Message deleted successfully")
                    true
                } else {
                    Log.e("ERROR", "Failed to delete message. Response Code: $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e("ERROR", "Exception in deleteMessage: ${e.message}")
                false
            }
        }
    }
}
