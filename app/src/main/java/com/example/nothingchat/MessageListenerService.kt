package com.example.nothingchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database

class MessageListenerService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (!userId.isNullOrEmpty()) {
            listenForPinnedChannelUpdates()
        }

        startForeground(
            1,
            NotificationCompat.Builder(this, "foreground_service")
                .setContentTitle("Listening for messages")
                .setContentText("Receiving updates from pinned channels")
                .setSmallIcon(R.drawable.icon)
                .build()
        )
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

        // Get the current user's ID
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        messagesRef.limitToLast(1).get().addOnSuccessListener { snapshot ->
            val lastTimestamp = snapshot.children.firstOrNull()
                ?.child("createdAt")?.getValue(Long::class.java) ?: System.currentTimeMillis()

            val query = messagesRef.orderByChild("createdAt").startAt(lastTimestamp.toDouble() + 1)
            query.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""

                    // Skip notification if the sender is the current user
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
        Log.d("Notification", "Showing notification for channel $channelId: $sender: $message")

        val notificationId = channelId.hashCode()
        val notification = NotificationCompat.Builder(this, "channel_notifications")
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("New message from $sender")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        val name = "Channel Notifications"
        val descriptionText = "Notifications for messages in pinned channels"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("channel_notifications", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
