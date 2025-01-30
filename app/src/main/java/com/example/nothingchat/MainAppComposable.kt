package com.example.nothingchat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nothingchat.feature.auth.forgetpassword.ForgetPasswordScreen
import com.example.nothingchat.feature.auth.signin.SignInScreen
import com.example.nothingchat.feature.auth.signout.SignOutScreen
import com.example.nothingchat.feature.auth.signup.SignUpScreen
import com.example.nothingchat.feature.chat.ChatScreen
import com.example.nothingchat.feature.home.HomeScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp(modifier: Modifier = Modifier){
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) "home" else "login"



        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                SignInScreen(navController)
            }
            composable("signup") {
                SignUpScreen(navController)
            }
            composable("home") {
                HomeScreen(navController)
            }
            composable(
                route = "chat/{channelID}/{channelName}/{isAIChannel}",
                arguments = listOf(
                    navArgument("channelID") { type = NavType.StringType },
                    navArgument("channelName") { type = NavType.StringType },
                    navArgument("isAIChannel") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                val channelID = backStackEntry.arguments?.getString("channelID") ?: ""
                val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                val isAIChannel = backStackEntry.arguments?.getBoolean("isAIChannel") ?: true

                ChatScreen(navController = navController,channelID,channelName,isAIChannel)
            }
            composable("signout") {
                SignOutScreen(navController)
            }
            composable("forgetpass") {
                ForgetPasswordScreen(navController)
            }






        }
    }
}