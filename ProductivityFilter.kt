package com.bbexperience.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.Build

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable
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

    private val blockedPackageTokens = listOf(
        "instagram", "facebook", "tiktok", "snapchat", "twitter", "x.android",
        "reddit", "youtube", "netflix", "twitch", "pinterest", "tinder",
        "bumble", "hulu", "disney", "primevideo"
    )

    private val blockedCategories = setOf(
        ApplicationInfo.CATEGORY_GAME,
        ApplicationInfo.CATEGORY_SOCIAL
    )

    fun getFilteredApps(): List<AppItem> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null

            val isExplicitlyAllowed = alwaysAllowedPackages.contains(pkg)
            val isBlocked = if (isExplicitlyAllowed) {
                false
            } else {
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val categoryBlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        blockedCategories.contains(appInfo.category)
                    } else false

                    val tokenBlocked = blockedPackageTokens.any { pkg.contains(it, ignoreCase = true) }
                    categoryBlocked || tokenBlocked
                } catch (e: Exception) {
                    false
                }
            }

            if (!isBlocked) {
                AppItem(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = pkg,
                    icon = resolveInfo.loadIcon(pm)
                )
            } else {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }
}
