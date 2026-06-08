package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false
    private var useLocalFallback = true

    fun initialize(context: Context) {
        if (isInitialized) return

        val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (e: Exception) { "" }
        val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Exception) { "" }
        val appId = try { BuildConfig.FIREBASE_APP_ID } catch (e: Exception) { "" }

        val isConfigProvided = apiKey.isNotEmpty() && 
                !apiKey.contains("your_firebase_api_key") && 
                projectId.isNotEmpty() && 
                !projectId.contains("your_firebase_project_id") && 
                appId.isNotEmpty() && 
                !appId.contains("your_firebase_app_id")

        if (isConfigProvided) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                        .setProjectId(projectId)
                        .build()
                    FirebaseApp.initializeApp(context.applicationContext, options)
                }
                useLocalFallback = false
                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully with options!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
                useLocalFallback = true
            }
        } else {
            Log.d(TAG, "Firebase credentials not configured. Running in Local Demo Mode.")
            useLocalFallback = true
        }
        isInitialized = true
    }

    fun isFirebaseActive(): Boolean {
        return !useLocalFallback
    }

    fun getAuth(): FirebaseAuth? {
        return if (isFirebaseActive()) FirebaseAuth.getInstance() else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseActive()) FirebaseFirestore.getInstance() else null
    }
}
