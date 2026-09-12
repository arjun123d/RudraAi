package com.arjun.rudra.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TTSManager(
    context: Context,
    private val onSpeakStart: () -> Unit = {},
    private val onSpeakDone: () -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var hindiAvailable = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val hiResult = tts?.setLanguage(Locale("hi", "IN"))
                hindiAvailable = hiResult != TextToSpeech.LANG_MISSING_DATA &&
                    hiResult != TextToSpeech.LANG_NOT_SUPPORTED
                if (!hindiAvailable) {
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

    fun isHindiVoiceAvailable(): Boolean = hindiAvailable

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
