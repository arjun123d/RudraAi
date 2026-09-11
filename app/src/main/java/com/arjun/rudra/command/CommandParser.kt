package com.arjun.rudra.command

import com.arjun.rudra.model.ParsedCommand
import com.arjun.rudra.model.RiskLevel
import com.arjun.rudra.model.Tool

/**
 * Fast, offline, keyword-based parser for common Bengali/Banglish/Hindi/English phrasings.
 * This handles the majority of simple single-step commands without needing a network call.
 *
 * Anything it can't confidently classify comes back as Tool.UNKNOWN with the raw text as
 * argument — CommandRouter then hands that off to AIManager for real natural-language
 * intent resolution (needed for the multi-step / free-form cases from the spec).
 */
object CommandParser {

    private val appAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "camera" to "com.android.camera",
        "settings" to "com.android.settings",
        "whatsapp" to "com.whatsapp"
    )

    fun parse(rawUtterance: String): ParsedCommand {
        val text = rawUtterance.lowercase().trim()
        if (text.isEmpty()) return ParsedCommand(Tool.UNKNOWN)

        // "call <name>"
        callTriggers.firstOrNull { text.contains(it) }?.let { trigger ->
            val name = text.substringAfter(trigger).trim().removeSuffix("kor").removeSuffix("de").trim()
            if (text.contains("recent") || text.contains("last")) {
                return ParsedCommand(Tool.CALL_RECENT_CONTACT, name, RiskLevel.LOW)
            }
            return ParsedCommand(Tool.CALL_CONTACT, name.ifBlank { null }, RiskLevel.LOW)
        }

        // "open/kholo/chalao <app>"
        openTriggers.firstOrNull { text.contains(it) }?.let {
            appAliases.entries.firstOrNull { (alias, _) -> text.contains(alias) }?.let { (alias, pkg) ->
                return ParsedCommand(Tool.OPEN_APP, pkg, RiskLevel.LOW)
            }
        }

        if (text.contains("youtube") && searchTriggers.any { text.contains(it) }) {
            val query = extractSearchQuery(text, "youtube")
            return ParsedCommand(Tool.SEARCH_YOUTUBE, query, RiskLevel.LOW)
        }

        if (searchTriggers.any { text.contains(it) }) {
            val query = extractSearchQuery(text, null)
            return ParsedCommand(Tool.SEARCH_WEB, query, RiskLevel.LOW)
        }

        if (text.contains("battery")) return ParsedCommand(Tool.BATTERY_STATUS)
        if (text.contains("time") || text.contains("shomoy")) return ParsedCommand(Tool.GET_TIME)
        if (text.contains("date") || text.contains("tarikh")) return ParsedCommand(Tool.GET_DATE)

        if (text.contains("flashlight") || text.contains("torch")) {
            return if (text.contains("off") || text.contains("bondho")) {
                ParsedCommand(Tool.FLASHLIGHT_OFF)
            } else {
                ParsedCommand(Tool.FLASHLIGHT_ON)
            }
        }

        if (text.contains("volume")) {
            return if (text.contains("kome") || text.contains("down") || text.contains("komao")) {
                ParsedCommand(Tool.VOLUME_DOWN)
            } else {
                ParsedCommand(Tool.VOLUME_UP)
            }
        }

        if (text.contains("lock")) return ParsedCommand(Tool.LOCK_DEVICE, risk = RiskLevel.DESTRUCTIVE)

        return ParsedCommand(Tool.UNKNOWN, rawUtterance)
    }

    private fun extractSearchQuery(text: String, platform: String?): String {
        var q = text
        searchTriggers.forEach { q = q.replace(it, "") }
        if (platform != null) q = q.replace(platform, "")
        return q.trim()
    }

    private val callTriggers = listOf("call kor", "call de", "phone kor", "call my", "call ")
    private val openTriggers = listOf("kholo", "khol", "open", "chalao", "chala")
    private val searchTriggers = listOf("search kor", "search de", "search")
}
