package com.casty.music.backend.innertube.pages

import com.casty.music.backend.innertube.models.Album
import com.casty.music.backend.innertube.models.Artist
import com.casty.music.backend.innertube.models.MusicResponsiveListItemRenderer
import com.casty.music.backend.innertube.models.MusicShelfRenderer
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.innertube.models.getItems
import com.casty.music.backend.innertube.models.oddElements
import com.casty.music.backend.innertube.models.splitBySeparator
import com.casty.music.backend.innertube.utils.parseTime

data class HistoryPage(
    val sections: List<HistorySection>?,
) {
    data class HistorySection(
        val title: String,
        val songs: List<SongItem>
    )

    companion object {
        fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): HistorySection? {
            val title = renderer.title?.runs?.firstOrNull()?.text ?: return null
            val songs = renderer.contents?.getItems()?.mapNotNull {
                fromMusicResponsiveListItemRenderer(it)
            }.orEmpty()
            if (songs.isEmpty()) return null
            return HistorySection(
                title = title,
                songs = songs
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            // Extract library tokens using the new method that properly handles multiple toggle items
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            // Split the secondary line by bullet separator to separate artists from other metadata (like views)
            val secondaryLineRuns = renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()

            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text ?: return null,
                artists = secondaryLineRuns?.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                } ?: emptyList(),
                album = renderer.flexColumns.getOrNull(3)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                    )
                },
                duration = renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer
                    ?.text?.runs?.firstOrNull()?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                    ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken,
                historyRemoveToken = renderer.menu?.menuRenderer?.items?.find {
                    it.menuServiceItemRenderer?.icon?.iconType == "REMOVE_FROM_HISTORY"
                }?.menuServiceItemRenderer?.serviceEndpoint?.feedbackEndpoint?.feedbackToken,
                isEpisode = renderer.isEpisode
            )
        }
    }
}
