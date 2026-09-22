package com.ahsan.movieapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ahsan.movieapp.data.local.dao.FavoriteDao
import com.ahsan.movieapp.data.local.dao.MovieDao
import com.ahsan.movieapp.data.local.dao.SearchHistoryDao
import com.ahsan.movieapp.data.local.dao.TvShowDao
import com.ahsan.movieapp.data.local.entity.CastMemberEntity
import com.ahsan.movieapp.data.local.entity.CategoryMovieCrossRef
import com.ahsan.movieapp.data.local.entity.CategoryRemoteKeys
import com.ahsan.movieapp.data.local.entity.CategoryTvShowCrossRef
import com.ahsan.movieapp.data.local.entity.DiscoverComboMovieCrossRef
import com.ahsan.movieapp.data.local.entity.DiscoverComboRemoteKeys
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
        TvRemoteKeys::class,
        DiscoverComboMovieCrossRef::class,
        DiscoverComboRemoteKeys::class
    ],
    // Bumped 2 -> 3 for Phase 3's Information-section fields on MovieDetailsEntity
    // (originalTitle, status, homepage, budget, revenue, productionCountries,
    // productionCompaniesRaw). Bumped 3 -> 4 for Phase 3 Round B's collection-teaser fields
    // (collectionId, collectionName, collectionPosterPath). Bumped 4 -> 5 for Phase 4's new
    // category_remote_keys table (CategoryRemoteKeys — tracks the next TMDB page per paginated
    // category). Bumped 5 -> 6 for Phase 2.6 Session 3's TV cache tables (tv_shows, category_tv_shows,
    // tv_remote_keys — caches the Trending TV and Popular TV carousels like movies, see TvShowDao).
    // Bumped 6 -> 7 for Session 6's composite-key Favorites migration: `favorites` gains a
    // mediaType column and its id becomes composite (id, mediaType) so TV shows can be favorited
    // without colliding with same-numbered movies. Unlike every prior bump (which reset Favorites
    // via fallbackToDestructiveMigration), this one is a real Migration — MIGRATION_6_7 rebuilds
    // the favorites table and replays existing movie favorites into it, so users keep their saved
    // movies.
    // Bumped 7 -> 8 for Session 7's cached Discover combos: two new tables
    // (discover_combo_movies, discover_combo_remote_keys) cache the search/Genre screen's filtered
    // Discover results per applied Genres-Year-Language-Rating combination, with present-on-install
    // tables only.
    version = 8,
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

        // Session 6 — the project's first real Room Migration. SQLite can't ALTER a primary key,
        // so the favorites table is recreated: new composite-key shape `(id, mediaType)`, every
        // existing row replayed as a `'movie'` favorite (they all were, pre-Session 6). Must match
        // FavoriteEntity exactly or Room's schema validation fails at open. See the Database
        // annotation's bump comment for the full change history.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorites_new` (
                        `id` INTEGER NOT NULL,
                        `mediaType` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`, `mediaType`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `favorites_new` (`id`, `mediaType`, `addedAt`) SELECT `movieId`, 'movie', `addedAt` FROM `favorites`"
                )
                db.execSQL("DROP TABLE `favorites`")
                db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
            }
        }

        // Session 7 — pure table creations for the cached Discover combos, mirroring the themed
        // carousel caches. Strings to match the exported 8.json (Room/FTS tables would need the
        // `CREATE VIRTUAL TABLE` variants; these are plain b-tree tables).
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discover_combo_movies` (
                        `comboKey` TEXT NOT NULL,
                        `movieId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`comboKey`, `movieId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discover_combo_remote_keys` (
                        `comboKey` TEXT NOT NULL,
                        `nextPage` INTEGER,
                        `totalResults` INTEGER,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`comboKey`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
