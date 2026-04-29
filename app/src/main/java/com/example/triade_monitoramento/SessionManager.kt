package com.example.triade_monitoramento.data

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "triade_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_IS_LOGGED = "is_logged"

    fun saveLogin(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED, true)
            .apply()
    }

    fun getLoggedUserId(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val isLogged = prefs.getBoolean(KEY_IS_LOGGED, false)
        if (!isLogged) return null

        val userId = prefs.getInt(KEY_USER_ID, -1)
        return if (userId != -1) userId else null
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}