package com.arjun.rudra.model

enum class Tool {
    OPEN_APP,
    SEARCH_WEB,
    SEARCH_YOUTUBE,
    PLAY_FIRST_YOUTUBE_VIDEO,
    CALL_CONTACT,
    CALL_RECENT_CONTACT,
    BATTERY_STATUS,
    GET_TIME,
    GET_DATE,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    VOLUME_UP,
    VOLUME_DOWN,
    LOCK_DEVICE,
    UNKNOWN
}

enum class RiskLevel { LOW, DESTRUCTIVE }

data class ParsedCommand(
    val tool: Tool,
    val argument: String? = null,
    val risk: RiskLevel = RiskLevel.LOW
)
