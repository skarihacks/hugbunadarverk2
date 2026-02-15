package com.hbv501g.forumapp.data.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hbv501g.forumapp.data.model.UserSession
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "forum_session")

class SessionStore(private val context: Context) {

    companion object {
        private val SESSION_ID = stringPreferencesKey("session_id")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USERNAME = stringPreferencesKey("username")
        private val EMAIL = stringPreferencesKey("email")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> prefs.toSession() }

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[SESSION_ID] = session.sessionId
            prefs[USER_ID] = session.userId
            prefs[USERNAME] = session.username
            prefs[EMAIL] = session.email
        }
    }

    suspend fun currentSessionId(): String? = sessionFlow.first()?.sessionId

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(SESSION_ID)
            prefs.remove(USER_ID)
            prefs.remove(USERNAME)
            prefs.remove(EMAIL)
        }
    }

    private fun Preferences.toSession(): UserSession? {
        val sessionId = this[SESSION_ID] ?: return null
        val userId = this[USER_ID] ?: return null
        val username = this[USERNAME] ?: return null
        val email = this[EMAIL] ?: return null
        return UserSession(
            sessionId = sessionId,
            userId = userId,
            username = username,
            email = email
        )
    }
}
