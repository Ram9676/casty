package com.casty.music.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.casty.music.backend.constants.PreferredArtistTagsKey
import com.casty.music.backend.constants.PreferredIndustryTagsKey
import com.casty.music.backend.constants.PreferredLanguageTagsKey
import com.casty.music.backend.constants.TasteOnboardingCompletedKey
import com.casty.music.backend.innertube.YouTube
import com.casty.music.backend.innertube.models.AlbumItem
import com.casty.music.backend.innertube.models.ArtistItem
import com.casty.music.backend.innertube.models.EpisodeItem
import com.casty.music.backend.innertube.models.PlaylistItem
import com.casty.music.backend.innertube.models.SongItem
import com.casty.music.backend.innertube.models.YTItem
import com.casty.music.backend.utils.dataStore
import com.casty.music.data.db.CastyDatabase
import com.casty.music.data.db.entities.AlbumEntity
import com.casty.music.data.db.entities.ArtistEntity
import com.casty.music.data.db.entities.PlaylistEntity
import com.casty.music.data.db.entities.PlaylistSongCrossRef
import com.casty.music.data.db.entities.RecentSearchEntity
import com.casty.music.data.db.entities.SongEntity as DbSongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CastyDatabase,
) {
    // DAOs - the new persistent backbone (best-ever: everything durable)
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val recentSearchDao = database.recentSearchDao()

    private val repositoryScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    private fun <T> Flow<T>.recoverRoomRead(source: String, fallback: T): Flow<T> =
        catch { throwable ->
            Timber.e(throwable, "Casty Room read failed for $source; using fallback")
            emit(fallback)
        }

    // Note: We keep almost zero in-memory state now.
    // Everything important flows from Room for crash-safety and persistence.

    fun getRecentTracks(): Flow<List<SongEntity>> =
        songDao.observeRecentSongs(limit = MAX_RECENT_ITEMS).map { songs ->
            songs.map { SongEntity(it.toSong()) }
        }.recoverRoomRead("recent tracks", emptyList())

    fun getRecentPlaylists(): Flow<List<PlaylistPreview>> =
        // For v1 we derive from persisted playlists (real recent playlist tracking can be added later)
        playlistDao.observeAllPlaylists().map { list ->
            list.take(MAX_RECENT_ITEMS).map { it.toPreview() }
        }.recoverRoomRead("recent playlists", emptyList())

    fun getLikedSongs(): Flow<List<Song>> =
        songDao.observeLikedSongs().map { songs -> songs.map { it.toSong() } }
            .recoverRoomRead("liked songs", emptyList())

    fun getLibrarySongs(): Flow<List<Song>> =
        songDao.observeLibrarySongs().map { songs -> songs.map { it.toSong() } }
            .recoverRoomRead("library songs", emptyList())

    fun getDownloadedSongs(): Flow<List<Song>> =
        songDao.observeDownloadedSongs().map { songs -> songs.map { it.toSong() } }
            .recoverRoomRead("downloaded songs", emptyList())

    fun getWatchLaterSongs(): Flow<List<Song>> =
        songDao.observeWatchLaterSongs().map { songs -> songs.map { it.toSong() } }
            .recoverRoomRead("watch later songs", emptyList())

    fun getPlaylists(
        sortBy: PlaylistSortBy = PlaylistSortBy.Name,
        sortOrder: SortOrder = SortOrder.Ascending,
    ): Flow<List<PlaylistPreview>> =
        playlistDao.observeAllPlaylists().map { entities ->
            val previews = entities.map { it.toPreview() }
            when (sortBy) {
                PlaylistSortBy.Name -> {
                    val sorted = previews.sortedBy { it.playlist.name.lowercase() }
                    if (sortOrder == SortOrder.Descending) sorted.reversed() else sorted
                }
            }
        }.recoverRoomRead("playlists", emptyList())

    // Albums / Artists - Now fully persisted in Room (v5+)
    fun getAlbums(
        sortBy: AlbumSortBy = AlbumSortBy.Title,
        sortOrder: SortOrder = SortOrder.Ascending,
    ): Flow<List<Album>> =
        when (sortBy) {
            AlbumSortBy.Title -> {
                val flow = if (sortOrder == SortOrder.Ascending) {
                    database.albumDao().observeVisibleAlbums()
                } else {
                    // For descending, we need to sort in memory since Room doesn't support dynamic ORDER BY
                    kotlinx.coroutines.flow.flow {
                        emit(database.albumDao().observeVisibleAlbums().first().sortedByDescending { it.title })
                    }
                }
                flow.map { entities -> entities.map { it.toAlbum() } }
            }
        }.recoverRoomRead("albums", emptyList())

    fun getArtists(
        sortBy: ArtistSortBy = ArtistSortBy.Name,
        sortOrder: SortOrder = SortOrder.Ascending,
    ): Flow<List<Artist>> =
        when (sortBy) {
            ArtistSortBy.Name -> {
                val flow = if (sortOrder == SortOrder.Ascending) {
                    database.artistDao().observeVisibleArtists()
                } else {
                    kotlinx.coroutines.flow.flow {
                        emit(database.artistDao().observeVisibleArtists().first().sortedByDescending { it.name })
                    }
                }
                flow.map { entities -> entities.map { it.toArtist() } }
            }
        }.recoverRoomRead("artists", emptyList())
    
    // Advanced album/artist queries for enhanced features
    fun getFavoriteAlbums(): Flow<List<Album>> =
        database.albumDao().observeFavoriteAlbums()
            .map { entities -> entities.map { it.toAlbum() } }
            .recoverRoomRead("favorite albums", emptyList())
    
    fun getRecentlyPlayedAlbums(limit: Int = 20): Flow<List<Album>> =
        database.albumDao().observeRecentlyPlayed(limit)
            .map { entities -> entities.map { it.toAlbum() } }
            .recoverRoomRead("recent albums", emptyList())
    
    fun getTrendingAlbums(limit: Int = 20): Flow<List<Album>> =
        database.albumDao().observeTrendingAlbums(limit)
            .map { entities -> entities.map { it.toAlbum() } }
            .recoverRoomRead("trending albums", emptyList())
    
    fun getFavoriteArtists(): Flow<List<Artist>> =
        database.artistDao().observeFavoriteArtists()
            .map { entities -> entities.map { it.toArtist() } }
            .recoverRoomRead("favorite artists", emptyList())
    
    fun getFollowedArtists(): Flow<List<Artist>> =
        database.artistDao().observeFollowedArtists()
            .map { entities -> entities.map { it.toArtist() } }
            .recoverRoomRead("followed artists", emptyList())
    
    fun getTopArtists(limit: Int = 20): Flow<List<Artist>> =
        database.artistDao().observeTopArtists(limit)
            .map { entities -> entities.map { it.toArtist() } }
            .recoverRoomRead("top artists", emptyList())

    fun getSearchQueries(): Flow<List<SearchQuery>> =
        recentSearchDao.observeRecent(limit = 20).map { list ->
            list.map { SearchQuery(it.query) }
        }.recoverRoomRead("search queries", emptyList())

    fun recordPlayback(song: Song) {
        repositoryScope.launch {
            upsertSongPreservingState(song) {
                copy(
                    lastPlayedAt = LocalDateTime.now(),
                    playCount = playCount + 1,
                )
            }
        }
    }

    fun recordPlaylistPlayback(preview: PlaylistPreview) {
        repositoryScope.launch {
            playlistDao.upsertPlaylist(preview.playlist.toEntity())
        }
    }

    fun observeTasteProfile(): Flow<TasteProfile> =
        context.dataStore.data.map { preferences ->
            TasteProfile(
                selectedLanguages = preferences[PreferredLanguageTagsKey].orEmpty(),
                selectedIndustries = preferences[PreferredIndustryTagsKey].orEmpty(),
                favoriteArtists = preferences[PreferredArtistTagsKey].orEmpty(),
                isCompleted = preferences[TasteOnboardingCompletedKey] ?: false,
            )
        }

    suspend fun loadSuggestedTasteArtists(): List<TasteArtist> =
        listOf(
            "Telugu trending artists",
            "Tamil trending artists",
            "Bollywood singers",
            "Malayalam singers",
        ).flatMap { query ->
            searchTasteArtists(query, limit = 8)
        }.distinctBy { it.id }.take(24)

    suspend fun searchTasteArtists(query: String, limit: Int = 16): List<TasteArtist> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST)
                .getOrThrow()
                .items
                .extractArtists()
                .map { it.toTasteArtist() }
                .take(limit)
        }.getOrElse { emptyList() }
    }

    suspend fun relatedTasteArtists(artist: TasteArtist, limit: Int = 18): List<TasteArtist> {
        val pageArtists = runCatching {
            YouTube.artist(artist.id)
                .getOrThrow()
                .sections
                .flatMap { it.items }
                .extractArtists()
                .map { it.toTasteArtist() }
        }.getOrElse { emptyList() }

        val searchArtists = if (pageArtists.size < limit / 2) {
            searchTasteArtists("${artist.name} similar artists", limit)
        } else {
            emptyList()
        }

        return (pageArtists + searchArtists)
            .filterNot { it.id == artist.id }
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun saveTasteProfile(profile: TasteProfile) {
        val seededProfile = profile.seeded()
        context.dataStore.edit { preferences ->
            preferences[TasteOnboardingCompletedKey] = true
            preferences[PreferredLanguageTagsKey] = seededProfile.selectedLanguages
            preferences[PreferredIndustryTagsKey] = seededProfile.selectedIndustries
            preferences[PreferredArtistTagsKey] = seededProfile.favoriteArtists
        }
    }

    suspend fun refreshHomeContent() = coroutineScope {
        val tasteProfile = observeTasteProfile().first().seeded()
        val homeItemsDeferred = async {
            runCatching { YouTube.home().getOrThrow().sections.flatMap { it.items } }
                .getOrElse { emptyList() }
        }
        val chartItemsDeferred = async {
            runCatching { YouTube.getChartsPage().getOrThrow().sections.flatMap { it.items } }
                .getOrElse { emptyList() }
        }
        val historySongsDeferred = async { fetchHistorySongs() }
        val tasteItemsDeferred = async { fetchTasteDiscoveryItems(tasteProfile) }
        val homeItems = homeItemsDeferred.await()
        val chartItems = chartItemsDeferred.await()
        val historySongs = historySongsDeferred.await()
        val tasteItems = tasteItemsDeferred.await()
        val fallbackItems = if (homeItems.isEmpty()) fetchDiscoveryItems() else emptyList()
        val allItems = historySongs + tasteItems + homeItems + chartItems + fallbackItems
        val songQueries = buildSongQueries(tasteProfile)
        val playlistQueries = buildPlaylistQueries(tasteProfile)
        val albumQueries = buildAlbumQueries(tasteProfile)
        val artistQueries = buildArtistQueries(tasteProfile)

        val discoveredSongs = allItems.extractSongs().ifEmpty {
            searchSongs(*(songQueries + listOf("top songs", "trending music", "viral songs")).toTypedArray(), limit = 12)
        }
        val discoveredPlaylists = allItems.extractPlaylists().ifEmpty {
            searchPlaylists(*(playlistQueries + listOf("top playlists", "music mix", "new music playlist")).toTypedArray(), limit = 8)
        }
        val discoveredAlbums = allItems.extractAlbums().ifEmpty {
            fetchNewReleaseAlbums().ifEmpty {
                searchAlbums(*(albumQueries + listOf("new albums", "popular albums")).toTypedArray(), limit = 8)
            }
        }
        val discoveredArtists = allItems.extractArtists().ifEmpty {
            searchArtists(*(artistQueries + songQueries + listOf("popular singers")).toTypedArray(), limit = 8)
        }

        upsertSongsPreservingState(discoveredSongs)
        if (historySongs.isNotEmpty()) {
            upsertSongsPreservingState(historySongs.map { it.asSong }) { index ->
                copy(lastPlayedAt = LocalDateTime.now().minusSeconds(index.toLong()))
            }
        }
        mergePlaylists(discoveredPlaylists.map { it.toCastyPlaylistPreview() })
        mergeAlbums(discoveredAlbums.map { it.toCastyAlbum() })
        mergeArtists(discoveredArtists.map { it.toCastyArtist() })
    }

    suspend fun refreshLibraryContent() = coroutineScope {
        val historyDeferred = async { fetchHistorySongs() }
        val likedDeferred = async {
            runCatching { YouTube.playlist("LM").getOrThrow().songs }
                .getOrElse { emptyList() }
        }
        val watchLaterDeferred = async {
            runCatching { YouTube.playlist("WL").getOrThrow().songs }
                .getOrElse { emptyList() }
        }
        val playlistDeferred = async {
            runCatching { YouTube.library("FEmusic_liked_playlists").getOrThrow().items }
                .getOrElse { emptyList() }
        }
        val albumDeferred = async {
            runCatching { YouTube.library("FEmusic_liked_albums").getOrThrow().items }
                .getOrElse { emptyList() }
        }
        val artistDeferred = async {
            runCatching { YouTube.library("FEmusic_library_corpus_artists").getOrThrow().items }
                .getOrElse { emptyList() }
        }

        val historySongs = historyDeferred.await()
        val likedSongItems = likedDeferred.await()
        val watchLaterItems = watchLaterDeferred.await()
        val playlistItems = playlistDeferred.await()
        val albumItems = albumDeferred.await()
        val artistItems = artistDeferred.await()

        val libraryItems = playlistItems + albumItems + artistItems
        val likedSongs = likedSongItems.mapIndexed { index, song ->
            song.asSong.copy(likedAt = LocalDateTime.now().minusSeconds(index.toLong()))
        }
        val watchLaterSongs = watchLaterItems.map { it.asSong }

        if (likedSongs.isNotEmpty()) {
            upsertSongsPreservingState(likedSongs.take(MAX_LIBRARY_ITEMS)) {
                copy(
                    inLibrary = true,
                    librarySyncedAt = LocalDateTime.now(),
                    likedAt = likedAt ?: LocalDateTime.now(),
                )
            }
        }
        if (watchLaterSongs.isNotEmpty()) {
            upsertSongsPreservingState(watchLaterSongs.take(MAX_LIBRARY_ITEMS)) {
                copy(
                    inWatchLater = true,
                    watchLaterAddedAt = LocalDateTime.now(),
                )
            }
        }
        if (historySongs.isNotEmpty()) {
            upsertSongsPreservingState(historySongs.map { it.asSong }) { index ->
                copy(lastPlayedAt = LocalDateTime.now().minusSeconds(index.toLong()))
            }
        }

        if (libraryItems.isNotEmpty()) {
            mergePlaylists(libraryItems.extractPlaylists().map { it.toCastyPlaylistPreview() })
            mergeAlbums(libraryItems.extractAlbums().map { it.toCastyAlbum() })
            mergeArtists(libraryItems.extractArtists().map { it.toCastyArtist() })
        }
    }

    fun rememberSearchResults(items: List<YTItem>) {
        rememberSongs(items.extractSongs())
        // Don't persist playlists/albums/artists from search to reduce data growth
    }

    fun insertSearchQuery(query: SearchQuery) {
        repositoryScope.launch {
            recentSearchDao.insert(RecentSearchEntity(query = query.query))
        }
    }

    fun deleteSearchQuery(query: SearchQuery) {
        repositoryScope.launch {
            recentSearchDao.delete(query.query)
        }
    }

    suspend fun toggleLike(song: Song) {
        val current = songDao.getById(song.id)?.toSong() ?: song
        val shouldLike = current.likedAt == null
        runCatching { YouTube.likeVideo(current.id, shouldLike).getOrThrow() }
        upsertSongPreservingState(current)

        songDao.setLiked(
            id = current.id,
            likedAt = if (shouldLike) LocalDateTime.now() else null
        )
    }

    fun markDownloaded(newSongs: List<Song>) {
        repositoryScope.launch {
            newSongs.forEach { song ->
                upsertSongPreservingState(song) {
                    copy(isDownloaded = true, downloadedAt = LocalDateTime.now())
                }
            }
        }
    }

    suspend fun insertPlaylist(playlist: Playlist): String {
        val youtubeId = runCatching { YouTube.createPlaylist(playlist.name) }.getOrNull()
        val stored = if (youtubeId.isNullOrBlank()) {
            playlist
        } else {
            playlist.copy(id = youtubeId, isYoutubePlaylist = true)
        }
        playlistDao.upsertPlaylist(stored.toEntity())
        return stored.id
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        if (playlist.isYoutubePlaylist && !playlist.id.startsWith("local-")) {
            runCatching { YouTube.deletePlaylist(playlist.id).getOrThrow() }
        }
        playlistDao.deletePlaylist(playlist.id)
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        repositoryScope.launch {
            upsertSongPreservingState(song)
            val currentCount = playlistDao.getSongCount(playlistId)
            playlistDao.insertCrossRef(
                PlaylistSongCrossRef(
                    playlistId = playlistId,
                    songId = song.id,
                    position = currentCount
                )
            )
            // Update denormalized count
            val pl = playlistDao.getPlaylist(playlistId)
            pl?.let { playlistDao.upsertPlaylist(it.copy(songCount = currentCount + 1, updatedAt = LocalDateTime.now())) }
        }
    }

    fun deleteSongFromPlaylist(songId: String, playlistId: String) {
        repositoryScope.launch {
            playlistDao.removeSongFromPlaylist(playlistId, songId)
            val remaining = playlistDao.getSongCount(playlistId)
            val pl = playlistDao.getPlaylist(playlistId)
            pl?.let { playlistDao.upsertPlaylist(it.copy(songCount = remaining, updatedAt = LocalDateTime.now())) }
        }
    }

    fun getPlaylistSongs(
        playlistId: String,
        sortBy: PlaylistSongSortBy = PlaylistSongSortBy.Position,
        sortOrder: SortOrder = SortOrder.Ascending,
    ): Flow<List<Song>> =
        playlistDao.observeSongsInPlaylist(playlistId).map { entities ->
            entities.map { it.toSong() }
        }.recoverRoomRead("playlist songs", emptyList())

    fun getSong(songId: String): Flow<Song?> =
        songDao.observeById(songId).map { it?.toSong() }
            .recoverRoomRead("song", null)

    fun rememberSong(song: Song) {
        repositoryScope.launch {
            upsertSongPreservingState(song)
        }
    }

    private fun rememberSongs(newSongs: List<Song>) {
        if (newSongs.isEmpty()) return
        repositoryScope.launch {
            upsertSongsPreservingState(newSongs)
        }
    }

    private suspend fun mergePlaylists(newPlaylists: List<PlaylistPreview>) {
        if (newPlaylists.isEmpty()) return
        newPlaylists.forEach { preview ->
            playlistDao.upsertPlaylist(preview.playlist.toEntity(songCount = preview.songCount))
        }
    }

    private suspend fun upsertSongsPreservingState(
        newSongs: List<Song>,
        transform: DbSongEntity.(index: Int) -> DbSongEntity = { this },
    ) {
        if (newSongs.isEmpty()) return
        newSongs.distinctBy { it.id }.forEachIndexed { index, song ->
            upsertSongPreservingState(song) { transform(index) }
        }
    }

    private suspend fun upsertSongPreservingState(
        song: Song,
        transform: DbSongEntity.() -> DbSongEntity = { this },
    ) {
        val incoming = song.toEntity()
        val existing = songDao.getById(song.id)
        val merged = existing?.mergeMetadata(incoming) ?: incoming
        songDao.upsert(merged.transform().copy(updatedAt = LocalDateTime.now()))
    }

    private fun mergeAlbums(newAlbums: List<Album>) {
        if (newAlbums.isEmpty()) return
        repositoryScope.launch {
            val entities = newAlbums.map { AlbumEntity.fromAlbum(it) }
            database.albumDao().upsertAll(entities)
        }
    }

    private fun mergeArtists(newArtists: List<Artist>) {
        if (newArtists.isEmpty()) return
        repositoryScope.launch {
            val entities = newArtists.map { ArtistEntity.fromArtist(it) }
            database.artistDao().upsertAll(entities)
        }
    }

    private suspend fun fetchDiscoveryItems(): List<YTItem> =
        runCatching { YouTube.explore().getOrThrow() }
            .map { page -> page.newReleaseAlbums.map { it as YTItem } }
            .getOrElse { emptyList() }

    private suspend fun fetchNewReleaseAlbums(): List<AlbumItem> =
        runCatching { YouTube.newReleaseAlbums().getOrThrow() }.getOrElse { emptyList() }

    private suspend fun fetchHistorySongs(): List<SongItem> =
        runCatching {
            YouTube.musicHistory()
                .getOrThrow()
                .sections
                .orEmpty()
                .flatMap { it.songs }
                .distinctBy { it.id }
        }.getOrElse { emptyList() }

    private suspend fun fetchTasteDiscoveryItems(profile: TasteProfile): List<YTItem> =
        buildDiscoveryQueries(profile)
            .take(6)
            .flatMap { query ->
                runCatching {
                    YouTube.searchSummary(query)
                        .getOrThrow()
                        .summaries
                        .flatMap { it.items }
                        .take(16)
                }.getOrElse { emptyList() }
            }

    private suspend fun searchSongs(vararg queries: String, limit: Int): List<Song> =
        queries.firstNotNullOfOrNull { query ->
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                    .getOrThrow()
                    .items
                    .extractSongs()
                    .take(limit)
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    private suspend fun searchPlaylists(vararg queries: String, limit: Int): List<PlaylistItem> =
        queries.firstNotNullOfOrNull { query ->
            listOf(
                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST,
                YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST,
            ).firstNotNullOfOrNull { filter ->
                runCatching {
                    YouTube.search(query, filter)
                        .getOrThrow()
                        .items
                        .extractPlaylists()
                        .take(limit)
                }.getOrNull()?.takeIf { it.isNotEmpty() }
            }
        }.orEmpty()

    private suspend fun searchArtists(vararg queries: String, limit: Int): List<ArtistItem> =
        queries.firstNotNullOfOrNull { query ->
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST)
                    .getOrThrow()
                    .items
                    .extractArtists()
                    .take(limit)
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    private suspend fun searchAlbums(vararg queries: String, limit: Int): List<AlbumItem> =
        queries.firstNotNullOfOrNull { query ->
            runCatching {
                YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrThrow()
                    .items
                    .extractAlbums()
                    .take(limit)
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    private fun buildDiscoveryQueries(profile: TasteProfile): List<String> =
        (
            buildSongQueries(profile) +
                buildPlaylistQueries(profile) +
                buildAlbumQueries(profile)
            )
            .distinct()

    private fun buildSongQueries(profile: TasteProfile): List<String> =
        (
            profile.favoriteArtists.map { "$it songs" } +
                profile.selectedIndustries.map { "$it hits" } +
                profile.selectedLanguages.map { "$it songs" }
            )
            .distinct()

    private fun buildPlaylistQueries(profile: TasteProfile): List<String> =
        (
            profile.selectedIndustries.map { "$it playlist" } +
                profile.selectedLanguages.map { "$it music playlist" } +
                profile.favoriteArtists.map { "$it mix" }
            )
            .distinct()

    private fun buildAlbumQueries(profile: TasteProfile): List<String> =
        (
            profile.favoriteArtists.map { "$it album" } +
                profile.selectedIndustries.map { "$it albums" } +
                profile.selectedLanguages.map { "$it albums" }
            )
            .distinct()

    private fun buildArtistQueries(profile: TasteProfile): List<String> =
        (
            profile.favoriteArtists.toList() +
                profile.selectedIndustries.map { "$it singers" } +
                profile.selectedLanguages.map { "$it artists" }
            )
            .distinct()

    private companion object {
        private const val MAX_RECENT_ITEMS = 40
        private const val MAX_LIBRARY_ITEMS = 300
        private const val MAX_SONGS_CACHE = 2000
        private const val MAX_COLLECTION_CACHE = 200
    }
}

private fun List<YTItem>.extractSongs(): List<Song> =
    flatMap { item ->
        when (item) {
            is SongItem -> listOf(item.asSong)
            is EpisodeItem -> listOf(item.asSongItem().asSong)
            else -> emptyList()
        }
    }.distinctBy { it.id }

private fun List<YTItem>.extractPlaylists(): List<PlaylistItem> =
    filterIsInstance<PlaylistItem>().distinctBy { it.id }

private fun List<YTItem>.extractAlbums(): List<AlbumItem> =
    filterIsInstance<AlbumItem>().distinctBy { it.id }

private fun List<YTItem>.extractArtists(): List<ArtistItem> =
    filterIsInstance<ArtistItem>().distinctBy { it.id }

private fun ArtistItem.toTasteArtist(): TasteArtist =
    TasteArtist(
        id = id,
        name = title,
        thumbnailUrl = thumbnail,
    )

// ===================================================================
// Best-ever mappers between Room entities and domain models (Casty v1 persistence)
// These keep ViewModels, screens, and all call sites unchanged.
// ===================================================================

private fun DbSongEntity.toSong(): Song = Song(
    id = id,
    title = title,
    artistsText = artistsText,
    durationText = durationText ?: durationMs
        .takeIf { it > 0L }
        ?.let { durationSecondsToText((it / 1000L).toInt()) },
    thumbnailUrl = thumbnailUrl,
    likedAt = likedAt
)

private fun Song.toEntity(): DbSongEntity = DbSongEntity(
    id = id,
    title = title,
    artistsText = artistsText,
    durationMs = durationText?.let(::durationTextToMillis)?.takeIf { it > 0L } ?: 0L,
    durationText = durationText,
    thumbnailUrl = thumbnailUrl,
    likedAt = likedAt,
    inLibrary = likedAt != null,
    librarySyncedAt = if (likedAt != null) LocalDateTime.now() else null,
)

private fun DbSongEntity.mergeMetadata(incoming: DbSongEntity): DbSongEntity {
    val incomingAudioQuality = incoming.audioQuality.takeUnless { it == "AUTO" } ?: audioQuality
    val incomingDurationMs = incoming.durationMs.takeIf { it > 0L } ?: durationMs

    return copy(
        // Fresh metadata from YouTube/backend.
        title = incoming.title.ifBlank { title },
        artistsText = incoming.artistsText ?: artistsText,
        albumTitle = incoming.albumTitle ?: albumTitle,
        albumId = incoming.albumId ?: albumId,
        durationMs = incomingDurationMs,
        durationText = incoming.durationText ?: durationText,
        thumbnailUrl = incoming.thumbnailUrl ?: thumbnailUrl,
        highResThumbnailUrl = incoming.highResThumbnailUrl ?: highResThumbnailUrl,
        audioQuality = incomingAudioQuality,
        availableFormats = incoming.availableFormats.takeIf { it.isNotEmpty() } ?: availableFormats,
        currentFormat = incoming.currentFormat ?: currentFormat,
        genre = incoming.genre ?: genre,
        year = incoming.year ?: year,
        explicit = explicit || incoming.explicit,
        isLive = isLive || incoming.isLive,
        isPremiere = isPremiere || incoming.isPremiere,
        language = incoming.language ?: language,
        similarityHash = incoming.similarityHash ?: similarityHash,
        moodTags = incoming.moodTags.takeIf { it.isNotEmpty() } ?: moodTags,

        // User/library state must survive every metadata refresh.
        likedAt = incoming.likedAt ?: likedAt,
        inLibrary = inLibrary || incoming.inLibrary || incoming.likedAt != null,
        librarySyncedAt = librarySyncedAt ?: incoming.librarySyncedAt,
        inWatchLater = inWatchLater || incoming.inWatchLater,
        watchLaterAddedAt = watchLaterAddedAt ?: incoming.watchLaterAddedAt,
        isHidden = isHidden || incoming.isHidden,
        updatedAt = LocalDateTime.now(),
        version = version + 1,
    )
}

private fun PlaylistEntity.toPreview(): PlaylistPreview = PlaylistPreview(
    playlist = Playlist(
        id = id,
        name = name,
        isYoutubePlaylist = isYoutubePlaylist,
        thumbnailUrl = thumbnailUrl,
        authorText = authorText
    ),
    songCount = songCount
)

private fun Playlist.toEntity(songCount: Int = 0): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    isYoutubePlaylist = isYoutubePlaylist,
    thumbnailUrl = thumbnailUrl,
    authorText = authorText,
    songCount = songCount,
)
