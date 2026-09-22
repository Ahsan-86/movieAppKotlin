package com.ahsan.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.dao.SearchHistoryDao
import com.ahsan.movieapp.data.local.dao.TvShowDao
import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.CategoryMovieCrossRef
import com.ahsan.movieapp.data.local.entity.CategoryRemoteKeys
import com.ahsan.movieapp.data.local.entity.CategoryTvShowCrossRef
import com.ahsan.movieapp.data.local.entity.FavoriteEntity
import com.ahsan.movieapp.data.local.entity.MovieDetailsEntity
import com.ahsan.movieapp.data.local.entity.MovieEntity
import com.ahsan.movieapp.data.local.entity.SearchHistoryEntity
import com.ahsan.movieapp.data.local.entity.TvRemoteKeys
import com.ahsan.movieapp.data.local.entity.TvShowEntity

@Database(
    entities = [
        MovieEntity::class,
        CategoryMovieCrossRef::class,
        FavoriteEntity::class,
        MovieDetailsEntity::class,
        CastMemberEntity::class,
        SearchHistoryEntity::class,
        CategoryRemoteKeys::class,
        TvShowEntity::class,
        CategoryTvShowCrossRef::class,
        TvRemoteKeys::class
    ],
    // Bumped 2 -> 3 for Phase 3's Information-section fields on MovieDetailsEntity
    // (originalTitle, status, homepage, budget, revenue, productionCountries,
    // productionCompaniesRaw). Bumped 3 -> 4 for Phase 3 Round B's collection-teaser fields
    // (collectionId, collectionName, collectionPosterPath). Bumped 4 -> 5 for Phase 4's new
    // category_remote_keys table (CategoryRemoteKeys — tracks the next TMDB page per paginated
    // category). Bumped 5 -> 6 for Phase 2.6 Session 3's TV cache tables (tv_shows, category_tv_shows,
    // tv_remote_keys — caches the Trending TV and Popular TV carousels like movies, see TvShowDao).
    // fallbackToDestructiveMigration() is set in DatabaseModule, so each bump resets local Favorites
    // once on first run after the update — same as the Phase 2 search_history addition did; flagged
    // to Ahsan each time.
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun tvShowDao(): TvShowDao

    companion object {
        const val DATABASE_NAME = "movie_app.db"
    }
}
