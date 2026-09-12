package com.arjun.rudra.command

import com.arjun.rudra.model.ParsedCommand
import com.arjun.rudra.model.RiskLevel
import com.arjun.rudra.model.Tool

object CommandParser {

    private val appAliases = mapOf(
        "youtube" to "com.google.android.youtube",
        "यूट्यूब" to "com.google.android.youtube",
        "युटुब" to "com.google.android.youtube",
        "यूटुब" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "इंस्टाग्राम" to "com.instagram.android",
        "इंस्टा" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "क्रोम" to "com.android.chrome",
        "ब्राउज़र" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "मैप" to "com.google.android.apps.maps",
        "camera" to "com.android.camera",
        "कैमरा" to "com.android.camera",
        "settings" to "com.android.settings",
        "सेटिंग्स" to "com.android.settings",
        "whatsapp" to "com.whatsapp",
        "व्हाट्सएप" to "com.whatsapp"
    )

    fun parse(rawUtterance: String): ParsedCommand {
        val text = rawUtterance.lowercase().trim()
        if (text.isEmpty()) return ParsedCommand(Tool.UNKNOWN)

        callTriggers.firstOrNull { text.contains(it) }?.let { trigger ->
            val name = text.substringAfter(trigger).trim()
                .removeSuffix("karo").removeSuffix("kar do").removeSuffix("को").trim()
            if (text.contains("recent") || text.contains("last") || text.contains("पिछला")) {
                return ParsedCommand(Tool.CALL_RECENT_CONTACT, name, RiskLevel.LOW)
            }
            return ParsedCommand(Tool.CALL_CONTACT, name.ifBlank { null }, RiskLevel.LOW)
        }

        val hasOpenTrigger = openTriggers.any { text.contains(it) }
        if (hasOpenTrigger) {
            appAliases.entries.firstOrNull { (alias, _) -> text.contains(alias) }?.let { (_, pkg) ->
                return ParsedCommand(Tool.OPEN_APP, pkg, RiskLevel.LOW)
            }
        }

        val youtubeWords = listOf("youtube", "यूट्यूब", "युटुब", "यूटुब")
        val hasYoutube = youtubeWords.any { text.contains(it) }
        if (hasYoutube && searchTriggers.any { text.contains(it) }) {
            val query = extractSearchQuery(text, youtubeWords)
            return ParsedCommand(Tool.SEARCH_YOUTUBE, query, RiskLevel.LOW)
        }

        if (searchTriggers.any { text.contains(it) }) {
            val query = extractSearchQuery(text, emptyList())
            return ParsedCommand(Tool.SEARCH_WEB, query, RiskLevel.LOW)
        }

        if (text.contains("battery") || text.contains("बैटरी")) return ParsedCommand(Tool.BATTERY_STATUS)
        if (text.contains("time") || text.contains("samay") || text.contains("समय")) return ParsedCommand(Tool.GET_TIME)
        if (text.contains("date") || text.contains("तारीख")) return ParsedCommand(Tool.GET_DATE)

        if (text.contains("flashlight") || text.contains("torch") || text.contains("टॉर्च") || text.contains("फ्लैश")) {
            return if (text.contains("off") || text.contains("band") || text.contains("बंद")) {
                ParsedCommand(Tool.FLASHLIGHT_OFF)
            } else {
                ParsedCommand(Tool.FLASHLIGHT_ON)
            }
        }

        if (text.contains("volume") || text.contains("आवाज़") || text.contains("वॉल्यूम")) {
            return if (text.contains("kam") || text.contains("down") || text.contains("कम")) {
                ParsedCommand(Tool.VOLUME_DOWN)
            } else {
                ParsedCommand(Tool.VOLUME_UP)
            }
        }

        if (text.contains("lock") || text.contains("लॉक")) return ParsedCommand(Tool.LOCK_DEVICE, risk = RiskLevel.DESTRUCTIVE)

        return ParsedCommand(Tool.UNKNOWN, rawUtterance)
    }

    private fun extractSearchQuery(text: String, platformWords: List<String>): String {
        var q = text
        searchTriggers.forEach { q = q.replace(it, "") }
        platformWords.forEach { q = q.replace(it, "") }
        return q.trim()
    }

    private val callTriggers = listOf("call kar", "call de", "phone kar", "call my", "call ", "कॉल कर", "फोन कर", "को कॉल")
    private val openTriggers = listOf("kholo", "khol", "open", "chalao", "chala", "खोलो", "खोल", "चलाओ", "ओपन", "ओपेन")
    private val searchTriggers = listOf("search kar", "search de", "search", "सर्च कर", "सर्च")
}
