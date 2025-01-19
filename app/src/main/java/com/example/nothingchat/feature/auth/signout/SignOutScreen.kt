package com.example.nothingchat.feature.auth.signout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun SignOutScreen(navController: NavController, viewModel: SignOutViewModel = hiltViewModel()) {
    val user = viewModel.currentUser
    val context = LocalContext.current

    Scaffold {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.logo2), contentDescription = "")
            // Title
            Text(
                text = context.getString(R.string.profile),
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Black),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Info Card
            Card(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "${context.getString(R.string.user_name)}: ${user?.displayName ?: "Unknown"}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                        modifier = Modifier.padding(20.dp)
                    )
                    Text(
                        text = "${context.getString(R.string.emial)}: ${user?.email ?: "Unknown"}",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            // Logout Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Color.Red)
                    .clickable {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.logout),
                    color = Color.White,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
