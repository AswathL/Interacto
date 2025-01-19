package com.example.nothingchat.feature.auth.signup

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor() :ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        _state.value = SignUpState.Loading
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                if (it.user != null){
                    it.user!!.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())
                _state.value = SignUpState.Success
                    return@addOnSuccessListener
            }
                _state.value = SignUpState.Error
            }
            .addOnFailureListener {
                _state.value = SignUpState.Error
            }
    }

}

sealed class SignUpState {
    object Loading : SignUpState()
    object Success : SignUpState()
    object Error : SignUpState()
    object Nothing : SignUpState()
}