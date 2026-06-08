package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.FirebaseManager
import com.example.data.model.UserSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface AuthRepository {
    val currentUserSession: StateFlow<UserSession>
    suspend fun signUp(email: String, password: String): Result<UserSession.LoggedIn>
    suspend fun login(email: String, password: String): Result<UserSession.LoggedIn>
    suspend fun logout()
    suspend fun checkActiveSession()
}

class AuthRepositoryImpl(
    private val context: Context,
    private val subscriptionRepository: SubscriptionRepository
) : AuthRepository {
    private val TAG = "AuthRepository"
    private val prefs: SharedPreferences = context.getSharedPreferences("cinema_flow_auth_prefs", Context.MODE_PRIVATE)
    
    private val _currentUserSession = MutableStateFlow<UserSession>(UserSession.LoggedOut)
    override val currentUserSession: StateFlow<UserSession> = _currentUserSession.asStateFlow()

    // Real Firebase Auth listener block if active
    private var firebaseAuthListener: FirebaseAuth.AuthStateListener? = null

    init {
        CoroutineScope(Dispatchers.Main).launch {
            checkActiveSession()
        }
    }

    override suspend fun checkActiveSession() {
        if (FirebaseManager.isFirebaseActive()) {
            val auth = FirebaseManager.getAuth()
            if (auth != null) {
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val email = firebaseUser.email ?: "user@firebase.com"
                    val subStatus = subscriptionRepository.fetchSubscriptionStatus(uid)
                    _currentUserSession.value = UserSession.LoggedIn(uid, email, subStatus)
                    return
                }
            }
        }

        // Fallback local persistence check
        val savedUid = prefs.getString("active_uid", null)
        val savedEmail = prefs.getString("active_email", null)
        if (savedUid != null && savedEmail != null) {
            val subStatus = subscriptionRepository.fetchSubscriptionStatus(savedUid)
            _currentUserSession.value = UserSession.LoggedIn(savedUid, savedEmail, subStatus)
        } else {
            _currentUserSession.value = UserSession.LoggedOut
        }
    }

    override suspend fun signUp(email: String, password: String): Result<UserSession.LoggedIn> {
        return try {
            if (FirebaseManager.isFirebaseActive()) {
                val auth = FirebaseManager.getAuth() ?: throw IllegalStateException("Firebase Auth unavailable")
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Signup failed: User was null")
                val uid = user.uid
                val subStatus = subscriptionRepository.fetchSubscriptionStatus(uid)
                
                val loggedIn = UserSession.LoggedIn(uid, email, subStatus)
                _currentUserSession.value = loggedIn
                Result.success(loggedIn)
            } else {
                // Local Fallback Simulation
                // Save user credentials stub to preferences
                val cleanEmail = email.trim().lowercase()
                prefs.edit().putString("local_user_pw_$cleanEmail", password).apply()
                
                val uid = "local_${cleanEmail.hashCode()}"
                prefs.edit()
                    .putString("active_uid", uid)
                    .putString("active_email", cleanEmail)
                    .apply()
                
                val subStatus = subscriptionRepository.fetchSubscriptionStatus(uid)
                val loggedIn = UserSession.LoggedIn(uid, cleanEmail, subStatus)
                _currentUserSession.value = loggedIn
                Result.success(loggedIn)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signup failure: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserSession.LoggedIn> {
        return try {
            if (FirebaseManager.isFirebaseActive()) {
                val auth = FirebaseManager.getAuth() ?: throw IllegalStateException("Firebase Auth unavailable")
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw Exception("Login failed: User was null")
                val uid = user.uid
                val subStatus = subscriptionRepository.fetchSubscriptionStatus(uid)
                
                val loggedIn = UserSession.LoggedIn(uid, email, subStatus)
                _currentUserSession.value = loggedIn
                Result.success(loggedIn)
            } else {
                // Local Fallback Simulation
                val cleanEmail = email.trim().lowercase()
                val savedPw = prefs.getString("local_user_pw_$cleanEmail", null)
                if (savedPw == null) {
                    throw Exception("Account does not exist. Please signup.")
                }
                if (savedPw != password) {
                    throw Exception("Incorrect password. Please try again.")
                }
                
                val uid = "local_${cleanEmail.hashCode()}"
                prefs.edit()
                    .putString("active_uid", uid)
                    .putString("active_email", cleanEmail)
                    .apply()
                
                val subStatus = subscriptionRepository.fetchSubscriptionStatus(uid)
                val loggedIn = UserSession.LoggedIn(uid, cleanEmail, subStatus)
                _currentUserSession.value = loggedIn
                Result.success(loggedIn)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failure: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        if (FirebaseManager.isFirebaseActive()) {
            FirebaseManager.getAuth()?.signOut()
        }
        
        prefs.edit()
            .remove("active_uid")
            .remove("active_email")
            .apply()
            
        _currentUserSession.value = UserSession.LoggedOut
    }
}
