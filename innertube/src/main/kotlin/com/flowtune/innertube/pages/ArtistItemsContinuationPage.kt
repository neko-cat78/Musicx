package com.flowtune.innertube.pages
import com.flowtune.innertube.models.YTItem
data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)