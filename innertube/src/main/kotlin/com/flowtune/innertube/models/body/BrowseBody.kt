package com.flowtune.innertube.models.body

import com.flowtune.innertube.models.Context
import com.flowtune.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)