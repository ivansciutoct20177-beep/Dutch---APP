package com.dutchapp.learn.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Thin wrapper around Android's built-in Text-To-Speech engine, configured for
 * Dutch (nl-NL). Gives real pronunciation with no bundled audio files.
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile var muted: Boolean = false
    @Volatile var dutchSupported: Boolean = true
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("nl", "NL"))
                dutchSupported = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.9f)
                ready = true
            }
        }
    }

    fun speak(text: String) {
        val engine = tts ?: return
        if (!ready || muted || text.isBlank()) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nl_${text.hashCode()}")
    }

    fun speakSlow(text: String) {
        val engine = tts ?: return
        if (!ready || muted || text.isBlank()) return
        engine.setSpeechRate(0.6f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "slow_${text.hashCode()}")
        engine.setSpeechRate(0.9f)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

val LocalTts = compositionLocalOf<TtsManager?> { null }

@Composable
fun rememberTtsManager(): TtsManager {
    val context = LocalContext.current
    val manager = remember { TtsManager(context) }
    DisposableEffect(Unit) {
        onDispose { manager.shutdown() }
    }
    return manager
}
