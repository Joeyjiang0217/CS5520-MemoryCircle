package com.cs5520group15.memorycircle.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Central entry point for Firebase SDK instances. All repositories pull
 * Auth / Firestore / Storage handles from here so we never call
 * FirebaseXxx.getInstance() scattered around the project.
 */
object FirebaseModule {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()
}
