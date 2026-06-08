package com.example

import android.app.Application
import com.example.data.api.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.repository.MovieRepository
import com.example.data.repository.MovieRepositoryImpl
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.SubscriptionRepositoryImpl
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl

class CinemaApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(database.favoriteMovieDao())
    }

    val subscriptionRepository: SubscriptionRepository by lazy {
        SubscriptionRepositoryImpl(this)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(this, subscriptionRepository)
    }

    override fun onCreate() {
        super.onCreate()
        // Gracefully initialize Firebase dynamic connectivity
        FirebaseManager.initialize(this)
    }
}
