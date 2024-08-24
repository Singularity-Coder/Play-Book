package com.singularitycoder.playbooks.helpers

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

// https://github.com/enricocid/Music-Player-GO
class AppPreferences(context: Context) {

    companion object {
        // Singleton prevents multiple instances of AppPreferences opening at the same time.
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun init(context: Context): AppPreferences {
            // if the INSTANCE is not null, then return it, if it is, then create the preferences
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context)
                INSTANCE = instance
                // return instance
                instance
            }
        }

        fun getInstance(): AppPreferences {
            return INSTANCE ?: error("Preferences not initialized!")
        }
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var latestVolume: Int
        get() = sharedPreferences.getInt("latest_volume_pref", 100)
        set(value) = sharedPreferences.edit { putInt("latest_volume_pref", value) }

    var ttsSpeechRate: Float
        get() = sharedPreferences.getFloat("TTS_SPEECH_RATE_PREF", 1.0F)
        set(value) = sharedPreferences.edit { putFloat("TTS_SPEECH_RATE_PREF", value) }

    var ttsPitch: Float
        get() = sharedPreferences.getFloat("TTS_PITCH_PREF", 1.0F)
        set(value) = sharedPreferences.edit { putFloat("TTS_PITCH_PREF", value) }

    var ttsLanguage: String
        get() = sharedPreferences.getString("TTS_LANGUAGE", "") ?: ""
        set(value) = sharedPreferences.edit { putString("TTS_LANGUAGE", value) }
}
