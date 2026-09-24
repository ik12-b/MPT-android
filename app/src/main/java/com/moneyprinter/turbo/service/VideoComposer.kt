package com.moneyprinter.turbo.service

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.moneyprinter.turbo.data.model.MaterialInfo
import com.moneyprinter.turbo.data.model.VideoAspect
import com.moneyprinter.turbo.data.model.VideoConcatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Video composition using FFmpeg-kit.
 * Mirrors the core responsibilities of the original video.py:
 * - concatenate / randomise clips
 * - fit to target aspect
 * - overlay voice + optional BGM
 * - burn-in subtitles
 */
class VideoComposer(private val context: Context) {

    private val outputDir: File
        get() = File(context.getExternalFilesDir(null), "videos").also { it.mkdirs() }

    suspend fun compose(
        materials: List<MaterialInfo>,
        voicePath: String,
        subtitlePath: String?,
        aspect: VideoAspect,
        concatMode: VideoConcatMode,
        bgmPath: String? = null,
        bgmVolume: Float = 0.25f,
        voiceVolume: Float = 1.0f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (materials.isEmpty()) {
                return@withContext Result.failure(Exception("No materials to compose"))
            }

            val clips = when (concatMode) {
                VideoConcatMode.RANDOM -> materials.shuffled()
                VideoConcatMode.SEQUENTIAL -> materials
            }.mapNotNull { it.localPath }

            if (clips.isEmpty()) {
                return@withContext Result.failure(Exception("No local clip paths"))
            }

            val workDir = File(outputDir, "work_${UUID.randomUUID()}").also { it.mkdirs() }
            val listFile = File(workDir, "concat.txt")
            listFile.writeText(clips.joinToString("\n") { "file '${it.replace("'", "'\\''")}'" })

            val concatVideo = File(workDir, "concat.mp4")
            val scaleFilter = "scale=${aspect.width}:${aspect.height}:force_original_aspect_ratio=increase,crop=${aspect.width}:${aspect.height}"

            // 1. Concat + scale/crop
            val concatCmd = "-y -f concat -safe 0 -i \"${listFile.absolutePath}\" " +
                    "-vf \"$scaleFilter\" -c:v libx264 -preset veryfast -crf 23 -an \"${concatVideo.absolutePath}\""
            val concatSession = FFmpegKit.execute(concatCmd)
            if (!ReturnCode.isSuccess(concatSession.returnCode)) {
                return@withContext Result.failure(
                    Exception("Concat failed: ${concatSession.allLogsAsString.takeLast(500)}")
                )
            }

            // 2. Mix audio (voice + optional BGM)
            val mixedAudio = File(workDir, "mixed_audio.m4a")
            val audioCmd = if (bgmPath != null && File(bgmPath).exists()) {
                "-y -i \"$voicePath\" -i \"$bgmPath\" " +
                        "-filter_complex \"[0:a]volume=$voiceVolume[a0];[1:a]volume=$bgmVolume[a1];[a0][a1]amix=inputs=2:duration=first:dropout_transition=2[aout]\" " +
                        "-map \"[aout]\" -c:a aac -b:a 192k \"${mixedAudio.absolutePath}\""
            } else {
                "-y -i \"$voicePath\" -c:a aac -b:a 192k \"${mixedAudio.absolutePath}\""
            }
            val audioSession = FFmpegKit.execute(audioCmd)
            if (!ReturnCode.isSuccess(audioSession.returnCode)) {
                return@withContext Result.failure(
                    Exception("Audio mix failed: ${audioSession.allLogsAsString.takeLast(300)}")
                )
            }

            // 3. Combine video + audio + optional subtitle burn-in
            val finalFile = File(outputDir, "final_${System.currentTimeMillis()}.mp4")
            val subtitleFilter = if (subtitlePath != null && File(subtitlePath).exists()) {
                ",subtitles='${subtitlePath.replace("'", "'\\''")}':force_style='FontSize=24,PrimaryColour=&HFFFFFF&,OutlineColour=&H000000&,Outline=2'"
            } else ""

            val finalCmd = "-y -i \"${concatVideo.absolutePath}\" -i \"${mixedAudio.absolutePath}\" " +
                    "-c:v libx264 -preset veryfast -crf 23 " +
                    "-vf \"scale=${aspect.width}:${aspect.height}$subtitleFilter\" " +
                    "-c:a aac -b:a 192k -shortest \"${finalFile.absolutePath}\""

            val finalSession = FFmpegKit.execute(finalCmd)
            if (!ReturnCode.isSuccess(finalSession.returnCode)) {
                return@withContext Result.failure(
                    Exception("Final compose failed: ${finalSession.allLogsAsString.takeLast(500)}")
                )
            }

            // Cleanup work dir
            workDir.deleteRecursively()

            Result.success(finalFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
