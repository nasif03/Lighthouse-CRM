package com.project.lighthouse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicReference

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    // Cache token in memory for synchronous access in interceptors
    private val cachedToken = AtomicReference<String?>()

    init {
        // Load token into cache on initialization
        runBlocking {
            cachedToken.set(getTokenFromStore())
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
        cachedToken.set(token)
    }

    suspend fun getToken(): String? {
        val token = getTokenFromStore()
        cachedToken.set(token)
        return token
    }

    // Synchronous method for interceptors
    fun getTokenSync(): String? {
        return cachedToken.get()
    }

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
        cachedToken.set(null)
    }

    private suspend fun getTokenFromStore(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.first()
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val token = preferences[TOKEN_KEY]
        cachedToken.set(token)
        token
    }
}

