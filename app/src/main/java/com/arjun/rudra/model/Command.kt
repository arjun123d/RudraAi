package com.arjun.rudra.model

/** Modular tool set. Add new entries here + a handler in CommandRouter to extend RUDRA. */
enum class Tool {
    OPEN_APP,
    SEARCH_WEB,
    SEARCH_YOUTUBE,
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

/** Risk level decides whether CommandRouter must ask for confirmation before acting. */
enum class RiskLevel { LOW, DESTRUCTIVE }

data class ParsedCommand(
    val tool: Tool,
    val argument: String? = null,
    val risk: RiskLevel = RiskLevel.LOW
)
