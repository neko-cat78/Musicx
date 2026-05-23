package com.flowtune.innertube.models.body
import com.flowtune.innertube.models.Context
import kotlinx.serialization.Serializable
@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)