package com.flowtune.music.viewmodels
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowtune.innertube.YouTube
import com.flowtune.innertube.models.filterVideoSongs
import com.flowtune.innertube.pages.BrowseResult
import com.flowtune.music.constants.HideExplicitKey
import com.flowtune.music.constants.HideVideoSongsKey
import com.flowtune.music.utils.dataStore
import com.flowtune.music.utils.get
import com.flowtune.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class YouTubeBrowseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")
    val result = MutableStateFlow<BrowseResult?>(null)
    init {
        viewModelScope.launch {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
            YouTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                }.onFailure {
                    reportException(it)
                }
        }
    }
}