package com.example.nothingchat.feature.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingchat.GeminiService
import com.example.nothingchat.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()

    private val geminiService = GeminiService("AIzaSyDcurlzpxLzhMa1k8pAjgoCxAuEr6aapLk") // Replace with your Gemini API Key
    private val db = Firebase.database

    fun sendMessage(channelID: String, messageText: String) {
        val message = Message(
            messageId = db.reference.push().key ?: UUID.randomUUID().toString(),
            senderId = Firebase.auth.currentUser?.uid ?: "",
            message = messageText,
            createdAt = System.currentTimeMillis(),
            senderName = Firebase.auth.currentUser?.displayName ?: "",
            imageUrl = null,
            videoUrl = null
        )

        db.reference.child("messages").child(channelID).push().setValue(message)

        db.reference.child("channel").child(channelID).child("isAIChannel")
            .get().addOnSuccessListener { dataSnapshot ->
                val isAIChannel = dataSnapshot.getValue(Boolean::class.java) ?: false
                if (isAIChannel) {
                    fetchChatHistory(channelID) { history ->
                        viewModelScope.launch {
                            val aiResponse = geminiService.generateResponse(messageText, history)
                            Log.i("AI Response", "AI Response: $aiResponse")

                            val aiMessage = Message(
                                messageId = db.reference.push().key ?: UUID.randomUUID().toString(),
                                senderId = "AI",
                                message = aiResponse,
                                createdAt = System.currentTimeMillis(),
                                senderName = "Interacto Intelligence",
                                imageUrl = null,
                                videoUrl = null
                            )
                            db.reference.child("messages").child(channelID).push().setValue(aiMessage)
                        }
                    }
                }
            }
    }

    private fun fetchChatHistory(channelID: String, onComplete: (List<String>) -> Unit) {
        db.reference.child("messages").child(channelID)
            .orderByChild("createdAt")
            .limitToLast(50) // Fetch last 50 messages
            .get()
            .addOnSuccessListener { snapshot ->
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java)?.message }
                onComplete(messages)
            }
            .addOnFailureListener {
                Log.e("ChatViewModel", "Failed to fetch chat history: ${it.message}")
                onComplete(emptyList())
            }
    }

    fun listenForMessages(channelID: String) {
        db.reference.child("messages").child(channelID)
            .orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Message>()
                    snapshot.children.forEach {
                        val message = it.getValue(Message::class.java)
                        message?.let { list.add(it) }
                    }
                    _messages.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatViewModel", "Error listening for messages: ${error.message}")
                }
            })
    }
}
