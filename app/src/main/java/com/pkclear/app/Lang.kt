package com.pkclear.app

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object Lang {
    private const val PREF = "lang_prefs"
    private const val KEY = "lang"

    fun get(ctx: Context): String {
        val saved = ctx.getSharedPreferences(PREF, 0).getString(KEY, null)
        if (saved == "ru" || saved == "en") return saved
        return if (Locale.getDefault().language == "en") "en" else "ru"
    }

    fun set(ctx: Context, code: String) {
        ctx.getSharedPreferences(PREF, 0).edit().putString(KEY, code).apply()
    }

    fun wrap(ctx: Context): Context {
        val locale = Locale(get(ctx))
        Locale.setDefault(locale)
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocale(locale)
        return ctx.createConfigurationContext(cfg)
    }
}