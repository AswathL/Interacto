package com.example.nothingchat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nothingchat.ui.theme.NothingChatTheme
import com.example.nothingchat.utils.PreferencesUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.database
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { PreferencesUtils.applyLanguage(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MessageListenerService::class.java)
        startService(intent)



        enableEdgeToEdge()
        setContent {
            NothingChatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    private fun listenForPinnedChannelUpdates() {
        val database = Firebase.database
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

                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                    val messageId = snapshot.child("messageId").getValue(String::class.java) ?: ""
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                return
            }
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
