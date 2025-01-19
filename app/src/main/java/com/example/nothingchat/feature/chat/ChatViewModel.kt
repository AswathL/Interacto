package com.example.nothingchat.feature.chat

import androidx.lifecycle.ViewModel
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(): ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()
    val db = Firebase.database

    fun sendMessage(channelID: String,messageText: String){
        val message = Message(
            db.reference.push().key?: UUID.randomUUID().toString(),
            Firebase.auth.currentUser?.uid ?: "",
            messageText,
            System.currentTimeMillis(),
            Firebase.auth.currentUser?.displayName ?: "",
            null,
            null,
            )
         db.reference.child("messages").child(channelID).push().setValue(message)
    }

    fun ListenForMessages(channelID: String){
        db.getReference("messages").child(channelID).orderByChild("createdAt").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Message>()
                snapshot.children.forEach {
                    val message = it.getValue(Message::class.java)
                    message?.let {
                        list.add(it)
                    }
                }
                _messages.value = list
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}