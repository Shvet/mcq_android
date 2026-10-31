package com.onegoal.mcq.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.onegoal.mcq.model.AppConfig
import com.onegoal.mcq.model.ReadingPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "mcq_prefs")

/** Small persisted state: last read position, cached app config, one-time hints. */
class AppPreferences(private val context: Context, private val json: Json) {

    private object Keys {
        val lastRead = stringPreferencesKey("last_read")
        val appConfig = stringPreferencesKey("app_config")
        val lastReadHintShown = booleanPreferencesKey("last_read_hint_shown")
        val nightMode = booleanPreferencesKey("night_mode")
        val pdfOpenCount = intPreferencesKey("pdf_open_count")
    }

    val lastRead: Flow<ReadingPosition?> = context.dataStore.data.map { prefs ->
        prefs[Keys.lastRead]?.let { runCatching { json.decodeFromString(ReadingPosition.serializer(), it) }.getOrNull() }
    }

    val appConfig: Flow<AppConfig> = context.dataStore.data.map { prefs ->
        prefs[Keys.appConfig]?.let { runCatching { json.decodeFromString(AppConfig.serializer(), it) }.getOrNull() } ?: AppConfig()
    }

    val nightMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.nightMode] ?: false }

    suspend fun isLastReadHintShown(): Boolean = context.dataStore.data.first()[Keys.lastReadHintShown] ?: false

    suspend fun setLastReadHintShown() = edit { it[Keys.lastReadHintShown] = true }

    suspend fun saveLastRead(position: ReadingPosition) = edit { it[Keys.lastRead] = json.encodeToString(ReadingPosition.serializer(), position) }

    suspend fun saveAppConfig(config: AppConfig) = edit { it[Keys.appConfig] = json.encodeToString(AppConfig.serializer(), config) }

    suspend fun setNightMode(enabled: Boolean) = edit { it[Keys.nightMode] = enabled }

    /** Increments and returns how many PDFs were opened, used to pace interstitial ads. */
    suspend fun incrementPdfOpenCount(): Int {
        var count = 0
        edit {
            count = (it[Keys.pdfOpenCount] ?: 0) + 1
            it[Keys.pdfOpenCount] = count
        }
        return count
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
