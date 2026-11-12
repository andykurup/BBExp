package com.bbexperience.launcher

import android.content.Context
import android.content.SharedPreferences

class BBPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bb_experience_prefs", Context.MODE_PRIVATE)

    var keyboardScale: Float
        get() = prefs.getFloat("keyboard_scale", 1.0f).coerceIn(0.75f, 1.35f)
        set(value) = prefs.edit().putFloat("keyboard_scale", value.coerceIn(0.75f, 1.35f)).apply()

    var themeMode: String
        get() = prefs.getString("theme_mode", "BBOS7") ?: "BBOS7"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    var wallpaperIndex: Int
        get() = prefs.getInt("wallpaper_index", 0)
        set(value) = prefs.edit().putInt("wallpaper_index", value).apply()

    var hasDismissedCompanionPrompt: Boolean
        get() = prefs.getBoolean("dismissed_companion_prompt", false)
        set(value) = prefs.edit().putBoolean("dismissed_companion_prompt", value).apply()

    fun initializeDefaultHomeFilter(allApps: List<AppItem>) {
        if (!prefs.getBoolean("initialized_home_filter_v2", false)) {
            val defaultHidden = allApps.filter { it.isSocialOrGame }.map { it.packageName }.toSet()
            prefs.edit()
                .putStringSet("hidden_home_packages", defaultHidden)
                .putBoolean("initialized_home_filter_v2", true)
                .apply()
        }
    }

    fun getHiddenHomePackages(): Set<String> {
        return prefs.getStringSet("hidden_home_packages", emptySet()) ?: emptySet()
    }

    fun setAppVisibleOnHome(packageName: String, visible: Boolean) {
        val current = getHiddenHomePackages().toMutableSet()
        if (visible) current.remove(packageName) else current.add(packageName)
        prefs.edit().putStringSet("hidden_home_packages", current).apply()
    }

    fun getShortcutPackage(keyChar: String): String? {
        return prefs.getString("shortcut_${keyChar.uppercase()}", null)
    }

    fun setShortcutPackage(keyChar: String, packageName: String?) {
        val editor = prefs.edit()
        if (packageName.isNullOrBlank()) {
            editor.remove("shortcut_${keyChar.uppercase()}")
        } else {
            editor.putString("shortcut_${keyChar.uppercase()}", packageName)
        }
        editor.apply()
    }
}
