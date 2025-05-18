package com.singularitycoder.playbooks.helpers

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.singularitycoder.playbooks.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject

class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Key {
        val TTS_SPEECH_RATE_PREF = intPreferencesKey("TTS_SPEECH_RATE_PREF")
        val TTS_PITCH_PREF = intPreferencesKey("TTS_PITCH_PREF")
        val TTS_LANGUAGE = stringPreferencesKey("TTS_LANGUAGE")
    }

    suspend fun getTtsSpeechRate(): Int? {
        return context.dataStore.data.first()[Key.TTS_SPEECH_RATE_PREF]
    }

    suspend fun setTtsSpeechRate(speech: Int) {
        context.dataStore.edit { mutablePrefs: MutablePreferences ->
            mutablePrefs[Key.TTS_SPEECH_RATE_PREF] = speech
        }
    }

    suspend fun getTtsPitch(): Int? {
        return context.dataStore.data.first()[Key.TTS_PITCH_PREF]
    }

    suspend fun setTtsPitch(pitch: Int) {
        context.dataStore.edit { mutablePrefs: MutablePreferences ->
            mutablePrefs[Key.TTS_PITCH_PREF] = pitch
        }
    }

    suspend fun getTtsLanguage(): String {
        return context.dataStore.data.first()[Key.TTS_LANGUAGE] ?: Locale.getDefault().displayName
    }

    suspend fun setTtsLanguage(lang: String) {
        context.dataStore.edit { mutablePrefs: MutablePreferences ->
            mutablePrefs[Key.TTS_LANGUAGE] = lang
        }
    }
}

val Context.dataStore by preferencesDataStore(
    name = BuildConfig.APPLICATION_ID
)