package com.example.whychat.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.whychat.model.Message
import com.example.whychat.model.TextMessage
import com.example.whychat.network.ApiClient
import com.example.whychat.network.WebSocketManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(senderId: String, chatGroupId: String) {
    val messages = remember { mutableStateListOf<Message>() }
    val messageIds = remember { mutableSetOf<String>() }
    val scope = rememberCoroutineScope()
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var selectedMessage by remember { mutableStateOf<Message?>(null) }  // Holds the selected message for deletion
    var showDeleteDialog by remember { mutableStateOf(false) }  // Controls delete confirmation popup

    // Fetch messages and setup WebSocket
    LaunchedEffect(Unit) {
        try {
            val previousMessages = ApiClient.fetchMessages(chatGroupId)
            messages.clear()
            messageIds.clear()

            previousMessages.forEach { msg ->
                if (messageIds.add(msg.MessageID)) {
                    messages.add(msg)
                }
            }

            WebSocketManager.connect(chatGroupId)

            scope.launch {
                WebSocketManager.messages.collect { newMsg ->
                    if (messageIds.add(newMsg.MessageID)) {
                        val index = messages.indexOfFirst { it.MessageID.startsWith("temp-") && it.SenderID == newMsg.SenderID }

                        if (index != -1) {
                            messages[index] = newMsg
                        } else {
                            messages.add(newMsg)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Error fetching messages: ${e.message}")
        }
    }

    // Cleanup WebSocket on exit
    DisposableEffect(Unit) {
        onDispose {
            WebSocketManager.disconnect()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            if (selectedMessage != null) {
                TopAppBar(
                    title = { Text("1 selected") },
                    actions = {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Message")
                        }
                    }
                )
            } else {
                TopAppBar(title = { Text("Hi - $senderId") })
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            val tempId = "temp-${System.currentTimeMillis()}"
                            val messageToSend = TextMessage(
                                SenderID = senderId,
                                Message = newMessage
                            )

                            WebSocketManager.sendMessage(messageToSend)

                            val tempMessage = Message(
                                MessageID = tempId,  // Temporary ID
                                ChatGroupID = chatGroupId,
                                SenderID = messageToSend.SenderID,
                                Message = messageToSend.Message,
                                timestamp = System.currentTimeMillis().toString(),
                                MessageType = "text",
                                IsDeleted = false
                            )

                            if (messageIds.add(tempMessage.MessageID)) {
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
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
                            .fillMaxWidth(0.75f)
                            .then(cardPadding)
                            .combinedClickable(
                                onClick = {
                                    if (selectedMessage != null) {
                                        selectedMessage = null
                                    }
                                },
                                onLongClick = {
                                    if (isSentByUser) {  // Only allow selection for user's own messages
                                        selectedMessage = message
                                    }
                                }
                            ),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMessage == message)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else if (isSentByUser)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = message.SenderID, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (message.IsDeleted) "Message deleted" else message.Message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Message") },
                text = { Text("Are you sure you want to delete this message?") },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedMessage?.let { message ->
                                scope.launch {
                                    val actualMessageId = message.MessageID
                                    if (!actualMessageId.startsWith("temp-")) {
                                        val deleted = ApiClient.deleteMessage(actualMessageId)
                                        if (deleted) {
                                            val index = messages.indexOfFirst { it.MessageID == actualMessageId }
                                            if (index != -1) {
                                                messages[index] = message.copy(IsDeleted = true)
                                            }
                                        }
                                    }
                                }
                            }
                            selectedMessage = null
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        selectedMessage = null
                        showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
