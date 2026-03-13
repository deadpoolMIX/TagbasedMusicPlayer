package com.tagplayer.musicplayer.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tagplayer.musicplayer.ui.home.screen.HomeScreen
import com.tagplayer.musicplayer.ui.playlist.screen.PlaylistScreen
import com.tagplayer.musicplayer.ui.playlist.screen.PlaylistDetailScreen
import com.tagplayer.musicplayer.ui.playlist.screen.AddSongsToPlaylistScreen
import com.tagplayer.musicplayer.ui.tags.screen.TagsScreen
import com.tagplayer.musicplayer.ui.tags.screen.TagDetailScreen
import com.tagplayer.musicplayer.ui.filter.screen.FilterScreen
import com.tagplayer.musicplayer.ui.player.screen.PlayerScreen
import com.tagplayer.musicplayer.ui.player.screen.LyricsScreen
import com.tagplayer.musicplayer.ui.artist.screen.ArtistListScreen
import com.tagplayer.musicplayer.ui.artist.screen.ArtistDetailScreen
import com.tagplayer.musicplayer.ui.album.screen.AlbumListScreen
import com.tagplayer.musicplayer.ui.settings.screen.SettingsScreen

object Routes {
    const val HOME = "home"
    const val PLAYLIST = "playlist"
    const val PLAYLIST_DETAIL = "playlist_detail/{playlistId}"
    const val ADD_SONGS_TO_PLAYLIST = "add_songs_to_playlist/{playlistId}"
    const val TAGS = "tags"
    const val TAG_DETAIL = "tag_detail/{tagId}"
    const val FILTER = "filter"
    const val PLAYER = "player"
    const val ARTIST_LIST = "artist_list"
    const val ARTIST_DETAIL = "artist_detail/{artistName}"
    const val ALBUM_LIST = "album_list"
    const val ALBUM_DETAIL = "album_detail/{albumName}/{artistName}"
    const val SETTINGS = "settings"
    const val LYRICS = "lyrics"

    fun playlistDetail(playlistId: Long) = "playlist_detail/$playlistId"
    fun addSongsToPlaylist(playlistId: Long) = "add_songs_to_playlist/$playlistId"
    fun tagDetail(tagId: Long) = "tag_detail/$tagId"
    fun artistDetail(artistName: String) = "artist_detail/$artistName"
    fun albumDetail(albumName: String, artistName: String) = "album_detail/$albumName/$artistName"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToArtist = { navController.navigate(Routes.ARTIST_LIST) },
                onNavigateToAlbum = { navController.navigate(Routes.ALBUM_LIST) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PLAYLIST) {
            PlaylistScreen(
                onPlaylistClick = { playlist ->
                    navController.navigate(Routes.playlistDetail(playlist.id))
                }
            )
        }
        composable(
            route = Routes.PLAYLIST_DETAIL,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddSongsClick = {
                    navController.navigate(Routes.addSongsToPlaylist(playlistId))
                }
            )
        }
        composable(
            route = Routes.ADD_SONGS_TO_PLAYLIST,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            AddSongsToPlaylistScreen(
                playlistId = playlistId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.TAGS) {
            TagsScreen(
                onTagClick = { tag ->
                    navController.navigate(Routes.tagDetail(tag.id))
                }
            )
        }
        composable(
            route = Routes.TAG_DETAIL,
            arguments = listOf(
                navArgument("tagId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tagId = backStackEntry.arguments?.getLong("tagId") ?: return@composable
            TagDetailScreen(
                tagId = tagId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.FILTER) {
            FilterScreen()
        }
        composable(Routes.ARTIST_LIST) {
            ArtistListScreen(
                onBackClick = { navController.popBackStack() },
                onArtistClick = { artist ->
                    navController.navigate(Routes.artistDetail(artist.name))
                }
            )
        }
        composable(
            route = Routes.ARTIST_DETAIL,
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
            ArtistDetailScreen(
                artistName = artistName,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.ALBUM_LIST) {
            AlbumListScreen(
                onBackClick = { navController.popBackStack() },
                onAlbumClick = { album ->
                    // TODO: 导航到专辑详情页
                }
            )
        }
        composable(
            route = Routes.PLAYER,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            PlayerScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToLyrics = {
                    navController.navigate(Routes.LYRICS)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Routes.LYRICS) {
            LyricsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
