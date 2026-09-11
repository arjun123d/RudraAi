package com.arjun.rudra.manager

import android.content.Context
import android.content.Intent
import android.net.Uri

class AppLauncher(private val context: Context) {

    /** Returns true if the app was found and launched. */
    fun openApp(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun searchWeb(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Fallback to a plain browser search URL if no ACTION_WEB_SEARCH handler exists.
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun searchYouTube(query: String) {
        openUrl("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
