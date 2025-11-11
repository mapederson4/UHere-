package com.cs407.uhere.auth

import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

fun signIn(
    email: String,
    password: String,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    // Validate inputs
    if (email.isBlank()) {
        onError("Email cannot be empty")
        return
    }
    if (password.isBlank()) {
        onError("Password cannot be empty")
        return
    }

    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            result.user?.let { onSuccess(it) }
                ?: onError("Failed to retrieve user")
        }
        .addOnFailureListener { exception ->
            onError(getAuthErrorMessage(exception))
        }
}

fun createAccount(
    email: String,
    password: String,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    // Validate inputs
    if (email.isBlank()) {
        onError("Email cannot be empty")
        return
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        onError("Invalid email format")
        return
    }
    if (password.length < 6) {
        onError("Password must be at least 6 characters")
        return
    }

    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            result.user?.let { onSuccess(it) }
                ?: onError("Failed to create user")
        }
        .addOnFailureListener { exception ->
            onError(getAuthErrorMessage(exception))
        }
}

fun updateDisplayName(
    name: String,
    onComplete: (Boolean, Exception?) -> Unit
) {
    if (name.isBlank()) {
        onComplete(false, Exception("Name cannot be empty"))
        return
    }

    val user = FirebaseAuth.getInstance().currentUser
    if (user == null) {
        onComplete(false, Exception("No user logged in"))
        return
    }

    val profileUpdates = userProfileChangeRequest {
        displayName = name
    }

    user.updateProfile(profileUpdates)
        .addOnCompleteListener { task ->
            onComplete(task.isSuccessful, task.exception)
        }
}

private fun getAuthErrorMessage(exception: Exception): String {
    return when (exception) {
        is FirebaseAuthInvalidCredentialsException ->
            "Invalid email or password"
        is FirebaseAuthUserCollisionException ->
            "An account with this email already exists"
        is FirebaseAuthInvalidUserException ->
            "No account found with this email"
        is FirebaseAuthWeakPasswordException ->
            "Password is too weak. Use at least 6 characters"
        else -> exception.message ?: "Authentication failed"
    }
}