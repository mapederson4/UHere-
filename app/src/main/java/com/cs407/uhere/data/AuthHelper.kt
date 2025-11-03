package com.cs407.uhere.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

fun signIn(
    email: String,
    password: String,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    auth.signInWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            result.user?.let { onSuccess(it) }
        }
        .addOnFailureListener { exception ->
            // If sign-in fails, try to create account
            createAccount(email, password, onSuccess, onError)
        }
}

fun createAccount(
    email: String,
    password: String,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { result ->
            result.user?.let { onSuccess(it) }
        }
        .addOnFailureListener { exception ->
            onError(exception.message ?: "Account creation failed")
        }
}

fun updateDisplayName(
    name: String,
    onComplete: (Boolean, Exception?) -> Unit
) {
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