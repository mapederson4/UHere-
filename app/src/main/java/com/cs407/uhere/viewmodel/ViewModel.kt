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
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                _userState.value = null
            } else {
                loadUser(firebaseUser.uid)
            }
        }
    }

    private fun loadUser(firebaseUid: String) {
        viewModelScope.launch {
            val user = userDao.getUserByFirebaseUid(firebaseUid)
            _userState.value = user
            android.util.Log.d(
                "UserViewModel",
                "Loaded user: id=${user?.id}, uid=${user?.firebaseUid}"
            )
        }
    }

    fun setUser(user: User) {
        viewModelScope.launch {
            // Check if user already exists
            val existingUser = userDao.getUserByFirebaseUid(user.firebaseUid)

            if (existingUser != null) {
                // User exists, use existing ID
                android.util.Log.d(
                    "UserViewModel",
                    "User exists: id=${existingUser.id}"
                )
                _userState.value = existingUser
            } else {
                // New user, insert
                val newUserId = userDao.insertUser(user)
                val insertedUser = user.copy(id = newUserId.toInt())
                android.util.Log.d(
                    "UserViewModel",
                    "New user created: id=${insertedUser.id}"
                )
                _userState.value = insertedUser
            }
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