package com.flowtune.innertube.pages
import com.flowtune.innertube.models.YTItem
data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)