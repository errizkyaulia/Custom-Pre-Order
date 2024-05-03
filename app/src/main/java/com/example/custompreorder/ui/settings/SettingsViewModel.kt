package com.example.custompreorder.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel : ViewModel() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _text = MutableLiveData<String>()

    val text: LiveData<String>
        get() = _text

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val user = auth.currentUser
        if (user != null) {
            _text.value = user.email
        } else {
            // Handle the case when current user is null, if needed
        }
    }
}