package com.onegoal.mcq.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentKey: String,
    val sectionIndex: Int,
    val page: Int,
    val title: String,
    val parentTitle: String?,
    val sectionLabel: String,
    /** The full ContentItem as JSON so the PDF can be reopened offline from the bookmark. */
    val itemJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)
