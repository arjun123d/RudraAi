package com.arjun.rudra.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles free-form conversation + intent understanding for anything
 * CommandParser's keyword matching can't confidently classify (multi-step
 * commands, casual chat, ambiguous phrasing).
 *
 * Using Groq's free API (OpenAI-compatible format) with the Llama 3.3 model.
 */
object ApiKeyProvider {
    fun get(context: Context): String? {
        val prefs = context.getSharedPreferences("rudra_secure_config", Context.MODE_PRIVATE)
        return prefs.getString("ai_api_key", null)
    }

    fun set(context: Context, key: String) {
        context.getSharedPreferences("rudra_secure_config", Context.MODE_PRIVATE)
            .edit().putString("ai_api_key", key).apply()
    }
}

class AIManager(private val context: Context) {

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    private val systemPrompt = """
        Tumi RUDRA, Arjun-er personal AI voice assistant. Casual, desi, bondhu-shulov
        tone e Bengali/Banglish e kotha bolo. Serious bishoy hole serious ar respectful
        hoye jao. Jokhon user kono action chay (app kholo, call koro, delete koro etc),
        shudhu ekta clear, short response dao — ki korte hocche seta bolo, extra explanation
        na diye.
    """.trimIndent()

    suspend fun ask(userUtterance: String, conversationHistory: List<Pair<String, String>> = emptyList()): String =
        withContext(Dispatchers.IO) {
            val apiKey = ApiKeyProvider.get(context)
                ?: return@withContext "Arjun, AI API key ta configure kora hoyni. Settings e giye seta boshiye dao."

            try {
                val messages = JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    conversationHistory.forEach { (role, content) ->
                        put(JSONObject().put("role", role).put("content", content))
                    }
                    put(JSONObject().put("role", "user").put("content", userUtterance))
                }
                val body = JSONObject().apply {
                    put("model", "openai/gpt-oss-120b")
                    put("messages", messages)
                    put("max_tokens", 300)
                }

                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 15_000
                }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val responseText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(responseText)
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } catch (e: Exception) {
                "Arjun, AI-r shathe connect korte parlam na — internet ba API key check koro."
            }
        }
}
