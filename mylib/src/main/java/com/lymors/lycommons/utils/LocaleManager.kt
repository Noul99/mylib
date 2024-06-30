package com.lymors.lycommons.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    private const val PREFS_NAME = "MyPrefs"
    private const val KEY_LANGUAGE = "language"

    fun setLocale(context: Context, languageCode: String) {
        persistLanguage(context, languageCode)
        updateResources(context, languageCode)
    }

    fun loadLocale(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language)
        language?.let {
            updateResources(context, it)
        }
    }


    private fun persistLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_LANGUAGE, languageCode)
        editor.apply()
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

}