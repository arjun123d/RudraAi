package com.arjun.rudra.manager

import android.content.Context
import org.json.JSONObject

/**
 * Local-only memory for RUDRA.
 *
 * Stores:
 *  - relationship -> contact lookup key mapping ("wife" -> "Priya Sharma")
 *  - the user's preferred name (default "Arjun")
 *  - non-secret AI endpoint config (the actual API key is NOT stored here,
 *    see SettingsManager / secure config note below)
 *
 * Deliberately never stores: passwords, PINs, OTPs, banking info, or any
 * authentication secret. This class is not a place to add those — if you
 * find yourself tempted to, stop and use Android Keystore instead, and
 * reconsider whether RUDRA needs it at all.
 */
class MemoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("rudra_memory", Context.MODE_PRIVATE)

    var preferredUserName: String
        get() = prefs.getString(KEY_USER_NAME, "Arjun") ?: "Arjun"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    /** relationship label (lowercase, e.g. "wife", "mom") -> raw contact lookup string */
    fun setContactLabel(label: String, contactLookupKey: String) {
        val map = getContactLabelsRaw()
        map.put(label.lowercase().trim(), contactLookupKey)
        prefs.edit().putString(KEY_CONTACT_LABELS, map.toString()).apply()
    }

    fun getContactForLabel(label: String): String? {
        return getContactLabelsRaw().optString(label.lowercase().trim(), null)
    }

    fun allContactLabels(): Map<String, String> {
        val json = getContactLabelsRaw()
        return json.keys().asSequence().associateWith { json.getString(it) }
    }

    private fun getContactLabelsRaw(): JSONObject {
        val raw = prefs.getString(KEY_CONTACT_LABELS, "{}") ?: "{}"
        return try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
    }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_CONTACT_LABELS = "contact_labels"
    }
}
