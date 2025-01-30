package com.example.nothingchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MessageListenerService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (!userId.isNullOrEmpty()) {
            listenForPinnedChannelUpdates()
        }

        // Start the foreground service with a notification
        try {
            val notification = NotificationCompat.Builder(this, "foreground_service")
                .setContentTitle("Listening for messages")
                .setContentText("Receiving updates from pinned channels")
                .setSmallIcon(R.drawable.icon) // Ensure this resource exists
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(1, notification)
        } catch (e: Exception) {
            Log.e("ForegroundService", "Error starting foreground service", e)
        }
    }

    private fun listenForPinnedChannelUpdates() {
        val database = Firebase.database
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val pinnedChannelsRef = database.getReference("favorites/$userId")

        pinnedChannelsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val channelId = snapshot.key ?: return
                Log.d("PinnedChannel", "Pinned channel added: $channelId")
                listenForChannelMessages(channelId)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("PinnedChannel", "Error listening for pinned channels: ${error.message}")
            }
        })
    }

    private fun listenForChannelMessages(channelId: String) {
        val messagesRef = Firebase.database.getReference("messages/$channelId")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        messagesRef.limitToLast(1).get().addOnSuccessListener { snapshot ->
            val lastTimestamp = snapshot.children.firstOrNull()
                ?.child("createdAt")?.getValue(Long::class.java) ?: System.currentTimeMillis()

            val query = messagesRef.orderByChild("createdAt").startAt(lastTimestamp.toDouble() + 1)
            query.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""

                    if (senderId == currentUserId) {
                        Log.d("ChannelMessages", "Skipping notification for own message")
                        return
                    }

                    val sender = snapshot.child("senderName").getValue(String::class.java) ?: "Unknown"
                    val messageText = snapshot.child("message").getValue(String::class.java)
                    val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                    val displayMessage = when {
                        !messageText.isNullOrEmpty() -> messageText
                        !imageUrl.isNullOrEmpty() -> "Image Message"
                        else -> "Unknown Message"
                    }

                    Log.i(
                        "ChannelMessages",
                        "New message in channel $channelId from $sender: $displayMessage"
                    )
                    showNotification(channelId, sender, displayMessage)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChannelMessages", "Error listening for messages: ${error.message}")
                }
            })
        }
    }

    private fun showNotification(channelId: String, sender: String, message: String) {
        val notificationId = channelId.hashCode()
        val notification = NotificationCompat.Builder(this, "channel_notifications")
            .setSmallIcon(R.drawable.icon) // Ensure this resource exists
            .setContentTitle("New message from $sender")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("Notification", "POST_NOTIFICATIONS permission not granted")
            return
        }

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground service notification channel
            val serviceChannel = NotificationChannel(
                "foreground_service",
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )

            // Channel for message notifications
            val messageChannel = NotificationChannel(
                "channel_notifications",
                "Channel Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for messages in pinned channels"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }
}
