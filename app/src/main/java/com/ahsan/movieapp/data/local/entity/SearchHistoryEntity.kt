package com.ahsan.movieapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per distinct search query the user has run; re-searching the same text just bumps searchedAt. */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long
)
