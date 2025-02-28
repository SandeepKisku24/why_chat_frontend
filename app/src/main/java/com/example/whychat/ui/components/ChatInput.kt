package com.example.whychat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whychat.model.TextMessage
import com.example.whychat.network.WebSocketManager
import java.util.UUID

@Composable
fun ChatInput(senderID: String, chatGroupId: String) {
    var text by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            label = { Text("Type a message") }
        )
        Button(onClick = {
            if (text.isNotEmpty()) {
                WebSocketManager.sendMessage(TextMessage(senderID, text))
                text = ""
            }
        }) {
            Text("Send")
        }
    }
}
