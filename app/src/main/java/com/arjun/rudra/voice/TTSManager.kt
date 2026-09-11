package com.arjun.rudra.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Text-to-Speech for RUDRA's replies.
 * Falls back to English locale if Bengali TTS voice isn't installed on the device
 * (many Android phones ship without a Bengali TTS voice by default — RUDRA should
 * tell the user this rather than silently going quiet).
 */
class TTSManager(
    context: Context,
    private val onSpeakStart: () -> Unit = {},
    private val onSpeakDone: () -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var bengaliAvailable = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val bnResult = tts?.setLanguage(Locale("bn", "IN"))
                bengaliAvailable = bnResult != TextToSpeech.LANG_MISSING_DATA &&
                    bnResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!bengaliAvailable) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = onSpeakStart()
                    override fun onDone(utteranceId: String?) = onSpeakDone()
                    override fun onError(utteranceId: String?) = onSpeakDone()
                })
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rudra_utt_${System.currentTimeMillis()}")
    }

    fun isBengaliVoiceAvailable(): Boolean = bengaliAvailable

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
