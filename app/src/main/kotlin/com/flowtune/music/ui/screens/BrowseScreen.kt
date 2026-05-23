package com.flowtune.music.ui.screens
 
 import androidx.compose.foundation.ExperimentalFoundationApi
 import androidx.compose.foundation.combinedClickable
 import androidx.compose.foundation.layout.asPaddingValues
 import androidx.compose.foundation.lazy.grid.GridCells
 import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
 import androidx.compose.foundation.lazy.grid.items
 import androidx.compose.material3.ExperimentalMaterial3Api
 import androidx.compose.material3.Icon
 import androidx.compose.material3.Text
 import androidx.compose.material3.TopAppBar
 import androidx.compose.material3.TopAppBarScrollBehavior
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.collectAsState
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.rememberCoroutineScope
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.res.painterResource
 import androidx.compose.ui.res.stringResource
 import androidx.compose.ui.unit.dp
 import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
 import androidx.navigation.NavController
 import com.flowtune.music.LocalPlayerAwareWindowInsets
 import com.flowtune.music.LocalPlayerConnection
 import com.flowtune.music.R
 import com.flowtune.music.constants.GridItemSize
 import com.flowtune.music.constants.GridItemsSizeKey
 import com.flowtune.music.constants.GridThumbnailHeight
 import com.flowtune.music.utils.rememberEnumPreference
 import com.flowtune.music.ui.component.IconButton
 import com.flowtune.music.ui.component.LocalMenuState
 import com.flowtune.music.ui.component.YouTubeGridItem
 import com.flowtune.music.ui.component.shimmer.GridItemPlaceHolder
 import com.flowtune.music.ui.component.shimmer.ShimmerHost
 import com.flowtune.music.ui.menu.YouTubeAlbumMenu
 import com.flowtune.music.ui.menu.YouTubeArtistMenu
 import com.flowtune.music.ui.menu.YouTubePlaylistMenu
 import com.flowtune.music.ui.utils.backToMain
 import com.flowtune.music.viewmodels.BrowseViewModel
 import com.flowtune.innertube.models.AlbumItem
 import com.flowtune.innertube.models.ArtistItem
 import com.flowtune.innertube.models.PlaylistItem
 
 @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
 @Composable
 fun BrowseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    browseId: String?,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
     val menuState = LocalMenuState.current
     val playerConnection = LocalPlayerConnection.current ?: return
     val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
     val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
 
     val title by viewModel.title.collectAsState()
     val items by viewModel.items.collectAsState()
 
     val coroutineScope = rememberCoroutineScope()
     val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
 
     LazyVerticalGrid(
         columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
         contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
     ) {
         items?.let { items ->
             items(
                 items = items.distinctBy { it.id },
                 key = { it.id }
             ) { item ->
                 YouTubeGridItem(
                     item = item,
                     isPlaying = isPlaying,
                     fillMaxWidth = true,
                     coroutineScope = coroutineScope,
                     modifier = Modifier
                         .combinedClickable(
                             onClick = {
                                 when (item) {
                                     is AlbumItem -> navController.navigate("album/${item.id}")
                                     is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                     is ArtistItem -> navController.navigate("artist/${item.id}")
                                     else -> {
                                         
                                     }
                                 }
                             },
                             onLongClick = {
                                 menuState.show {
                                     when (item) {
                                         is AlbumItem ->
                                             YouTubeAlbumMenu(
                                                 albumItem = item,
                                                 navController = navController,
                                                 onDismiss = menuState::dismiss
                                             )
 
                                         is PlaylistItem -> {
                                             YouTubePlaylistMenu(
                                                 playlist = item,
                                                 coroutineScope = coroutineScope,
                                                 onDismiss = menuState::dismiss
                                             )
                                         }
 
                                         is ArtistItem -> {
                                             YouTubeArtistMenu(
                                                 artist = item,
                                                 onDismiss = menuState::dismiss
                                             )
                                         }
 
                                         else -> {
                                             
                                         }
                                     }
                                 }
                             }
                         )
                 )
             }
 
             if (items.isEmpty()) {
                 items(8) {
                     ShimmerHost {
                         GridItemPlaceHolder(fillMaxWidth = true)
                     }
                 }
             }
         }
     }
 
     TopAppBar(
         title = { Text(title ?: "") },
         navigationIcon = {
             IconButton(
                 onClick = navController::navigateUp,
                 onLongClick = navController::backToMain
             ) {
                 Icon(
                     painterResource(R.drawable.arrow_back),
                     contentDescription = null
                 )
             }
         }
     )
 }