package com.example.nothingchat.feature.home

import androidx.lifecycle.ViewModel
import com.example.nothingchat.model.Channel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    private val firebaseDatabase = Firebase.database
    private val userId = Firebase.auth.currentUser?.uid ?: ""
    private val messagesRef: DatabaseReference = firebaseDatabase.getReference("messages")
    private val pinnedChannelsRef: DatabaseReference = firebaseDatabase.getReference("favorites")


    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    init {
        getChannels()
    }

    private fun getChannels() {
        firebaseDatabase.getReference("channel").get().addOnSuccessListener { snapshot ->
            val list = mutableListOf<Channel>()
            val favoriteChannelsRef = firebaseDatabase.getReference("favorites/$userId")

            favoriteChannelsRef.get().addOnSuccessListener { favSnapshot ->
                val favoriteIds = favSnapshot.children.map { it.key }.toSet()

                snapshot.children.forEach { data ->
                    val channelId = data.key!!
                    val channelName = data.child("name").getValue(String::class.java) ?: ""
                    val hasPassword = data.child("password").exists()

                    val channel = Channel(
                        id = channelId,
                        name = channelName,
                        isFavorite = channelId in favoriteIds,
                        hasPassword = hasPassword
                    )
                    list.add(channel)
                }
                _channels.value = list.sortedByDescending { it.isFavorite }
            }
        }
    }

    fun addChannel(name: String, password: String?) {
        val channelRef = firebaseDatabase.getReference("channel").push()
        val channelData = mutableMapOf<String, Any>("name" to name)
        if (!password.isNullOrEmpty()) {
            channelData["password"] = password
        }

        channelRef.setValue(channelData).addOnSuccessListener {
            getChannels()
        }
    }

    fun toggleFavorite(channelId: String, isFavorite: Boolean) {
        val favoriteRef = firebaseDatabase.getReference("favorites/$userId/$channelId")
        if (isFavorite) {
            favoriteRef.removeValue().addOnSuccessListener { getChannels() }
        } else {
            favoriteRef.setValue(true).addOnSuccessListener { getChannels() }
        }
    }

    fun validateChannelPassword(
        channelId: String,
        enteredPassword: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        firebaseDatabase.getReference("channel/$channelId/password").get().addOnSuccessListener { snapshot ->
            val correctPassword = snapshot.getValue(String::class.java) ?: ""
            if (enteredPassword == correctPassword) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun getChannelLink(channelId: String): String {
        return "app://channel/$channelId"
    }
}
