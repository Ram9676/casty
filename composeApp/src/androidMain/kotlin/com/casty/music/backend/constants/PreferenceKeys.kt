package com.casty.music.backend.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

val AudioQualityKey = stringPreferencesKey("audioQuality")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")
val TasteOnboardingCompletedKey = booleanPreferencesKey("tasteOnboardingCompleted")
val PreferredLanguageTagsKey = stringSetPreferencesKey("preferredLanguageTags")
val PreferredIndustryTagsKey = stringSetPreferencesKey("preferredIndustryTags")
val PreferredArtistTagsKey = stringSetPreferencesKey("preferredArtistTags")

enum class AudioQuality {
    AUTO,
    LOW,
    HIGH,
    VERY_HIGH,
}
