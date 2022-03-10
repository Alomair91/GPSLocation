package com.omairtech.gpslocation.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

internal class SessionPreference private constructor(context: Context) {
    private val ref: SharedPreferences =
        context.getSharedPreferences("SettingsRef", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: SessionPreference? = null

        fun getInstance(context: Context): SessionPreference {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionPreference(context).also { INSTANCE = it }
            }
        }

        const val IS_FOREGROUND_ON = "isForegroundOn"
        const val IS_BACKGROUND_ON = "isBackgroundOn"
        const val KEY_REQUESTING_LOCATION_UPDATES = "LocationUpdateEnable"
    }

    internal fun saveForeground(isForegroundOn: Boolean) {
        val editor: SharedPreferences.Editor = ref.edit()
        editor.putBoolean(IS_FOREGROUND_ON, isForegroundOn)
        editor.apply()
    }

    val isForegroundOn: Boolean
        get() = ref.getBoolean(IS_FOREGROUND_ON, false)

    internal fun saveBackground(isBackgroundOn: Boolean) {
        val editor: SharedPreferences.Editor = ref.edit()
        editor.putBoolean(IS_BACKGROUND_ON, isBackgroundOn)
        editor.apply()
    }

    val isBackgroundOn: Boolean
        get() = ref.getBoolean(IS_BACKGROUND_ON, false)


    fun setRequestingLocationUpdate(value: Boolean) {
        val editor: SharedPreferences.Editor = ref.edit()
        editor.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, value).apply()
        editor.apply()
    }

    val requestingLocationUpdates: Boolean
        get() = ref.getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)

}