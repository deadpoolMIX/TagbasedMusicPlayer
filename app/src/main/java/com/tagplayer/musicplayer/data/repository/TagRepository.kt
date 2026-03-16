package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.TagDao
import com.tagplayer.musicplayer.data.local.entity.Song
import com.tagplayer.musicplayer.data.local.entity.SongTag
import com.tagplayer.musicplayer.data.local.entity.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getAllTags(): Flow<List<Tag>> = tagDao.getAllTags()

    // 按歌曲数量排序（从多到少）
    fun getAllTagsOrderBySongCount(): Flow<List<Tag>> = tagDao.getAllTagsOrderBySongCount()

    suspend fun getTagById(tagId: Long): Tag? = tagDao.getTagById(tagId)

    suspend fun getTagByName(name: String): Tag? = tagDao.getTagByName(name)

    fun getTagCount(): Flow<Int> = tagDao.getTagCount()

    fun getTagSongCount(tagId: Long): Flow<Int> = tagDao.getTagSongCount(tagId)

    fun searchTags(query: String): Flow<List<Tag>> = tagDao.searchTags(query)

    suspend fun createTag(name: String): Long {
        val now = System.currentTimeMillis()
        val tag = Tag(
            name = name,
            createdAt = now,
            updatedAt = now
        )
        return tagDao.insertTag(tag)
    }

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun deleteTagById(tagId: Long) = tagDao.deleteTagById(tagId)

    // Song-Tag operations
    suspend fun addTagToSong(songId: Long, tagId: Long) {
        val songTag = SongTag(
            songId = songId,
            tagId = tagId,
            addedAt = System.currentTimeMillis()
        )
        tagDao.addSongToTag(songTag)
    }

    suspend fun removeTagFromSong(songId: Long, tagId: Long) {
        tagDao.removeSongFromTagById(songId, tagId)
    }

    suspend fun removeAllTagsFromSong(songId: Long) {
        tagDao.removeSongFromAllTags(songId)
    }

    fun getTagsForSong(songId: Long): Flow<List<Tag>> = tagDao.getTagsForSong(songId)

    fun getSongsForTag(tagId: Long): Flow<List<Song>> = tagDao.getSongsForTag(tagId)

    suspend fun isSongTagged(songId: Long, tagId: Long): Boolean =
        tagDao.isSongTagged(songId, tagId)
}
