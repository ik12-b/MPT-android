package com.moneyprinter.turbo.service

import android.content.Context
import com.google.gson.Gson
import com.moneyprinter.turbo.data.local.AppDatabase
import com.moneyprinter.turbo.data.model.PipelineProgress
import com.moneyprinter.turbo.data.model.TaskEntity
import com.moneyprinter.turbo.data.model.TaskState
import com.moneyprinter.turbo.data.model.VideoParams
import com.moneyprinter.turbo.data.model.VideoSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

/**
 * Main orchestration service – direct mirror of task.py _run_pipeline.
 *
 * Stages:
 * 1. script
 * 2. terms
 * 3. audio
 * 4. subtitle
 * 5. materials
 * 6. video (final compose)
 */
class PipelineService(
    private val context: Context,
    private val llmService: LlmService,
    private val ttsService: TtsService,
    private val materialService: MaterialService,
    private val subtitleService: SubtitleService,
    private val videoComposer: VideoComposer,
    private val bgmService: BgmService = BgmService(context)
) {
    private val dao = AppDatabase.getInstance(context).taskDao()
    private val gson = Gson()
    private val workDir = File(context.filesDir, "pipeline").also { it.mkdirs() }

    private val _progress = MutableSharedFlow<PipelineProgress>(extraBufferCapacity = 16)
    val progress: SharedFlow<PipelineProgress> = _progress

    suspend fun run(params: VideoParams): Result<TaskEntity> {
        val task = TaskEntity(
            videoSubject = params.videoSubject,
            videoScript = params.videoScript,
            videoLanguage = params.videoLanguage,
            videoAspect = params.videoAspect.label,
            videoSource = params.videoSource.name,
            voiceName = params.voiceName,
            bgmType = params.bgmType.name,
            subtitleEnabled = params.subtitleEnabled,
            state = TaskState.PROCESSING.name,
            progress = 5
        )
        dao.insert(task)
        emit(task.id, "start", 5, "Memulai pipeline...")

        try {
            // 1. Script
            val script = if (params.videoScript.isNotBlank()) {
                params.videoScript
            } else {
                emit(task.id, "script", 10, "Membuat script dengan AI...")
                llmService.generateScript(
                    subject = params.videoSubject,
                    language = params.videoLanguage,
                    paragraphNumber = params.paragraphNumber,
                    customSystemPrompt = params.customSystemPrompt
                ).getOrElse {
                    return fail(task, "script", it.message ?: "Gagal generate script")
                }
            }
            update(task.copy(generatedScript = script, progress = 15))

            // 2. Terms
            emit(task.id, "terms", 20, "Mengekstrak keyword materi...")
            val terms = llmService.generateTerms(script).getOrElse {
                return fail(task, "terms", it.message ?: "Gagal generate terms")
            }
            update(task.copy(
                generatedScript = script,
                searchTerms = terms.joinToString(", "),
                progress = 25
            ))

            // 3. Audio (TTS)
            emit(task.id, "audio", 30, "Membuat voiceover...")
            val audioResult = ttsService.synthesizeToFile(
                text = script,
                outputDir = File(workDir, task.id),
                voiceRate = params.voiceRate
            ).getOrElse {
                return fail(task, "audio", it.message ?: "Gagal TTS")
            }
            val (audioPath, audioDurationMs) = audioResult
            update(task.copy(
                generatedScript = script,
                searchTerms = terms.joinToString(", "),
                audioPath = audioPath,
                audioDurationMs = audioDurationMs,
                progress = 40
            ))

            // 4. Subtitle
            var subtitlePath: String? = null
            if (params.subtitleEnabled) {
                emit(task.id, "subtitle", 45, "Membuat subtitle...")
                subtitlePath = subtitleService.generateSrt(
                    script = script,
                    totalDurationMs = audioDurationMs,
                    outputDir = File(workDir, task.id)
                )
                update(task.copy(
                    generatedScript = script,
                    searchTerms = terms.joinToString(", "),
                    audioPath = audioPath,
                    audioDurationMs = audioDurationMs,
                    subtitlePath = subtitlePath,
                    progress = 50
                ))
            }

            // 5. Materials
            emit(task.id, "materials", 55, "Mencari & mengunduh materi video...")
            if (params.videoSource != VideoSource.LOCAL) {
                val materials = materialService.searchAndDownload(
                    terms = terms,
                    aspect = params.videoAspect,
                    targetDurationMs = audioDurationMs + 2000,
                    maxClips = 12
                ).getOrElse {
                    return fail(task, "materials", it.message ?: "Gagal unduh materi")
                }
                update(task.copy(
                    generatedScript = script,
                    searchTerms = terms.joinToString(", "),
                    audioPath = audioPath,
                    audioDurationMs = audioDurationMs,
                    subtitlePath = subtitlePath,
                    materialPathsJson = gson.toJson(materials.map { it.localPath }),
                    progress = 70
                ))

                // 6. Final video (+ optional BGM)
                emit(task.id, "video", 75, "Merender video final...")
                val bgmPath = when (params.bgmType) {
                    com.moneyprinter.turbo.data.model.BgmType.NONE -> null
                    else -> bgmService.pickRandomBgm()
                }
                val finalPath = videoComposer.compose(
                    materials = materials,
                    voicePath = audioPath,
                    subtitlePath = subtitlePath,
                    aspect = params.videoAspect,
                    concatMode = params.videoConcatMode,
                    bgmPath = bgmPath,
                    bgmVolume = params.bgmVolume,
                    voiceVolume = params.voiceVolume
                ).getOrElse {
                    return fail(task, "video", it.message ?: "Gagal compose video")
                }

                val finished = task.copy(
                    generatedScript = script,
                    searchTerms = terms.joinToString(", "),
                    audioPath = audioPath,
                    audioDurationMs = audioDurationMs,
                    subtitlePath = subtitlePath,
                    materialPathsJson = gson.toJson(materials.map { it.localPath }),
                    finalVideoPath = finalPath,
                    state = TaskState.COMPLETE.name,
                    progress = 100,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(finished)
                emit(task.id, "video", 100, "Selesai!")
                return Result.success(finished)
            } else {
                return fail(task, "materials", "Local material source belum diimplementasi di Mode 2")
            }
        } catch (e: Exception) {
            return fail(task, "unknown", e.message ?: "Unknown error")
        }
    }

    private suspend fun update(task: TaskEntity) {
        dao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    private suspend fun fail(task: TaskEntity, stage: String, error: String): Result<TaskEntity> {
        val failed = task.copy(
            state = TaskState.FAILED.name,
            failedStage = stage,
            error = error,
            updatedAt = System.currentTimeMillis()
        )
        dao.update(failed)
        emit(task.id, stage, task.progress, "Gagal: $error")
        return Result.failure(Exception(error))
    }

    private suspend fun emit(taskId: String, stage: String, progress: Int, message: String) {
        _progress.emit(PipelineProgress(taskId, stage, progress, message))
    }
}
