package com.example.whychat.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.example.whychat.model.Message
import com.example.whychat.model.TextMessage
import com.example.whychat.network.ApiClient
import com.example.whychat.network.WebSocketManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(senderId:String, chatGroupId:String) {
    val messages = remember { mutableStateListOf<Message>() }
    val messageIds = remember { mutableSetOf<String>() } // Use normal Set
    val scope = rememberCoroutineScope()
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState() // LazyColumn state for scrolling

    LaunchedEffect(Unit) {
        try {
            val previousMessages = ApiClient.fetchMessages(chatGroupId)
            messages.clear()
            messageIds.clear()

            previousMessages.forEach { msg ->
                if (messageIds.add(msg.MessageID)) { // Check for duplicates
                    messages.add(msg)
                }
            }

            WebSocketManager.connect(chatGroupId)

            scope.launch {
                WebSocketManager.messages.collect { newMsg ->
                    if (messageIds.add(newMsg.MessageID) && newMsg.SenderID != senderId) { // Prevent duplicate messages
                        messages.add(newMsg)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Error fetching messages: ${e.message}")
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            WebSocketManager.disconnect() // Disconnect WebSocket when leaving screen
        }
    }

    // Auto-scroll when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hi - $senderId") }) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Button(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            val messageToSend = TextMessage(
                                SenderID = senderId,
                                Message = newMessage
                            )

                            WebSocketManager.sendMessage(messageToSend)

                            val tempMessage = Message(
                                MessageID = "temp-${System.currentTimeMillis()}",
                                ChatGroupID = chatGroupId,
                                SenderID = messageToSend.SenderID,
                                Message = messageToSend.Message,
                                timestamp = System.currentTimeMillis().toString(),
                                MessageType = "text"
                            )

                            if (messageIds.add(tempMessage.MessageID)) { // Avoid duplicates
                                messages.add(tempMessage)
                            }

                            newMessage = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                val isSentByUser = message.SenderID == senderId
                val alignment = if (isSentByUser) Arrangement.End else Arrangement.Start
                val cardPadding = if (isSentByUser) Modifier.padding(start = 50.dp) else Modifier.padding(end = 50.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = alignment
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.75f) // Messages should not be full width
                            .then(cardPadding),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSentByUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = message.SenderID, style = MaterialTheme.typography.labelMedium)
                            Text(text = message.Message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

    }
}
