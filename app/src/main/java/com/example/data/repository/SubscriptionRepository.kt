package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

interface SubscriptionRepository {
    val userStatus: StateFlow<String>
    suspend fun fetchSubscriptionStatus(userId: String): String
    suspend fun updateSubscriptionStatus(userId: String, status: String): Boolean
}

class SubscriptionRepositoryImpl(context: Context) : SubscriptionRepository {
    private val TAG = "SubscriptionRepo"
    private val prefs: SharedPreferences = context.getSharedPreferences("cinema_flow_prefs", Context.MODE_PRIVATE)
    
    private val _userStatus = MutableStateFlow(prefs.getString("fallback_sub_status", "Free") ?: "Free")
    override val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    override suspend fun fetchSubscriptionStatus(userId: String): String {
        if (FirebaseManager.isFirebaseActive()) {
            val db = FirebaseManager.getFirestore()
            if (db != null) {
                try {
                    val document = db.collection("users").document(userId).get().await()
                    if (document.exists()) {
                        val status = document.getString("subscriptionStatus") ?: "Free"
                        _userStatus.value = status
                        Log.d(TAG, "Fetched Firestore subscription: $status")
                        return status
                    } else {
                        // Create default user profile in Firestore
                        val data = mapOf("subscriptionStatus" to "Free")
                        db.collection("users").document(userId).set(data).await()
                        _userStatus.value = "Free"
                        return "Free"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error seeking Firestore subscription", e)
                }
            }
        }
        // Fallback local status
        val localStatus = prefs.getString("fallback_sub_status", "Free") ?: "Free"
        _userStatus.value = localStatus
        return localStatus
    }

    override suspend fun updateSubscriptionStatus(userId: String, status: String): Boolean {
        var success = false
        if (FirebaseManager.isFirebaseActive()) {
            val db = FirebaseManager.getFirestore()
            if (db != null) {
                try {
                    val data = mapOf("subscriptionStatus" to status)
                    db.collection("users").document(userId).set(data).await()
                    _userStatus.value = status
                    Log.d(TAG, "Updated Firestore subscription status: $status")
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing Firestore subscription status", e)
                }
            }
        }
        
        // Also always write to Local Preference fallback so both modes stay aligned and robust
        prefs.edit().putString("fallback_sub_status", status).apply()
        _userStatus.value = status
        success = true
        return success
    }
}
