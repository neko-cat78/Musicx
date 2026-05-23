package com.flowtune.music.models
import com.flowtune.innertube.models.YTItem
import com.flowtune.music.db.entities.LocalItem
data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)