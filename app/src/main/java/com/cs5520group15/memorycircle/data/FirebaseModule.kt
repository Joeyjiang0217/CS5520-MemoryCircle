/**
 * What: Central provider of shared Firebase SDK singletons (Auth, Firestore,
 *       Storage) so the rest of the app never calls FirebaseXxx.getInstance()
 *       directly.
 * Who: Used by every repository in this package (AuthRepository,
 *       FriendsRepository, GroupRepository, ProfileRepository,
 *       ScrapbookRepository, NotificationsRepository, SeedRepository) and by
 *       ViewModels that touch Firestore directly (HomeViewModel,
 *       ScrapbookViewModel, GroupDetailViewModel, etc.).
 * When: Resolved lazily on first property access — each get() returns the live
 *       Firebase instance whenever a consumer reads auth / db / storage.
 */

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
