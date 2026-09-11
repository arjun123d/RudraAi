package com.arjun.rudra.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Speech-to-Text using Android's built-in SpeechRecognizer.
 *
 * Note on language: bn-BD / bn-IN gives best results for pure Bengali.
 * For Banglish/Hinglish mixed speech, on-device recognizers are inconsistent —
 * this is exactly where routing the raw utterance to a cloud AI model (see
 * AIManager) for intent understanding, rather than relying on exact STT
 * text matching, pays off.
 */
class SpeechManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onPartial: (String) -> Unit = {}
) {
    private var recognizer: SpeechRecognizer? = null

    fun startListening(languageTag: String = "bn-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Ei device e speech recognition available na.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty()
                    onResult(best)
                }

                override fun onPartialResults(partialResults: Bundle) {
                    val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let(onPartial)
                }

                override fun onError(error: Int) {
                    onError(errorText(error))
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NETWORK -> "Network problem hocche."
        SpeechRecognizer.ERROR_NO_MATCH -> "Kichu bujhte parlam na, abar bolo."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kono kotha shunte pelam na."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission nei."
        else -> "Recognition e ekta problem hoyeche."
    }
}
