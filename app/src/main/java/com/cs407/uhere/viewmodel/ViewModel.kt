package com.cs407.uhere.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.uhere.data.UHereDatabase
import com.cs407.uhere.data.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = UHereDatabase.getDatabase(application).userDao()
    private val auth = FirebaseAuth.getInstance()

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _userState.value = null
            } else {
                // Load user from database
                loadUser(firebaseUser.uid)
            }
        }
    }

    private fun loadUser(firebaseUid: String) {
        viewModelScope.launch {
            val user = userDao.getUserByFirebaseUid(firebaseUid)
            _userState.value = user
        }
    }

    fun setUser(user: User) {
        viewModelScope.launch {
            userDao.insertUser(user)
            _userState.value = user
        }
    }

    fun clearUser() {
        _userState.value = null
    }

    fun logout() {
        auth.signOut()
        clearUser()
    }
}