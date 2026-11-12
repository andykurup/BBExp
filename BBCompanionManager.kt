package com.bbexperience.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class CompanionAppSpec(
    val title: String,
    val subtitle: String,
    val packageName: String,
    val isRequired: Boolean,
    val isInstalled: Boolean
)

class BBCompanionManager(private val context: Context) {

    private val suiteDefinitions = listOf(
        Triple("BlackBerry Hub+ Services", "Core framework required for BB Hub, Calendar & Notes", "com.blackberry.infrastructure"),
        Triple("BlackBerry Hub (Unified Inbox)", "Aggregates Emails, SMS, WhatsApp & Calls in one feed", "com.blackberry.hub"),
        Triple("BlackBerry Calendar", "Enterprise agenda & meeting join integration", "com.blackberry.calendar"),
        Triple("BlackBerry Notes & Tasks", "Synced scratchpad & checklist widgets for Slider DOC", "com.blackberry.notes")
    )

    fun getSuiteStatus(): List<CompanionAppSpec> {
        val pm = context.packageManager
        return suiteDefinitions.mapIndexed { index, (title, subtitle, pkg) ->
            val installed = isPackageInstalled(pm, pkg)
            CompanionAppSpec(
                title = title,
                subtitle = subtitle,
                packageName = pkg,
                isRequired = index <= 1,
                isInstalled = installed
            )
        }
    }

    fun getMissingCount(): Int = getSuiteStatus().count { !it.isInstalled }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openPlayStoreForPackage(packageName: String) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
