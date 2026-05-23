package com.flowtune.innertube.pages
import com.flowtune.innertube.models.AlbumItem
data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)