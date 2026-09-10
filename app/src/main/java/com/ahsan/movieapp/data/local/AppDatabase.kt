package com.ahsan.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.dao.SearchHistoryDao
import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.CategoryMovieCrossRef
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieDetailsEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        MovieEntity::class,
        CategoryMovieCrossRef::class,
        FavoriteEntity::class,
        MovieDetailsEntity::class,
        CastMemberEntity::class,
        SearchHistoryEntity::class
    ],
    // Bumped 2 -> 3 for Phase 3's Information-section fields on MovieDetailsEntity
    // (originalTitle, status, homepage, budget, revenue, productionCountries,
    // productionCompaniesRaw). Bumped 3 -> 4 for Phase 3 Round B's collection-teaser fields
    // (collectionId, collectionName, collectionPosterPath). fallbackToDestructiveMigration() is
    // set in DatabaseModule, so each bump resets local Favorites once on first run after the
    // update — same as the Phase 2 search_history addition did; flagged to Ahsan each time.
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "movie_app.db"
    }
}
