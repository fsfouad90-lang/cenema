package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Movie

@Entity(tableName = "favorite_movies")
data class FavoriteMovie(
    @PrimaryKey val id: Int,
    val title: String,
    val overview: String,
    val posterUrl: String,
    val backdropUrl: String,
    val rating: String,
    val releaseDate: String,
    val category: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMovie(): Movie {
        return Movie(
            id = id,
            title = title,
            overview = overview,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            rating = rating,
            releaseDate = releaseDate,
            category = category
        )
    }

    companion object {
        fun fromMovie(movie: Movie): FavoriteMovie {
            return FavoriteMovie(
                id = movie.id,
                title = movie.title,
                overview = movie.overview,
                posterUrl = movie.posterUrl,
                backdropUrl = movie.backdropUrl,
                rating = movie.rating,
                releaseDate = movie.releaseDate,
                category = movie.category
            )
        }
    }
}
