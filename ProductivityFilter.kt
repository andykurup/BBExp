package com.bbexperience.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val isSocialOrGame: Boolean = false
)

class ProductivityFilter(private val context: Context) {

    private val alwaysAllowedPackages = setOf(
        "com.blackberry.hub",
        "com.blackberry.calendar",
        "com.blackberry.notes",
        "com.blackberry.tasks",
        "com.blackberry.contacts",
        "com.blackberry.infrastructure"
    )

    private val socialAndGameTokens = listOf(
        "instagram", "facebook", "tiktok", "snapchat", "twitter", "x.android",
        "reddit", "youtube", "netflix", "twitch", "pinterest", "tinder",
        "bumble", "hulu", "disney", "primevideo", "whatsapp", "telegram", "discord"
    )

    private val distractionCategories = setOf(
        ApplicationInfo.CATEGORY_GAME,
        ApplicationInfo.CATEGORY_SOCIAL
    )

    fun getAllLaunchableApps(): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null

            val isSocialOrGame = if (alwaysAllowedPackages.contains(pkg)) {
                false
            } else {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val categoryMatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        distractionCategories.contains(appInfo.category)
                    } else false

                    val tokenMatch = socialAndGameTokens.any { pkg.contains(it, ignoreCase = true) }
                    categoryMatch || tokenMatch
                } catch (e: Exception) {
                    false
                }
            }

            AppItem(
                label = resolveInfo.loadLabel(pm).toString(),
                packageName = pkg,
                icon = resolveInfo.loadIcon(pm),
                isSocialOrGame = isSocialOrGame
            )
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}
