package com.flowtune.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtune.innertube.YouTube
import com.flowtune.innertube.models.PlaylistItem
import com.flowtune.innertube.models.WatchEndpoint
import com.flowtune.innertube.models.YTItem
import com.flowtune.innertube.models.filterExplicit
import com.flowtune.innertube.models.filterVideoSongs
import com.flowtune.innertube.pages.ExplorePage
import com.flowtune.innertube.pages.HomePage
import com.flowtune.innertube.utils.completed
import com.flowtune.music.constants.HideExplicitKey
import com.flowtune.music.constants.HideVideoSongsKey
import com.flowtune.music.constants.InnerTubeCookieKey
import com.flowtune.music.constants.QuickPicks
import com.flowtune.music.constants.QuickPicksKey
import com.flowtune.music.constants.YtmSyncKey
import com.flowtune.music.db.MusicDatabase
import com.flowtune.music.db.entities.Album
import com.flowtune.music.db.entities.LocalItem
import com.flowtune.music.db.entities.Song
import com.flowtune.music.extensions.filterVideoSongs
import com.flowtune.music.extensions.toEnum
import com.flowtune.music.models.SimilarRecommendation
import com.flowtune.music.utils.dataStore
import com.flowtune.music.utils.get
import com.flowtune.music.utils.reportException
import com.flowtune.music.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow


import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    private var lastProcessedCookie: String? = null
    
    private var isProcessingAccountData = false

    private suspend fun getQuickPicks() {
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> {
                val relatedSongs = database.quickPicks().first().filterVideoSongs(hideVideoSongs)
                val forgotten = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).take(8)
                
                val recentSong = database.events().first().firstOrNull()?.song
                val ytSimilarSongs = mutableListOf<Song>()
                
                if (recentSong != null) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        YouTube.related(endpoint).onSuccess { page ->
                            
                            page.songs.take(10).forEach { ytSong ->
                                database.song(ytSong.id).first()?.let { localSong ->
                                    if (!hideVideoSongs || !localSong.song.isVideo) {
                                        ytSimilarSongs.add(localSong)
                                    }
                                }
                            }
                        }
                    }
                }
                
                val combined = (relatedSongs + forgotten + ytSimilarSongs)
                    .distinctBy { it.id }
                    .shuffled()
                    .take(20)
                
                quickPicks.value = combined.ifEmpty { relatedSongs.shuffled().take(20) }
            }
            QuickPicks.LAST_LISTEN -> {
                val song = database.events().first().firstOrNull()?.song
                if (song != null && database.hasRelatedSongs(song.id)) {
                    quickPicks.value = database.getRelatedSongs(song.id).first().filterVideoSongs(hideVideoSongs).shuffled().take(20)
                }
            }
        }
    }

    private suspend fun load() {
        isLoading.value = true
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        getQuickPicks()
        forgottenFavorites.value = database.forgottenFavorites().first().filterVideoSongs(hideVideoSongs).shuffled().take(20)

        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5).first().filterVideoSongs(hideVideoSongs).shuffled().take(10)
        val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2).first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
        val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp).first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.shuffled().take(5)
        keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 15).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(4)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    
                    page.sections.takeLast(3).forEach { section ->
                        items += section.items
                    }
                }
                SimilarRecommendation(
                    title = it,
                    items = items
                        .distinctBy { item -> item.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .take(12)
                        .ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 15).first()
            .filter { it.album != null }
            .shuffled().take(3)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(10) +
                            page.albums.shuffled().take(5) +
                            page.artists.shuffled().take(3) +
                            page.playlists.shuffled().take(3))
                        .distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }
        
        val albumRecommendations = database.mostPlayedAlbums(fromTimeStamp, limit = 10).first()
            .filter { it.album.thumbnailUrl != null }
            .shuffled().take(2)
            .mapNotNull { album ->
                val items = mutableListOf<YTItem>()
                YouTube.album(album.id).onSuccess { page ->
                    
                    page.otherVersions.let { items += it }
                }
                
                album.artists.firstOrNull()?.id?.let { artistId ->
                    YouTube.artist(artistId).onSuccess { page ->
                        page.sections.lastOrNull()?.items?.let { items += it }
                    }
                }
                SimilarRecommendation(
                    title = album,
                    items = items
                        .distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .shuffled()
                        .take(10)
                        .ifEmpty { return@mapNotNull null }
                )
            }
        
        similarRecommendations.value = (artistRecommendations + songRecommendations + albumRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            homePage.value = page.copy(
                sections = page.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
        }.onFailure {
            reportException(it)
        }

        YouTube.explore().onSuccess { page ->
            explorePage.value = page.copy(
                newReleaseAlbums = page.newReleaseAlbums.filterExplicit(hideExplicit)
            )
        }.onFailure {
            reportException(it)
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
            .filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty()

        isLoading.value = false
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = (homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            val nextSections = YouTube.home(params = chip.endpoint?.params).getOrNull() ?: return@launch

            homePage.value = nextSections.copy(
                chips = homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideoSongs(hideVideoSongs))
                }
            )
            selectedChip.value = chip
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }
    }

    init {
        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .first()

            load()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            syncUtils.tryAutoSync()
        }

        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .collect { cookie ->
                    
                    if (isProcessingAccountData) return@collect
                    
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    
                    try {
                        if (cookie != null && cookie.isNotEmpty()) {
                            
                            YouTube.cookie = cookie
                            
                            YouTube.accountInfo().onSuccess { info ->
                                accountName.value = info.name
                                accountImageUrl.value = info.thumbnailUrl
                            }.onFailure {
                                reportException(it)
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            accountPlaylists.value = null
                        }
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}