package com.casty.music.backend.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "Casty_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember {
        context.dataStore.data
            .map { it[key] ?: defaultValue }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(defaultValue)

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    scope.launch { context.dataStore.edit { it[key] = value } }
                }

            override fun component1(): T = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

fun reportException(throwable: Throwable) {
    timber.log.Timber.e(throwable)
}
