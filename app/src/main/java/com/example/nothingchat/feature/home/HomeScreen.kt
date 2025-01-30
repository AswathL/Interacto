package com.example.nothingchat.feature.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nothingchat.R
import com.example.nothingchat.model.Channel
@Composable
fun HomeScreen(navController: NavController) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val channelsState = viewModel.channels.collectAsState()
    val searchQuery = remember { mutableStateOf("") }
    val showAddChannelSheet = remember { mutableStateOf(false) }
    val showAddAIChannelSheet = remember { mutableStateOf(false) }
    val joinChannelDialogChannelId = remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // State for the selected tab
    val selectedTab = remember { mutableStateOf(ChannelTab.PINNED) }

    // Filter channels based on the search query and selected tab
    val filteredChannels = when (selectedTab.value) {
        ChannelTab.PINNED -> channelsState.value.filter { it.isFavorite }
        ChannelTab.ALL -> channelsState.value
        ChannelTab.AI -> channelsState.value.filter { it.isAIChannel }
        ChannelTab.ALL_WITH_PINNED -> {
            val pinnedChannels = channelsState.value.filter { it.isFavorite }
            val otherChannels = channelsState.value.filterNot { it.isFavorite }
            pinnedChannels + otherChannels
        }
    }.filter { it.name.contains(searchQuery.value, ignoreCase = true) }

    Scaffold(
        floatingActionButton = {
            Column {
                // Button to add a regular channel
                FloatingActionButton(
                    onClick = { showAddChannelSheet.value = true },
                    containerColor = Color.Red,
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = context.getString(R.string.Add_Channel),
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        style = TextStyle(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(navController)
                SearchBar(searchQuery)

                // Tab bar for channel views
                ChannelTabBar(selectedTab = selectedTab.value, onTabSelected = { selectedTab.value = it })

                if (filteredChannels.isEmpty() && selectedTab.value == ChannelTab.PINNED) {
                    // Show a message if no channels are pinned
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No pinned channels",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Navigate to 'All Channels' to pin your favorites",
                                style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { selectedTab.value = ChannelTab.ALL },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text(text = "Go to All Channels", color = Color.White)
                            }
                        }
                    }
                } else {
                    // Show the list of channels
                    ChannelList(
                        channels = filteredChannels,
                        onChannelClick = { channel ->
                            if (channel.hasPassword) {
                                joinChannelDialogChannelId.value = channel.id
                            } else {
                                navController.navigate("chat/${channel.id}/${channel.name}/${channel.isAIChannel}")
                            }
                        },
                        onFavoriteToggle = { channelId, isFavorite ->
                            viewModel.toggleFavorite(channelId, isFavorite)
                        },
                        context = context
                    )
                }
            }
        }

        if (showAddChannelSheet.value) {
            AddChannelModalBottomSheet(
                showAddChannelSheet = showAddChannelSheet,
                onAddChannel = { channelName, channelPassword, isAIChannel ->
                    viewModel.addChannel(channelName, channelPassword, isAIChannel)
                }
            )
        }

        joinChannelDialogChannelId.value?.let { channelId ->
            JoinChannelDialog(
                viewModel = viewModel,
                channelId = channelId,
                onDismiss = { joinChannelDialogChannelId.value = null },
                onSuccess = {
                    joinChannelDialogChannelId.value = null
                    navController.navigate("chat/$channelId/${filteredChannels.find { it.id == channelId }?.name}/${filteredChannels.find { it.id == channelId }?.isAIChannel}")
                }
            )
        }
    }
}

// Enum for channel tabs
enum class ChannelTab {
    PINNED, ALL, AI, ALL_WITH_PINNED
}

// Composable for the tab bar
@Composable
fun ChannelTabBar(selectedTab: ChannelTab, onTabSelected: (ChannelTab) -> Unit) {
    val tabs = listOf(ChannelTab.PINNED, ChannelTab.ALL, ChannelTab.AI, ChannelTab.ALL_WITH_PINNED)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Color.Red // Selected tab color
                        else MaterialTheme.colorScheme.surfaceVariant // Unselected tab color (card-like)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = when (tab) {
                        ChannelTab.PINNED -> "Pinned"
                        ChannelTab.ALL -> "All"
                        ChannelTab.AI -> "AI"
                        ChannelTab.ALL_WITH_PINNED -> "All with Pinned"
                    },
                    color = if (isSelected) Color.White // Text color for selected tab
                    else MaterialTheme.colorScheme.onSurfaceVariant, // Text color for unselected tab
                    style = TextStyle(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}
@Composable
fun Header(navController: NavController) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.logo2),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Red
        )
        Text(
            text = context.getString(R.string.Channels),
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black),
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp)
        )
        Card(
            modifier = Modifier
                .clickable { navController.navigate("signout") }
                .clip(RoundedCornerShape(16.dp))
                .padding(8.dp), // Spacing around the card
            elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.person_red),
                contentDescription = "Person Icon",
                tint = Color.Red,
                modifier = Modifier.padding(10.dp) // Padding inside the card
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(searchQuery: MutableState<String>) {
    val context = LocalContext.current
    TextField(
        value = searchQuery.value,
        onValueChange = { searchQuery.value = it },
        placeholder = { Text(text = context.getString(R.string.Search)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Red,
            unfocusedIndicatorColor = Color.Transparent
        ),
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search Icon",
                tint = Color.Red
            )
        }
    )
}

@Composable
fun ChannelList(
    channels: List<Channel>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String, Boolean) -> Unit,
    context: Context
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(channels) { channel ->
            ChannelItem(
                channel = channel,
                onChannelClick = { onChannelClick(channel) },
                onFavoriteToggle = { onFavoriteToggle(channel.id, channel.isFavorite) },
                context = context
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelModalBottomSheet(
    showAddChannelSheet: MutableState<Boolean>,
    onAddChannel: (String, String, Boolean) -> Unit // Now accepts AI Channel flag
) {
    val context = LocalContext.current
    if (showAddChannelSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showAddChannelSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            val channelName = remember { mutableStateOf("") }
            val channelPassword = remember { mutableStateOf("") }
            val isAIChannel = remember { mutableStateOf(false) } // State for the AI Channel checkbox

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = context.getString(R.string.Add_Channel),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                TextField(
                    value = channelName.value,
                    onValueChange = { channelName.value = it },
                    label = { Text(context.getString(R.string.Channel_Name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Red,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                TextField(
                    value = channelPassword.value,
                    onValueChange = { channelPassword.value = it },
                    label = { Text(context.getString(R.string.Password_dialog)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Red,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Checkbox for AI Channel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isAIChannel.value,
                        onCheckedChange = { isAIChannel.value = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Create as Interacto Intelligence Channel (AI)")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { showAddChannelSheet.value = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text(text = "Cancel", color = Color.White)
                    }
                    Button(
                        onClick = {
                            if (channelName.value.isNotBlank()) {
                                // Pass the channel name, password, and AI flag
                                onAddChannel(channelName.value, channelPassword.value, isAIChannel.value)
                                showAddChannelSheet.value = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(text = context.getString(R.string.Add_Channel_Button), color = Color.White)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinChannelDialog(
    viewModel: HomeViewModel,
    channelId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val password = remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.validateChannelPassword(
                        channelId = channelId,
                        enteredPassword = password.value,
                        onSuccess = {
                            Toast.makeText(context, context.getString(R.string.Access_Granted), Toast.LENGTH_SHORT).show()
                            onSuccess()
                        },
                        onFailure = {
                            Toast.makeText(context, context.getString(R.string.Incorrect_Password), Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(context.getString(R.string.Join_Channel), color = Color.White)
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(context.getString(R.string.Cancel_Join_channel))
            }
        },
        text = {
            Column {
                Text(text = context.getString(R.string.Join_channel_pass),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                TextField(
                    value = password.value,
                    onValueChange = { password.value = it },
                    label = { Text(context.getString(R.string.password)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Red,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    )
}
@Composable
fun ChannelItem(
    channel: Channel,
    onChannelClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onChannelClick() }, // Trigger navigation when clicked
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Icon (First Letter of Channel Name)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(1).uppercase(), // Display the first letter of the channel name
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Channel Name
            Text(
                text = channel.name,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Favorite Icon
            Icon(
                imageVector =  Icons.Outlined.Star,
                contentDescription = if (channel.isFavorite) "Unpin" else "Pin",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onFavoriteToggle() },
                tint = if (channel.isFavorite) Color.Yellow else Color.Gray
            )

            // Share Icon
        }
    }
}
