package com.moneyprinter.turbo.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.moneyprinter.turbo.data.SettingsRepository
import com.moneyprinter.turbo.data.model.VideoAspect
import com.moneyprinter.turbo.data.model.VideoParams
import com.moneyprinter.turbo.data.model.VideoSource
import com.moneyprinter.turbo.service.*
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker so generation can continue in background.
 * Currently optional – MainViewModel still runs in process for UI feedback.
 * You can enqueue this from ViewModel if you want true background jobs.
 */
class VideoGenerationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val subject = inputData.getString(KEY_SUBJECT) ?: return Result.failure()
        val language = inputData.getString(KEY_LANGUAGE) ?: "id"
        val aspectLabel = inputData.getString(KEY_ASPECT) ?: "9:16"

        val settings = SettingsRepository(applicationContext)
        val llmKey = settings.llmApiKey.first()
        val llmUrl = settings.llmBaseUrl.first()
        val llmModel = settings.llmModel.first()
        val pexelsKey = settings.pexelsApiKey.first()

        if (llmKey.isBlank() || pexelsKey.isBlank()) {
            return Result.failure(workDataOf(KEY_ERROR to "API key missing"))
        }

        val params = VideoParams(
            videoSubject = subject,
            videoLanguage = language,
            videoAspect = VideoAspect.fromLabel(aspectLabel),
            videoSource = VideoSource.PEXELS
        )

        val llm = LlmService(llmUrl, llmKey, llmModel)
        val tts = TtsService(applicationContext)
        tts.init()
        val material = MaterialService(applicationContext, pexelsKey)
        val subtitle = SubtitleService()
        val composer = VideoComposer(applicationContext)
        val bgm = BgmService(applicationContext)

        val pipeline = PipelineService(
            context = applicationContext,
            llmService = llm,
            ttsService = tts,
            materialService = material,
            subtitleService = subtitle,
            videoComposer = composer,
            bgmService = bgm
        )

        val result = pipeline.run(params)
        tts.shutdown()

        return if (result.isSuccess) {
            Result.success(workDataOf(KEY_VIDEO_PATH to result.getOrNull()?.finalVideoPath))
        } else {
            Result.failure(workDataOf(KEY_ERROR to (result.exceptionOrNull()?.message ?: "unknown")))
        }
    }

    companion object {
        const val KEY_SUBJECT = "subject"
        const val KEY_LANGUAGE = "language"
        const val KEY_ASPECT = "aspect"
        const val KEY_VIDEO_PATH = "video_path"
        const val KEY_ERROR = "error"
    }
}
