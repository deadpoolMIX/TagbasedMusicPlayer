package com.tagplayer.musicplayer.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.tagplayer.musicplayer.data.local.entity.SongTag
import com.tagplayer.musicplayer.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun getAllTags(): Flow<List<Tag>>

    // 按歌曲数量排序（从多到少）
    @Query("""
        SELECT t.* FROM tags t
        LEFT JOIN song_tags st ON t.id = st.tagId
        GROUP BY t.id
        ORDER BY COUNT(st.songId) DESC, t.name ASC
    """)
    fun getAllTagsOrderBySongCount(): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Query("SELECT COUNT(*) FROM tags")
    fun getTagCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM song_tags WHERE tagId = :tagId")
    fun getTagSongCount(tagId: Long): Flow<Int>

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<Tag>)

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    // Song-Tag relationship
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToTag(songTag: SongTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongsToTag(songTags: List<SongTag>)

    @Delete
    suspend fun removeSongFromTag(songTag: SongTag)

    @Query("DELETE FROM song_tags WHERE songId = :songId AND tagId = :tagId")
    suspend fun removeSongFromTagById(songId: Long, tagId: Long)

    @Query("DELETE FROM song_tags WHERE tagId = :tagId")
    suspend fun removeAllSongsFromTag(tagId: Long)

    @Query("DELETE FROM song_tags WHERE songId = :songId")
    suspend fun removeSongFromAllTags(songId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM song_tags WHERE songId = :songId AND tagId = :tagId)")
    suspend fun isSongTagged(songId: Long, tagId: Long): Boolean

    @Transaction
    @Query("SELECT t.* FROM tags t INNER JOIN song_tags st ON t.id = st.tagId WHERE st.songId = :songId ORDER BY t.name ASC")
    fun getTagsForSong(songId: Long): Flow<List<Tag>>

    @Transaction
    @Query("SELECT s.* FROM songs s INNER JOIN song_tags st ON s.id = st.songId WHERE st.tagId = :tagId ORDER BY s.title ASC")
    fun getSongsForTag(tagId: Long): Flow<List<com.tagplayer.musicplayer.data.local.entity.Song>>

    // Backup/Restore methods
    @Query("SELECT * FROM tags")
    suspend fun getAllTagsList(): List<Tag>

    @Query("SELECT * FROM song_tags")
    suspend fun getAllSongTagsList(): List<SongTag>

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Query("DELETE FROM song_tags")
    suspend fun deleteAllSongTags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongTag(songTag: SongTag)
}
