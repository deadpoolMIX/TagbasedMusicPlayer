package com.tagplayer.musicplayer.data.repository

import com.tagplayer.musicplayer.data.local.database.SongDao
import com.tagplayer.musicplayer.data.local.database.TagDao
import com.tagplayer.musicplayer.data.local.entity.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterRepository @Inject constructor(
    private val songDao: SongDao,
    private val tagDao: TagDao
) {
    /**
     * Filter songs using boolean logic: (A AND B AND ...) OR (B1 OR B2 OR ...) - C
     *
     * Special case: When both A and B are empty but C is not empty,
     * the result is "all songs minus songs with C tags"
     *
     * @param boxATags Tags that songs in box A must have (AND logic within box)
     * @param boxBTags Tags that songs in box B must have (OR logic within box)
     * @param boxCTags Tags that songs must NOT have
     * @return Filtered songs matching: (A-songs ∪ B-songs) - C-songs, or (all-songs - C-songs) when A and B are empty
     */
    fun filterSongs(
        boxATags: List<Long> = emptyList(),
        boxBTags: List<Long> = emptyList(),
        boxCTags: List<Long> = emptyList()
    ): Flow<List<Song>> = flow {
        // If all boxes are empty, return all songs
        if (boxATags.isEmpty() && boxBTags.isEmpty() && boxCTags.isEmpty()) {
            emit(songDao.getAllSongs().first())
            return@flow
        }

        // Get songs for each tag in box A (must have ALL tags in box A - AND logic)
        val boxASongs = if (boxATags.isEmpty()) {
            emptySet()
        } else {
            val firstTagSongs = tagDao.getSongsForTag(boxATags[0]).first().map { it.id }.toSet()
            if (boxATags.size == 1) {
                firstTagSongs
            } else {
                firstTagSongs.intersect(
                    boxATags.drop(1).map { tagId ->
                        tagDao.getSongsForTag(tagId).first().map { it.id }.toSet()
                    }.reduce { acc, set -> acc.intersect(set) }
                )
            }
        }

        // Get songs for each tag in box B (must have ANY tag in box B - OR logic)
        val boxBSongs = if (boxBTags.isEmpty()) {
            emptySet()
        } else {
            boxBTags.flatMap { tagId ->
                tagDao.getSongsForTag(tagId).first().map { it.id }
            }.toSet()
        }

        // Get songs to exclude (have ANY tag in box C)
        val excludeSongs = if (boxCTags.isEmpty()) {
            emptySet()
        } else {
            boxCTags.flatMap { tagId ->
                tagDao.getSongsForTag(tagId).first().map { it.id }
            }.toSet()
        }

        // Calculate result based on the logic
        val resultIds = when {
            // A and B are both empty, but C is not empty: all songs - C
            boxATags.isEmpty() && boxBTags.isEmpty() && boxCTags.isNotEmpty() -> {
                songDao.getAllSongs().first().map { it.id }.toSet().minus(excludeSongs)
            }
            // Normal case: (A ∪ B) - C
            else -> {
                val unionSongs = boxASongs.union(boxBSongs)
                unionSongs.minus(excludeSongs)
            }
        }

        // Fetch full song objects
        if (resultIds.isEmpty()) {
            emit(emptyList())
        } else {
            emit(songDao.getSongsByIds(resultIds.toList()).first())
        }
    }

    /**
     * Get count of filtered songs without fetching all songs
     */
    suspend fun getFilteredSongCount(
        boxATags: List<Long> = emptyList(),
        boxBTags: List<Long> = emptyList(),
        boxCTags: List<Long> = emptyList()
    ): Int {
        return filterSongs(boxATags, boxBTags, boxCTags).first().size
    }
}
