package com.example.nothingchat.feature.auth.signup

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nothingchat.R
import com.example.nothingchat.utils.PreferencesUtils.saveSelectedLanguage
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    val viewModel: SignInViewModel = hiltViewModel()
    val uiState = viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Language selection state
    val languages = mapOf(
        "English" to Locale.ENGLISH,
        "हिन्दी" to Locale("hi"), // Hindi
        "தமிழ்" to Locale("ta"), // Tamil
        "తెలుగు" to Locale("te"), // Telugu
        "ಕನ್ನಡ" to Locale("ka"), // Kannada
        "മലയാളം" to Locale("ml"), // Malayalam
        "मराठी" to Locale("mt"), // Marathi
        "বাংলা" to Locale("be"), // Bengali
        "ગુજરાતી" to Locale("gt"), // Gujarati
        "ଓଡ଼ିଆ" to Locale("od"), // Odia
        "ਪੰਜਾਬੀ" to Locale("pa"), // Punjabi
        "অসমীয়া" to Locale("as"), // Assamese
        "संस्कृतम्" to Locale("sa"), // Sanskrit
        "Български" to Locale("bg"), // Bulgarian
        "Русский" to Locale("ru"), // Russian
        "Español" to Locale("es"), // Spanish
        "Português" to Locale("pt"), // Portuguese
        "Deutsch" to Locale("de"), // German
        "日本語" to Locale("ja"), // Japanese
        "한국어" to Locale("ko"), // Korean
        "中文" to Locale("zh") // Chinese
    )

    var selectedLanguage by remember { mutableStateOf("Choose the language") }
    var isLanguagePickerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = uiState.value) {
        when (uiState.value) {
            is SignUpState.Success -> navController.navigate("home")
            is SignUpState.Error -> Toast.makeText(context, context.getString(R.string.signup_error), Toast.LENGTH_SHORT).show()
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

            // Language Selection Text Field
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true, // Prevent manual editing
                label = { Text(text = "Language") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { isLanguagePickerVisible = true }) {
                        Icon(
                             imageVector = Icons.Default.ArrowDropDown, // Replace with dropdown arrow
                            contentDescription = "Select Language"
                        )
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Modal Bottom Sheet for Language Picker
            if (isLanguagePickerVisible) {
                ModalBottomSheet(
                    onDismissRequest = { isLanguagePickerVisible = false }
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3), // Display 4 items per row
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp), // Add padding to the grid
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(languages.entries.toList()) { (language, locale) ->
                            Button(
                                onClick = {
                                    selectedLanguage = language
                                    updateLocale(context, locale)
                                    isLanguagePickerVisible = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )

                            ) {
                                Text(text = language,color = Color.White)
                            }
                        }
                    }
                }


            }

            // Other Text Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = context.getString(R.string.user_name)) },
                placeholder = { Text(text = context.getString(R.string.user_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = context.getString(R.string.emial)) },
                placeholder = { Text(text = context.getString(R.string.email_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotEmpty() && !email.contains("@"),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = context.getString(R.string.password)) },
                placeholder = { Text(text = context.getString(R.string.password_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(text = context.getString(R.string.confirm_password)) },
                placeholder = { Text(text = context.getString(R.string.confirm_password_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                isError = password.isNotEmpty() && confirm.isNotEmpty() && password != confirm,
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Red,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.size(16.dp))

            if (uiState.value == SignUpState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.signUp(name, email, password) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    enabled = password.isNotEmpty() && confirm.isNotEmpty() && password == confirm
                ) {
                    Text(text = context.getString(R.string.signup))
                }
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(text = context.getString(R.string.already_have_account))
                }
            }
        }
    }
}


fun updateLocale(context: Context, locale: Locale) {
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    saveSelectedLanguage(context, locale.language)
}

