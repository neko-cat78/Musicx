package com.flowtune.music.viewmodels
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtune.music.constants.HideExplicitKey
import com.flowtune.music.constants.HideVideoSongsKey
import com.flowtune.music.db.MusicDatabase
import com.flowtune.music.db.entities.Song
import com.flowtune.music.extensions.filterExplicit
import com.flowtune.music.extensions.filterVideoSongs
import com.flowtune.music.utils.dataStore
import com.flowtune.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.flowtune.music.di.PlayerCache
import com.flowtune.music.di.DownloadCache
import androidx.media3.datasource.cache.SimpleCache
import java.time.LocalDateTime
@HiltViewModel
class CachePlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    @PlayerCache private val playerCache: SimpleCache,
    @DownloadCache private val downloadCache: SimpleCache
) : ViewModel() {
    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs
    init {
        viewModelScope.launch {
            while (true) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val cachedIds = playerCache.keys.toSet()
                val downloadedIds = downloadCache.keys.toSet()
                val pureCacheIds = cachedIds.subtract(downloadedIds)
                val songs = if (pureCacheIds.isNotEmpty()) {
                    database.getSongsByIds(pureCacheIds.toList())
                } else {
                    emptyList()
                }
                val completeSongs = songs.filter {
                    val contentLength = it.format?.contentLength
                    contentLength != null && playerCache.isCached(it.song.id, 0, contentLength)
                }
                if (completeSongs.isNotEmpty()) {
                    database.query {
                        completeSongs.forEach {
                            if (it.song.dateDownload == null) {
                                update(it.song.copy(dateDownload = LocalDateTime.now()))
                            }
                        }
                    }
                }
                _cachedSongs.value = completeSongs
                    .filter { it.song.dateDownload != null }
                    .sortedByDescending { it.song.dateDownload }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                delay(1000)
            }
        }
    }
    fun removeSongFromCache(songId: String) {
        playerCache.removeResource(songId)
    }
}