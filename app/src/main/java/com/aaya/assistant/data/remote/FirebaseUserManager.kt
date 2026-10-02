package com.aaya.assistant.data.remote

import android.content.Context
import com.aaya.assistant.data.local.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserAccountInfo(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val lastActiveDate: String = "",
    val dailyRequestCount: Int = 0,
    val totalRequests: Int = 0,
    val isBlocked: Boolean = false,
    val blockedReason: String = ""
)

class FirebaseUserManager(private val context: Context) {

    private val prefs = PreferenceManager(context)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var blockListener: ListenerRegistration? = null

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isUserBlocked(): Boolean = prefs.isUserBlocked

    fun initialize() {
        val user = currentUser
        if (user != null && !prefs.isGuestUser) {
            prefs.isLoggedIn = true
            prefs.userUid = user.uid
            prefs.userEmail = user.email ?: ""
            if (!user.displayName.isNullOrBlank()) {
                prefs.userName = user.displayName!!
            }
            if (user.photoUrl != null) {
                prefs.userPhotoUrl = user.photoUrl.toString()
            }
            listenToBlockStatus(user.uid)
        }
    }

    fun listenToBlockStatus(uid: String) {
        blockListener?.remove()
        try {
            blockListener = firestore.collection("users").document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val blocked = snapshot.getBoolean("isBlocked") ?: false
                    val reason = snapshot.getString("blockedReason") ?: ""
                    prefs.isUserBlocked = blocked
                    prefs.userBlockedReason = reason
                }
        } catch (_: Exception) {
            // Offline or Firebase not reachable
        }
    }

    suspend fun syncUserOnLogin(user: FirebaseUser): UserAccountInfo {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val uid = user.uid
        val email = user.email ?: ""
        val name = user.displayName ?: prefs.userName
        val photo = user.photoUrl?.toString() ?: ""

        prefs.isLoggedIn = true
        prefs.isGuestUser = false
        prefs.userUid = uid
        prefs.userEmail = email
        prefs.userName = name
        prefs.userPhotoUrl = photo

        val userDocRef = firestore.collection("users").document(uid)

        return try {
            val snapshot = userDocRef.get().await()
            val existing = snapshot.toObject(UserAccountInfo::class.java)

            val isBlocked = existing?.isBlocked ?: false
            val reason = existing?.blockedReason ?: ""
            prefs.isUserBlocked = isBlocked
            prefs.userBlockedReason = reason

            // Reset daily request count if new day
            val lastActiveDate = existing?.lastActiveDate ?: todayStr
            val isNewDay = lastActiveDate != todayStr
            val currentDailyCount = if (isNewDay) 0 else (existing?.dailyRequestCount ?: 0)

            val updatedData = mutableMapOf<String, Any>(
                "uid" to uid,
                "displayName" to name,
                "email" to email,
                "photoUrl" to photo,
                "lastActiveAt" to System.currentTimeMillis(),
                "lastActiveDate" to todayStr
            )

            if (existing == null) {
                updatedData["createdAt"] = System.currentTimeMillis()
                updatedData["dailyRequestCount"] = 0
                updatedData["totalRequests"] = 0
                updatedData["isBlocked"] = false
                updatedData["blockedReason"] = ""
            } else if (isNewDay) {
                updatedData["dailyRequestCount"] = 0
            }

            userDocRef.set(updatedData, SetOptions.merge()).await()
            listenToBlockStatus(uid)

            UserAccountInfo(
                uid = uid,
                displayName = name,
                email = email,
                photoUrl = photo,
                dailyRequestCount = currentDailyCount,
                isBlocked = isBlocked,
                blockedReason = reason
            )
        } catch (e: Exception) {
            // Local fallback if offline
            listenToBlockStatus(uid)
            UserAccountInfo(
                uid = uid,
                displayName = name,
                email = email,
                photoUrl = photo,
                isBlocked = prefs.isUserBlocked
            )
        }
    }

    fun continueAsGuest() {
        prefs.isLoggedIn = true
        prefs.isGuestUser = true
        prefs.isUserBlocked = false
        prefs.userEmail = "guest@aaya.offline"
    }

    fun signOut() {
        blockListener?.remove()
        try {
            auth.signOut()
        } catch (_: Exception) {}
        prefs.isLoggedIn = false
        prefs.isGuestUser = false
        prefs.userUid = ""
        prefs.userEmail = ""
        prefs.userPhotoUrl = ""
        prefs.isUserBlocked = false
    }

    suspend fun updateUserProfile(name: String, age: String, dob: String, location: String) {
        prefs.userName = name
        prefs.userAge = age
        prefs.userDob = dob
        prefs.userLocation = location
        val uid = prefs.userUid
        if (uid.isNotBlank() && !prefs.isGuestUser) {
            try {
                firestore.collection("users").document(uid).update(
                    mapOf(
                        "displayName" to name,
                        "age" to age,
                        "dob" to dob,
                        "location" to location
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    fun incrementApiRequest() {
        val uid = prefs.userUid
        if (uid.isBlank() || prefs.isGuestUser) return
        try {
            val userDocRef = firestore.collection("users").document(uid)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            userDocRef.update(
                mapOf(
                    "dailyRequestCount" to FieldValue.increment(1),
                    "totalRequests" to FieldValue.increment(1),
                    "lastActiveAt" to System.currentTimeMillis(),
                    "lastActiveDate" to todayStr
                )
            )
        } catch (_: Exception) {
            // Silently ignore offline error
        }
    }
}
