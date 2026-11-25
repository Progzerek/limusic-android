package com.simohypers.limusic.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.simohypers.limusic.domain.manager.DataStoreManager
import com.simohypers.limusic.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.simohypers.limusic.ui.navigation.destination.library.LikedSongsDestination
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistScreen
import com.simohypers.limusic.ui.screen.library.LibraryDynamicPlaylistType
import com.simohypers.limusic.ui.screen.other.PlaylistScreen
import com.simohypers.limusic.viewModel.SharedViewModel
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
fun NavGraphBuilder.libraryScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<LibraryDynamicPlaylistDestination> { entry ->
        val data = entry.toRoute<LibraryDynamicPlaylistDestination>()
        LibraryDynamicPlaylistScreen(
            innerPadding = innerPadding,
            navController = navController,
            type = data.type,
        )
    }
    composable<LikedSongsDestination> {
        val sharedViewModel: SharedViewModel = koinInject()
        val loggedInState by sharedViewModel.getYouTubeLoggedIn().collectAsStateWithLifecycle(initialValue = DataStoreManager.FALSE)
        val isLoggedIn = loggedInState == DataStoreManager.TRUE

        if (isLoggedIn) {
            PlaylistScreen(
                playlistId = "LM",
                isYourYouTubePlaylist = true,
                navController = navController,
                innerPadding = innerPadding,
            )
        } else {
            LibraryDynamicPlaylistScreen(
                innerPadding = innerPadding,
                navController = navController,
                type = LibraryDynamicPlaylistType.Favorite.toStringParams(),
            )
        }
    }
}