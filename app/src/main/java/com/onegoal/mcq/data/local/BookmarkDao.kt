package com.onegoal.mcq.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM bookmarks WHERE documentKey = :documentKey AND sectionIndex = :sectionIndex")
    fun observeForSection(documentKey: String, sectionIndex: Int): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentKey = :documentKey AND sectionIndex = :sectionIndex AND page = :page")
    suspend fun deletePage(documentKey: String, sectionIndex: Int, page: Int)
}
