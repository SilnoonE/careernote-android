package com.thirtytwo_cereernote.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val languageKey = stringPreferencesKey("language")
    private val themeKey = stringPreferencesKey("theme")
    private val draftCoverLetterKey = stringPreferencesKey("draft_cover_letter")
    private val draftMemoKey = stringPreferencesKey("draft_memo")
    private val draftTimeKey = stringPreferencesKey("draft_time")

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[languageKey] ?: "ko"
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[themeKey] ?: "system"
    }

    private val draftsKey = stringPreferencesKey("drafts_json")

    val draftsJson: Flow<String> = context.dataStore.data.map { preferences ->
        var json = preferences[draftsKey] ?: "{}"
        
        // Legacy migration
        if (json == "{}") {
            val legacyCoverLetter = preferences[draftCoverLetterKey]
            val legacyMemo = preferences[draftMemoKey]
            if (legacyCoverLetter != null || legacyMemo != null) {
                json = "{\"cover_letter_0\":{\"type\":\"cover_letter\",\"itemId\":0,\"field1\":\"$legacyCoverLetter\",\"field3\":\"$legacyMemo\"}}"
            }
        }
        json
    }

    suspend fun updateDraft(type: String, itemId: Long, update: (com.thirtytwo_cereernote.data.model.Draft?) -> com.thirtytwo_cereernote.data.model.Draft?) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[draftsKey] ?: "{}"
            val draftsMap = try {
                Json.decodeFromString<Map<String, com.thirtytwo_cereernote.data.model.Draft>>(currentJson).toMutableMap()
            } catch (_: Exception) {
                mutableMapOf()
            }
            
            val key = "${type}_$itemId"
            val existing = draftsMap[key]
            val updated = update(existing)
            
            if (updated == null) {
                draftsMap.remove(key)
            } else {
                draftsMap[key] = updated
            }
            
            preferences[draftsKey] = Json.encodeToString(draftsMap.toMap())
        }
    }

    suspend fun saveDraftJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[draftsKey] = json
        }
    }

    suspend fun clearDrafts() {
        context.dataStore.edit { preferences ->
            preferences.remove(draftsKey)
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[languageKey] = language
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme
        }
    }
}
