package com.arjun.rudra.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
        Tum RUDRA ho, Arjun ke personal AI voice assistant. Casual, desi, dost jaise
        tone mein Hindi/Hinglish mein baat karo. Serious topic ho to serious aur
        respectful ho jao. Jab user koi action chahe (app kholo, call karo, delete
        karo etc), sirf ek clear, short response do — kya ho raha hai wo batao, extra
        explanation na do.
    """.trimIndent()

    suspend fun ask(userUtterance: String, conversationHistory: List<Pair<String, String>> = emptyList()): String =
        withContext(Dispatchers.IO) {
            val apiKey = ApiKeyProvider.get(context)
                ?: return@withContext "Arjun, AI API key configure nahi hui. Settings mein jaake use daal do."

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
                "Arjun, AI se connect nahi kar paya — internet ya API key check karo."
            }
        }
}
