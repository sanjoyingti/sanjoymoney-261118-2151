package com.jaiho.sss.in.data

import android.content.Context
import android.content.Intent
import com.jaiho.sss.in.util.L

/** The package the game normally ships under. */
private const val GAME_PACKAGE = "com.jaiho.sss.in"

object TargetResolver {

    /** A launch intent for the given package, or null when it is not installed. */
    fun launchIntentFor(context: Context, pkg: String): Intent? {
        if (pkg.isBlank()) return null
        return try {
            context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (t: Throwable) {
            L.e("launch intent failed for $pkg", t)
            null
        }
    }

    fun isInstalled(context: Context, pkg: String): Boolean = launchIntentFor(context, pkg) != null

    /**
     * Every installed launcher app whose package mentions "jaiho", except this app.
     * The game can never be this app, so its own package is filtered out on purpose.
     */
    fun candidates(context: Context): List<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val self = context.packageName
            context.packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { it.activityInfo?.packageName }
                .distinct()
                .filter { it != self && it.contains("jaiho", ignoreCase = true) }
        } catch (t: Throwable) {
            L.e("target scan failed", t)
            emptyList()
        }
    }

    /**
     * The package the bot should drive, or an empty string when no game is installed.
     * A package the user stored by hand wins as long as it is really there and is not us.
     */
    fun effectiveTarget(context: Context, stored: String): String {
        val clean = stored.trim()
        if (clean.isNotEmpty() && clean != context.packageName && isInstalled(context, clean)) return clean
        val found = candidates(context)
        if (found.isNotEmpty()) return found.first()
        return ""
    }

    fun gameInstalled(context: Context, stored: String): Boolean =
        effectiveTarget(context, stored).isNotEmpty()

    fun storeSearchIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse("https://play.google.com/store/search?q=jaiho%20777&c=apps")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val defaultPackage: String get() = GAME_PACKAGE
}
