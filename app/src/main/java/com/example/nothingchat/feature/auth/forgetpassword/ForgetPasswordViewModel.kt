package com.example.nothingchat.feature.auth.forgetpassword

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ForgetPasswordViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<ForgetPasswordState>(ForgetPasswordState.Nothing)
    val state = _state.asStateFlow()

    fun resetPassword(email: String) {
        _state.value = ForgetPasswordState.Loading
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _state.value = ForgetPasswordState.Success
            }
            .addOnFailureListener {
                _state.value = ForgetPasswordState.Error
            }
    }
}

sealed class ForgetPasswordState {
    object Loading : ForgetPasswordState()
    object Success : ForgetPasswordState()
    object Error : ForgetPasswordState()
    object Nothing : ForgetPasswordState()
}
