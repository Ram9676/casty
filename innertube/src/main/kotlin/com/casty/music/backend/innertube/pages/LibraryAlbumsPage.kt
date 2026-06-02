package com.casty.music.backend.innertube.pages

import com.casty.music.backend.innertube.models.Album
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.Artist
import com.casty.music.backend.innertube.models.ArtistItem
import com.casty.music.backend.innertube.models.MusicResponsiveListItemRenderer
import com.casty.music.backend.innertube.models.MusicTwoRowItemRenderer
import com.casty.music.backend.innertube.models.PlaylistItem
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.innertube.models.YTItem
import com.casty.music.backend.innertube.models.oddElements
import com.casty.music.backend.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
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
