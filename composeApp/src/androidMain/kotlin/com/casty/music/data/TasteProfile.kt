package com.casty.music.data

data class TasteProfile(
    val selectedLanguages: Set<String> = emptySet(),
    val selectedIndustries: Set<String> = emptySet(),
    val favoriteArtists: Set<String> = emptySet(),
    val isCompleted: Boolean = false,
) {
    val hasSelections: Boolean
        get() = selectedLanguages.isNotEmpty() || selectedIndustries.isNotEmpty() || favoriteArtists.isNotEmpty()

    fun highlightChips(limit: Int = 8): List<String> =
        (selectedLanguages.sorted() + selectedIndustries.sorted() + favoriteArtists.sorted())
            .distinct()
            .take(limit)

    fun seeded(): TasteProfile =
        if (hasSelections) {
            this
        } else {
            copy(
                selectedLanguages = DEFAULT_LANGUAGES,
                selectedIndustries = DEFAULT_INDUSTRIES,
            )
        }

    companion object {
        val DEFAULT_LANGUAGES = setOf("Telugu", "Tamil", "Hindi")
        val DEFAULT_INDUSTRIES = setOf("Tollywood", "Kollywood", "Bollywood")
    }
}

data class TasteArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
)
