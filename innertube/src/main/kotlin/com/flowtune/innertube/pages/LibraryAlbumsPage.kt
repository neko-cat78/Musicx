package com.flowtune.innertube.pages

import com.flowtune.innertube.models.Album
import com.flowtune.innertube.models.AlbumItem
import com.flowtune.innertube.models.Artist
import com.flowtune.innertube.models.ArtistItem
import com.flowtune.innertube.models.MusicResponsiveListItemRenderer
import com.flowtune.innertube.models.MusicTwoRowItemRenderer
import com.flowtune.innertube.models.PlaylistItem
import com.flowtune.innertube.models.SongItem
import com.flowtune.innertube.models.YTItem
import com.flowtune.innertube.models.oddElements
import com.flowtune.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null
            
            val playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                ?.watchPlaylistEndpoint?.playlistId
                
                ?: renderer.menu?.menuRenderer?.items?.firstOrNull()
                    ?.menuNavigationItemRenderer?.navigationEndpoint
                    ?.watchPlaylistEndpoint?.playlistId
                
                ?: browseId.removePrefix("MPREb_").let { "OLAK5uy_$it" }
            
            return AlbumItem(
                browseId = browseId,
                playlistId = playlistId,
                title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                artists = null,
                year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }
    }
}