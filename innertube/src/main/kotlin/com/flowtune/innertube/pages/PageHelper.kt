package com.flowtune.innertube.pages

import com.flowtune.innertube.models.Menu
import com.flowtune.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.flowtune.innertube.models.Run

object PageHelper {
    
    private val LIBRARY_ADD_ICONS = setOf("LIBRARY_ADD", "BOOKMARK_BORDER")
    private val LIBRARY_SAVED_ICONS = setOf("LIBRARY_SAVED", "BOOKMARK")
    private val ALL_LIBRARY_ICONS = LIBRARY_ADD_ICONS + LIBRARY_SAVED_ICONS + setOf("LIBRARY_REMOVE")
    
    fun isLibraryIcon(iconType: String?): Boolean {
        return iconType != null && (iconType.startsWith("LIBRARY_") || iconType in ALL_LIBRARY_ICONS)
    }
    
    fun extractRuns(columns: List<FlexColumn>, typeLike: String): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                ?: continue

            for (run in runs) {
                val typeStr = run.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    ?: run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }

    fun extractFeedbackToken(menu: Menu.MenuRenderer.Item.ToggleMenuServiceRenderer?, type: String): String? {
        if (menu == null) return null
        val defaultToken = menu.defaultServiceEndpoint.feedbackEndpoint?.feedbackToken
        val toggledToken = menu.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken
        val iconType = menu.defaultIcon.iconType

        val isAddIcon = when (type) {
            "LIBRARY_ADD" -> iconType in LIBRARY_ADD_ICONS
            "LIBRARY_SAVED", "LIBRARY_REMOVE" -> iconType in LIBRARY_SAVED_ICONS
            else -> iconType == type
        }

        return if (isAddIcon) {
            defaultToken
        } else {
            toggledToken
        }
    }
}