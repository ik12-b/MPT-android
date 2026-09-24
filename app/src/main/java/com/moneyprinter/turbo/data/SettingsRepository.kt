package com.moneyprinter.turbo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        val LLM_API_KEY = stringPreferencesKey("llm_api_key")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val PEXELS_API_KEY = stringPreferencesKey("pexels_api_key")
    }

    val llmBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.LLM_BASE_URL] ?: "https://api.openai.com"
    }
    val llmApiKey: Flow<String> = context.dataStore.data.map { it[Keys.LLM_API_KEY] ?: "" }
    val llmModel: Flow<String> = context.dataStore.data.map { it[Keys.LLM_MODEL] ?: "gpt-4o-mini" }
    val pexelsApiKey: Flow<String> = context.dataStore.data.map { it[Keys.PEXELS_API_KEY] ?: "" }

    suspend fun saveLlm(baseUrl: String, apiKey: String, model: String) {
        context.dataStore.edit {
            it[Keys.LLM_BASE_URL] = baseUrl
            it[Keys.LLM_API_KEY] = apiKey
            it[Keys.LLM_MODEL] = model
        }
    }

    suspend fun savePexels(apiKey: String) {
        context.dataStore.edit { it[Keys.PEXELS_API_KEY] = apiKey }
    }
}
