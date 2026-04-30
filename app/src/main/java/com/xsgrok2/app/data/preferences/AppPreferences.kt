package com.xsgrok2.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("xsgrok2_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var model: String
        get() = prefs.getString("model", "grok-4.20-beta") ?: "grok-4.20-beta"
        set(value) = prefs.edit().putString("model", value).apply()

    var apiBaseUrl: String
        get() = prefs.getString("api_base_url", "https://api.apiyi.com/v1") ?: "https://api.apiyi.com/v1"
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    val isApiKeySet: Boolean
        get() = apiKey.isNotEmpty()

    companion object {
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
