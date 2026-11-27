package com.simohypers.limusic.ui.screen.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.simohypers.limusic.domain.data.entities.AlbumEntity
import com.simohypers.limusic.domain.data.entities.ArtistEntity
import com.simohypers.limusic.domain.data.entities.LocalPlaylistEntity
import com.simohypers.limusic.domain.data.entities.PlaylistEntity
import com.simohypers.limusic.domain.data.entities.PodcastsEntity
import com.simohypers.limusic.domain.data.model.searchResult.albums.AlbumsResult
import com.simohypers.limusic.domain.data.model.searchResult.artists.ArtistsResult
import com.simohypers.limusic.domain.data.model.searchResult.playlists.PlaylistsResult
import com.simohypers.limusic.domain.data.type.ArtistType
import com.simohypers.limusic.domain.data.type.PlaylistType
import com.simohypers.limusic.ui.component.ArtistFullWidthItems
import com.simohypers.limusic.ui.component.CenterLoadingBox
import com.simohypers.limusic.ui.component.Chip
import com.simohypers.limusic.ui.component.CollectionSearchRow
import com.simohypers.limusic.ui.component.LibrarySectionHeader
import com.simohypers.limusic.ui.component.PlaylistFullWidthItems
import com.simohypers.limusic.ui.component.SongFullWidthItems
import com.simohypers.limusic.ui.component.openLibraryItem
import com.simohypers.limusic.ui.icon.Album
import com.simohypers.limusic.ui.icon.GridView
import com.simohypers.limusic.ui.icon.LibraryMusic
import com.simohypers.limusic.ui.icon.LimusicIcons
import com.simohypers.limusic.ui.icon.PeopleAlt
import com.simohypers.limusic.ui.icon.QueueMusic
import com.simohypers.limusic.ui.navigation.destination.list.ArtistDestination
import com.simohypers.limusic.viewModel.LibraryViewModel
import limusic.composeapp.generated.resources.Res
import limusic.composeapp.generated.resources.albums
import limusic.composeapp.generated.resources.all
import limusic.composeapp.generated.resources.artists
import limusic.composeapp.generated.resources.playlists
import limusic.composeapp.generated.resources.search
import limusic.composeapp.generated.resources.songs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

enum class LibrarySearchFilter {
    ALL,
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
    SONGS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySearchScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(LibrarySearchFilter.ALL) }

    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val filteredPlaylists by viewModel.filteredPlaylists.collectAsStateWithLifecycle()
    val filteredAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val filteredArtists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val isLibraryLoading by viewModel.isLibraryLoading.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadAllLibraryData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        CollectionSearchRow(
            query = searchQuery,
            onQueryChange = { query ->
                searchQuery = query
                viewModel.setLibrarySearchQuery(query)
            },
            onClose = {
                viewModel.setLibrarySearchQuery("")
                navController.popBackStack()
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 15.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Chip(
                isAnimated = false,
                isSelected = selectedFilter == LibrarySearchFilter.ALL,
                text = stringResource(Res.string.all),
                icon = LimusicIcons.GridView,
            ) { selectedFilter = LibrarySearchFilter.ALL }

            Chip(
                isAnimated = false,
                isSelected = selectedFilter == LibrarySearchFilter.PLAYLISTS,
                text = stringResource(Res.string.playlists),
                icon = LimusicIcons.QueueMusic,
            ) { selectedFilter = LibrarySearchFilter.PLAYLISTS }

            Chip(
                isAnimated = false,
                isSelected = selectedFilter == LibrarySearchFilter.ALBUMS,
                text = stringResource(Res.string.albums),
                icon = LimusicIcons.Album,
            ) { selectedFilter = LibrarySearchFilter.ALBUMS }

            Chip(
                isAnimated = false,
                isSelected = selectedFilter == LibrarySearchFilter.ARTISTS,
                text = stringResource(Res.string.artists),
                icon = LimusicIcons.PeopleAlt,
            ) { selectedFilter = LibrarySearchFilter.ARTISTS }

            Chip(
                isAnimated = false,
                isSelected = selectedFilter == LibrarySearchFilter.SONGS,
                text = stringResource(Res.string.songs),
                icon = LimusicIcons.LibraryMusic,
            ) { selectedFilter = LibrarySearchFilter.SONGS }
        }

        if (searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.search),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val showPlaylists = selectedFilter == LibrarySearchFilter.ALL || selectedFilter == LibrarySearchFilter.PLAYLISTS
            val showAlbums = selectedFilter == LibrarySearchFilter.ALL || selectedFilter == LibrarySearchFilter.ALBUMS
            val showArtists = selectedFilter == LibrarySearchFilter.ALL || selectedFilter == LibrarySearchFilter.ARTISTS
            val showSongs = selectedFilter == LibrarySearchFilter.ALL || selectedFilter == LibrarySearchFilter.SONGS

            val hasResults = (showPlaylists && filteredPlaylists.isNotEmpty()) ||
                    (showAlbums && filteredAlbums.isNotEmpty()) ||
                    (showArtists && filteredArtists.isNotEmpty()) ||
                    (showSongs && filteredSongs.isNotEmpty())

            if (isLibraryLoading && !hasResults) {
                CenterLoadingBox(modifier = Modifier.fillMaxSize())
            } else if (!hasResults) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                ) {
                    if (showSongs && filteredSongs.isNotEmpty()) {
                        if (selectedFilter == LibrarySearchFilter.ALL) {
                            item {
                                LibrarySectionHeader(
                                    icon = LimusicIcons.LibraryMusic,
                                    title = stringResource(Res.string.songs),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    onMore = { selectedFilter = LibrarySearchFilter.SONGS },
                                )
                            }
                        }
                        items(filteredSongs, key = { "song_${it.videoId}" }) { track ->
                            SongFullWidthItems(
                                track = track,
                                isPlaying = track.videoId == nowPlaying,
                                modifier = Modifier,
                                onClickListener = { videoId ->
                                    viewModel.playSong(videoId)
                                },
                            )
                        }
                    }

                    if (showArtists && filteredArtists.isNotEmpty()) {
                        if (selectedFilter == LibrarySearchFilter.ALL) {
                            item {
                                LibrarySectionHeader(
                                    icon = LimusicIcons.PeopleAlt,
                                    title = stringResource(Res.string.artists),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    onMore = { selectedFilter = LibrarySearchFilter.ARTISTS },
                                )
                            }
                        }
                        items(filteredArtists, key = { "artist_${it.artistChannelId()}" }) { artist ->
                            ArtistFullWidthItems(
                                data = artist,
                                subtitle = (artist as? ArtistsResult)?.category?.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.artists),
                                onClickListener = {
                                    val id = artist.artistChannelId()
                                    if (id.isNotEmpty()) {
                                        navController.navigate(ArtistDestination(channelId = id))
                                    }
                                },
                            )
                        }
                    }

                    if (showPlaylists && filteredPlaylists.isNotEmpty()) {
                        if (selectedFilter == LibrarySearchFilter.ALL) {
                            item {
                                LibrarySectionHeader(
                                    icon = LimusicIcons.QueueMusic,
                                    title = stringResource(Res.string.playlists),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    onMore = { selectedFilter = LibrarySearchFilter.PLAYLISTS },
                                )
                            }
                        }
                        items(filteredPlaylists, key = { "playlist_${it.playlistBrowseId()}" }) { playlist ->
                            PlaylistFullWidthItems(
                                data = playlist,
                                onClickListener = { navController.openLibraryItem(playlist) },
                            )
                        }
                    }

                    if (showAlbums && filteredAlbums.isNotEmpty()) {
                        if (selectedFilter == LibrarySearchFilter.ALL) {
                            item {
                                LibrarySectionHeader(
                                    icon = LimusicIcons.Album,
                                    title = stringResource(Res.string.albums),
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                    onMore = { selectedFilter = LibrarySearchFilter.ALBUMS },
                                )
                            }
                        }
                        items(filteredAlbums, key = { "album_${it.playlistBrowseId()}" }) { album ->
                            PlaylistFullWidthItems(
                                data = album,
                                onClickListener = { navController.openLibraryItem(album) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ArtistType.artistChannelId(): String = when (this) {
    is ArtistEntity -> channelId
    is ArtistsResult -> browseId
    else -> ""
}

private fun PlaylistType.playlistBrowseId(): String = when (this) {
    is LocalPlaylistEntity -> id.toString()
    is PlaylistsResult -> browseId
    is AlbumEntity -> browseId
    is PlaylistEntity -> id
    is PodcastsEntity -> podcastId
    is AlbumsResult -> browseId
    else -> ""
}
