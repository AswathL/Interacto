package com.example.nothingchat.feature.auth.forgetpassword

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.nothingchat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgetPasswordScreen(navController: NavController) {
    val viewModel: ForgetPasswordViewModel = hiltViewModel()
    val uiState = viewModel.state.collectAsState()
    var email by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.value) {
        when (uiState.value) {
            is ForgetPasswordState.Success -> {
                Toast.makeText(context, context.getString(R.string.forgetpass_toast), Toast.LENGTH_SHORT).show()
                navController.navigate("login")
            }
            is ForgetPasswordState.Error -> {
                Toast.makeText(context, context.getString(R.string.forgetpass_error), Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = context.getString(R.string.emial)) },
                placeholder = { Text(text = context.getString(R.string.email_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.size(16.dp))
            if (uiState.value == ForgetPasswordState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.resetPassword(email) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotEmpty()
                ) {
                    Text(text = context.getString(R.string.reset_pass))
                }
                Spacer(modifier = Modifier.size(8.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(text = context.getString(R.string.Back_to_Sign_In))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgetPasswordScreenPreview() {
    ForgetPasswordScreen(navController = rememberNavController())
}
