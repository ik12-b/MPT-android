package com.moneyprinter.turbo.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Local TTS using Android TextToSpeech.
 * For higher quality (Edge TTS style) you can later add network Edge TTS or Azure.
 * This mirrors the "generate_audio" stage of the original pipeline.
 */
class TtsService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    suspend fun init(): Boolean = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            tts = TextToSpeech(context) { status ->
                isReady = status == TextToSpeech.SUCCESS
                if (isReady) {
                    // Prefer Indonesian if available
                    val result = tts?.setLanguage(Locale("id", "ID"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }
                cont.resume(isReady)
            }
        }
    }

    /**
     * Synthesize script to a local WAV/MP3 file.
     * Returns path + approximate duration in ms.
     */
    suspend fun synthesizeToFile(
        text: String,
        outputDir: File,
        voiceRate: Float = 1.1f,
        voicePitch: Float = 1.0f
    ): Result<Pair<String, Long>> = withContext(Dispatchers.IO) {
        if (!isReady || tts == null) {
            val ok = init()
            if (!ok) return@withContext Result.failure(Exception("TTS engine not available"))
        }

        val outFile = File(outputDir, "voice_${UUID.randomUUID()}.wav")
        outputDir.mkdirs()

        try {
            tts?.setSpeechRate(voiceRate.coerceIn(0.5f, 2.0f))
            tts?.setPitch(voicePitch.coerceIn(0.5f, 2.0f))

            val utteranceId = UUID.randomUUID().toString()

            suspendCancellableCoroutine { cont ->
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        // Rough duration estimate: ~12 chars/sec at rate 1.0
                        val estimatedMs = ((text.length / 12.0) / voiceRate * 1000).toLong()
                        cont.resume(Result.success(outFile.absolutePath to estimatedMs))
                    }
                    override fun onError(utteranceId: String?) {
                        cont.resume(Result.failure(Exception("TTS synthesis error")))
                    }
                })

                val result = tts?.synthesizeToFile(
                    text,
                    null,
                    outFile,
                    utteranceId
                )
                if (result != TextToSpeech.SUCCESS) {
                    cont.resume(Result.failure(Exception("synthesizeToFile failed")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
