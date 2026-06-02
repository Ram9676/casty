package com.casty.music.backend.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences: SharedPreferences = createPreferences(context.applicationContext)

    var innerTubeCookie: String?
        get() = readString(KEY_INNER_TUBE_COOKIE).takeUnless { it.isNullOrBlank() }
        set(value) = putNullable(KEY_INNER_TUBE_COOKIE, value)

    var visitorData: String?
        get() = readString(KEY_VISITOR_DATA).takeUnless { it.isNullOrBlank() || it == "null" }
        set(value) = putNullable(KEY_VISITOR_DATA, value)

    var dataSyncId: String?
        get() = readString(KEY_DATA_SYNC_ID).takeUnless { it.isNullOrBlank() || it == "null" }
        set(value) = putNullable(KEY_DATA_SYNC_ID, value)

    var accountName: String?
        get() = readString(KEY_ACCOUNT_NAME).takeUnless { it.isNullOrBlank() }
        set(value) = putNullable(KEY_ACCOUNT_NAME, value)

    var accountEmail: String?
        get() = readString(KEY_ACCOUNT_EMAIL).takeUnless { it.isNullOrBlank() }
        set(value) = putNullable(KEY_ACCOUNT_EMAIL, value)

    var accountChannelHandle: String?
        get() = readString(KEY_ACCOUNT_CHANNEL_HANDLE).takeUnless { it.isNullOrBlank() }
        set(value) = putNullable(KEY_ACCOUNT_CHANNEL_HANDLE, value)

    fun updateSession(
        innerTubeCookie: String? = this.innerTubeCookie,
        visitorData: String? = this.visitorData,
        dataSyncId: String? = this.dataSyncId,
    ) {
        preferences.edit()
            .putOrRemove(KEY_INNER_TUBE_COOKIE, innerTubeCookie)
            .putOrRemove(KEY_VISITOR_DATA, visitorData)
            .putOrRemove(KEY_DATA_SYNC_ID, dataSyncId)
            .apply()
    }

    fun updateAccount(name: String?, email: String?, channelHandle: String?) {
        preferences.edit()
            .putOrRemove(KEY_ACCOUNT_NAME, name)
            .putOrRemove(KEY_ACCOUNT_EMAIL, email)
            .putOrRemove(KEY_ACCOUNT_CHANNEL_HANDLE, channelHandle)
            .apply()
    }

    fun clear() {
        runCatching { preferences.edit().clear().apply() }
            .onFailure { Timber.e(it, "Unable to clear Casty secure session") }
    }

    private fun readString(key: String): String? {
        return runCatching { preferences.getString(key, null) }
            .onFailure { throwable ->
                Timber.e(throwable, "Unable to read Casty secure preference: $key")
                runCatching { preferences.edit().remove(key).apply() }
            }
            .getOrNull()
    }

    private fun putNullable(key: String, value: String?) {
        runCatching { preferences.edit().putOrRemove(key, value).apply() }
            .onFailure { Timber.e(it, "Unable to write Casty secure preference: $key") }
    }

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: String?): SharedPreferences.Editor =
        if (value.isNullOrBlank() || value == "null") remove(key) else putString(key, value)

    private fun createPreferences(context: Context): SharedPreferences {
        // Triple layer of defense. EncryptedSharedPreferences + MasterKey is fragile
        // on many devices (especially Xiaomi, Samsung, after OS updates, etc.).
        // This has historically caused "app exits immediately" on launch.
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t1: Throwable) {
            try {
                // Second attempt with a fresh MasterKey
                val masterKey = MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context.applicationContext,
                    SECURE_PREFS_NAME + "_retry",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (t2: Throwable) {
                // Final nuclear fallback - never crash the app because of secure prefs
                android.util.Log.e("CastySecureStore", "Encrypted prefs completely broken. Using plain SharedPreferences. Error1=$t1 Error2=$t2")
                context.applicationContext.getSharedPreferences(PRIVATE_FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    companion object {
        private const val SECURE_PREFS_NAME = "casty_secure_session"
        private const val PRIVATE_FALLBACK_PREFS_NAME = "casty_private_session_fallback"
        private const val KEY_INNER_TUBE_COOKIE = "inner_tube_cookie"
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_DATA_SYNC_ID = "data_sync_id"
        private const val KEY_ACCOUNT_NAME = "account_name"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_ACCOUNT_CHANNEL_HANDLE = "account_channel_handle"
    }
}
