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
     * Filter songs using boolean logic: (A AND B AND ...) OR (B AND ...) - C
     *
     * @param boxATags Tags that songs in box A must have (AND logic within box)
     * @param boxBTags Tags that songs in box B must have (AND logic within box)
     * @param boxCTags Tags that songs must NOT have
     * @return Filtered songs matching: (A-songs ∪ B-songs) - C-songs
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

        // Get songs for each tag in box A (must have ALL tags in box A)
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

        // Get songs for each tag in box B (must have ALL tags in box B)
        val boxBSongs = if (boxBTags.isEmpty()) {
            emptySet()
        } else {
            val firstTagSongs = tagDao.getSongsForTag(boxBTags[0]).first().map { it.id }.toSet()
            if (boxBTags.size == 1) {
                firstTagSongs
            } else {
                firstTagSongs.intersect(
                    boxBTags.drop(1).map { tagId ->
                        tagDao.getSongsForTag(tagId).first().map { it.id }.toSet()
                    }.reduce { acc, set -> acc.intersect(set) }
                )
            }
        }

        // Union of box A and box B
        val unionSongs = boxASongs.union(boxBSongs)

        // Get songs to exclude (have ANY tag in box C)
        val excludeSongs = if (boxCTags.isEmpty()) {
            emptySet()
        } else {
            boxCTags.flatMap { tagId ->
                tagDao.getSongsForTag(tagId).first().map { it.id }
            }.toSet()
        }

        // Final result: (A ∪ B) - C
        val resultIds = unionSongs.minus(excludeSongs)

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
