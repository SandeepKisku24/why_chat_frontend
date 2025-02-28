package com.example.whychat.network

import android.util.Log
import com.example.whychat.model.Message
import com.example.whychat.model.MessagesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    suspend fun fetchMessages(chatGroupId: String): List<Message> {
        return withContext(Dispatchers.IO) { // Switch to a background thread
            try {
                val url = URL("http://1xx.xxx.x.xx:8080/messages?chat_group_id=$chatGroupId")
                Log.d("DEBUG", "Fetching messages from: $url")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
//                Log.d("DEBUG", "Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DEBUG", "Raw API Response: $response")

                    val messagesResponse = Json.decodeFromString<MessagesResponse>(response)
                    Log.d("DEBUG", "Parsed Messages: ${messagesResponse.messages}")

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
}
