package com.example.nothingchat.feature.auth.signout

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignOutViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
}
