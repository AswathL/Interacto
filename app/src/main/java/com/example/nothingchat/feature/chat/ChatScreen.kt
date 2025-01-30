package com.example.nothingchat.feature.chat

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.text.format.DateFormat
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.example.nothingchat.R
import com.example.nothingchat.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

// Supabase client initialization
private val supabaseClient = createSupabaseClient(
    supabaseUrl = "https://yzpfzemaxmbxzdwcoftc.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl6cGZ6ZW1heG1ieHpkd2NvZnRjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzY1MjI5OTQsImV4cCI6MjA1MjA5ODk5NH0.IFdECstZ4MLKQRhEyZlCoArMx4TamFtifwKFwS92D-k"
) {
    install(Storage)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String,isAIChannel: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val channelId = channelId.removeSurrounding("{", "}")
    val channelName = channelName.removeSurrounding("{", "}")

    // Permission states
    var showPermissionDialog by remember { mutableStateOf(false) }
    val chooserDialog = remember { mutableStateOf(false) }
    val cameraImageUri = remember { mutableStateOf<Uri?>(null) }

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let { uri ->
                scope.launch {
                    uploadMediaToSupabase(
                        context,
                        uri,
                        "image",
                        channelId,
                    )
                }
            }
        }
        chooserDialog.value = false
    }
    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            createImageUri(context)?.let { uri ->
                cameraImageUri.value = uri
                cameraImageLauncher.launch(uri)
            }
        } else {
            showPermissionDialog = true
        }
    }

    // Camera launcher


    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                uploadMediaToSupabase(
                    context,
                    it,
                    "image",
                    channelId,
                )
            }
        }
        chooserDialog.value = false
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val viewModel = hiltViewModel<ChatViewModel>()
            LaunchedEffect(key1 = channelId) {
                viewModel.listenForMessages(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                messages = messages.value,
                channelName = channelName,
                onSendMessage = { viewModel.sendMessage(channelId, it) },
                onAttachClick = { chooserDialog.value = true }
            )
        }

        // Content selection dialog
        if (chooserDialog.value) {
            ContentSelectionDialog(
                onDismissRequest = { chooserDialog.value = false },
                onCameraSelected = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            createImageUri(context)?.let { uri ->
                                cameraImageUri.value = uri
                                cameraImageLauncher.launch(uri)
                            }
                        }
                        else -> {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                    chooserDialog.value = false
                },
                onGallerySelected = {
                    galleryLauncher.launch("image/*")
                }
            )
        }

        // Permission explanation dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text(context.getString(R.string.Camera_per_title)) },
                text = {
                    Text(context.getString(R.string.Camera_per_desc))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    ) {
                        Text(context.getString(R.string.Camera_per_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionDialog = false }) {
                        Text(context.getString(R.string.Cancel_dialog))
                    }
                }
            )
        }
    }
}


private fun createImageUri(context: Context): Uri? {
    return try {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir =
            ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_PICTURES).first()
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun uploadMediaToSupabase(
    context: Context,
    uri: Uri,
    type: String,
    channelId: String
) {
    withContext(Dispatchers.IO) {
        try {
            // Generate a unique file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}_${UUID.randomUUID()}.jpg"

            // Get input stream from Uri
            val inputStream = context.contentResolver.openInputStream(uri)

            // Upload to Supabase storage
            inputStream?.use { stream ->
                val bucket = supabaseClient.storage["chat-media"]

                // Upload the file
                bucket.upload(
                    path = fileName,
                    data = stream.readBytes(),
                    upsert = false
                )

                // Get the public URL
                val publicUrl = bucket.publicUrl(fileName)

                // Save the URL to Firebase
                saveUrlToFirebase(publicUrl, type, channelId, context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // You might want to handle the error more gracefully here
        }
    }
}

@Composable
fun ContentSelectionDialog(
    onDismissRequest: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onCameraSelected) {
                Text(context.getString(R.string.camera))
            }
        },
        dismissButton = {
            TextButton(onClick = onGallerySelected) {
                Text(context.getString(R.string.gallery))
            }
        },
        title = { Text(context.getString(R.string.Select_Media_title)) },
        text = { Text(context.getString(R.string.Select_Media_desc)) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
    val senderName = message.senderName
    val bubbleColor = if (isCurrentUser) Color.Red else Color.Gray
    val timestamp = message.createdAt

    var showDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var showDownloadConfirmation by remember { mutableStateOf(false) }
    var isTextSelectable by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun formatTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return DateFormat.format("h:mm a", calendar).toString()
    }

    // Format the timestamp
    val formattedTimestamp = formatTimestamp(timestamp)


    if (showDialog) {
        FullScreenImageDialog(
            imageUrl = selectedImageUrl,
            onDismiss = { showDialog = false }
        )
    }

    if (showDownloadConfirmation) {
        ConfirmationDialog(
            message = context.getString(R.string.Download_media),
            onConfirm = {
                downloadImage(context, selectedImageUrl)
                showDownloadConfirmation = false
            },
            onDismiss = { showDownloadConfirmation = false }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isCurrentUser) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Cyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (senderName.isNotEmpty()) senderName[0].uppercase().toString() else "",
                    style = TextStyle(fontSize = 24.sp, color = Color.White)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .padding(8.dp)
                .clickable {
                    // Deactivate text selection if the box itself is clicked
                    isTextSelectable = false
                }
        ) {
            Column {
                if (!message.imageUrl.isNullOrEmpty()) {
                    // Image Message
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bubbleColor)
                            .padding(8.dp)
                    ) {
                        if (!isCurrentUser) {
                            Text(text = senderName, color = Color.Cyan, fontSize = 15.sp)
                        }
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Image Message",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        selectedImageUrl = message.imageUrl
                                        showDialog = true
                                    },
                                    onLongClick = {
                                        selectedImageUrl = message.imageUrl
                                        showDownloadConfirmation = true
                                    }
                                ),
                            contentScale = ContentScale.Crop
                        )
                        // Add timestamp below image message
                        Text(
                            text = formattedTimestamp,
                            style = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                    }
                } else {
                    // Text Message with Links Highlighted and Selectable
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bubbleColor)
                            .padding(8.dp)
                    ) {
                        if (!isCurrentUser) {
                            Text(text = senderName, color = Color.Cyan, fontSize = 15.sp)
                        }

                        val annotatedString = buildAnnotatedString {
                            val words = message.message.split(" ")
                            for (word in words) {
                                if (word.startsWith("http://") || word.startsWith("https://")) {
                                    val start = length
                                    append("$word ")
                                    addStyle(
                                        style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline, fontSize = 16.sp),
                                        start = start,
                                        end = start + word.length
                                    )
                                    addStringAnnotation(
                                        tag = "URL",
                                        annotation = word,
                                        start = start,
                                        end = start + word.length
                                    )
                                } else {
                                    append("$word ")
                                    addStyle(
                                        style = SpanStyle(color = Color.White, fontSize = 16.sp),
                                        start = length - word.length - 1,
                                        end = length
                                    )
                                }
                            }
                        }

                        if (isTextSelectable) {
                            SelectionContainer {
                                ClickableText(
                                    text = annotatedString,
                                    style = TextStyle(color = Color.White),
                                    onClick = { offset ->
                                        annotatedString
                                            .getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { stringAnnotation ->
                                                val url = stringAnnotation.item
                                                openLinkInBrowser(context, url)
                                            }
                                    }
                                )
                            }
                        } else {
                            ClickableText(
                                text = annotatedString,
                                style = TextStyle(color = Color.White),
                                onClick = { offset ->
                                    annotatedString
                                        .getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { stringAnnotation ->
                                            val url = stringAnnotation.item
                                            openLinkInBrowser(context, url)
                                        }
                                    isTextSelectable = true // Activate text selection when tapped
                                }
                            )
                        }
                        // Add timestamp below text message
                        Text(
                            text = formattedTimestamp,
                            style = TextStyle(fontSize = 12.sp, color = Color.White),
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}


fun openLinkInBrowser(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}




fun downloadImage(context: Context, imageUrl: String) {
    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // Ensure the "Interacto" folder exists within the "Pictures" directory
    val interactoDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Interacto")
    if (!interactoDir.exists()) {
        interactoDir.mkdirs() // Create the directory if it doesn't exist
    }

    // Set the file name
    val fileName = "downloaded_image_${System.currentTimeMillis()}.jpg"

    val request = DownloadManager.Request(Uri.parse(imageUrl))
        .setTitle(context.getString(R.string.Download_media_notification_title))
        .setDescription(context.getString(R.string.Download_media_notification_desc))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
        .setDestinationUri(Uri.fromFile(File(interactoDir, fileName))) // Use the manually created directory

    // Enqueue the download request
    downloadManager.enqueue(request)

    Toast.makeText(context, context.getString(R.string.Download_media_toast), Toast.LENGTH_SHORT).show()
}


@Composable
fun ConfirmationDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(context.getString(R.string.confirmation)) },
        text = { Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm() }) {
                Text(context.getString(R.string.yes))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { onDismiss() }) {
                Text(context.getString(R.string.no))
            }
        }
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessages(
    messages: List<Message>,
    channelName: String,
    onSendMessage: (String) -> Unit,
    onAttachClick: () -> Unit
) {
    val context = LocalContext.current
    val msg = remember { mutableStateOf("") }
    val hideKeyboardController = LocalSoftwareKeyboardController.current

    // State for LazyColumn to handle scroll position
    val listState = rememberLazyListState()

    // Scroll to the latest message when messages change
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }


    // Group messages by date
    fun formatTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return DateFormat.format("MMM dd, yyyy", calendar).toString()
    }

    val groupedMessages = messages
        .groupBy { message -> formatTimestamp(message.createdAt) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Card(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channelName[0].uppercase(),
                            style = TextStyle(fontSize = 24.sp, color = Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = channelName,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black)
                    )
                }
            }
            LazyColumn(
                state = listState, // Attach the state to the LazyColumn
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 70.dp)
            ) {
                groupedMessages.forEach { (date, messagesForDate) ->
                    item {
                        DateDivider(date = date)
                    }
                    items(messagesForDate) { message ->
                        ChatBubble(message = message)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachClick) {
                Icon(
                    painter = painterResource(id = R.drawable.attach),
                    contentDescription = "Attach Media",
                    tint = Color.Red
                )
            }
            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { Text(text = context.getString(R.string.messgae)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    hideKeyboardController?.hide()
                }),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(onClick = {
                if (msg.value.isNotBlank()) {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.Red
                )
            }
        }
    }
}



fun saveUrlToFirebase(url: String, type: String, channelId: String,context: Context) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val senderId = currentUser?.uid ?: ""
    val senderName = currentUser?.displayName ?: context.getString(R.string.unknown_user) // Get the sender's name

    val message = Message(
        messageId = System.currentTimeMillis().toString(),
        senderId = senderId,
        senderName = senderName, // Include the sender's name
        message = "",
        createdAt = System.currentTimeMillis(),
        imageUrl = if (type == "image") url else null,
        videoUrl = if (type == "video") url else null
    )

    FirebaseDatabase.getInstance().getReference("messages/$channelId")
        .push()
        .setValue(message)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) } // Variable to control zoom scale
    var offset by remember { mutableStateOf(Offset(0f, 0f)) } // Variable to control image offset
    val context = LocalContext.current

    // Gesture detector to support pinch zoom and drag
    val scaleModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(1f, 3f) // Limit zoom level between 1x and 3x
            offset = Offset(offset.x + pan.x, offset.y + pan.y)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismiss() } // Dismiss the dialog when tapped
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Full Screen Image",
                    modifier = scaleModifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit // Make sure to preserve the image aspect ratio
                )
                Row( modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }
                    .background(Color.Red)
                    .padding(8.dp)) {

                    Text(text = context.getString(R.string.close), color = Color.White)
                    Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )


            }
            }
        }
    )
}
@Composable
fun DateDivider(date: String) {
    Column(modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {


        Divider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 32.dp),
            color = Color.LightGray,
            thickness = 1.dp
        )
        Text(
            text = date,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray),
            modifier = Modifier.padding(vertical = 8.dp)

        )
    }
}

