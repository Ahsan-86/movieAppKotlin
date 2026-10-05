package com.ahsan.movieapp.di

import android.content.Context
import androidx.room.Room
import com.ahsan.movieapp.BuildConfig
import com.ahsan.movieapp.data.local.AppDatabase
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.dao.SearchHistoryDao
import com.ahsan.movieapp.data.local.dao.TvShowDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // Session 6 — the project's first explicit Room migration (composite-key Favorites),
            // wired before the destructive fallback so existing movie favorites survive the bump.
            // Session 7 — pure table creations for the cached Discover combos.
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
            .apply {
                // Debug builds may still wipe the DB when a schema bump has no Migration, so
                // development isn't blocked. Release builds must never do that — it would silently
                // delete users' Favorites. The Play Store baseline is schema v8; every future version
                // bump MUST ship a Migration (and its exported schema JSON) before release.
                if (BuildConfig.DEBUG) fallbackToDestructiveMigration()
            }
            .build()

    @Provides
    fun provideMovieDao(database: AppDatabase): MovieDao = database.movieDao()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun provideTvShowDao(database: AppDatabase): TvShowDao = database.tvShowDao()
}
