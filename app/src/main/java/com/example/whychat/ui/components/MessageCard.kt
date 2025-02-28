package com.example.whychat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whychat.model.Message

@Composable
fun MessageCard(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message.Message, style = MaterialTheme.typography.bodyLarge)
            Text(text = "From: ${message.SenderID}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
