package com.moneyprinter.turbo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Mirror of original VideoAspect, VideoConcatMode, etc.
 */
enum class VideoAspect(val label: String, val width: Int, val height: Int) {
    PORTRAIT("9:16", 1080, 1920),
    LANDSCAPE("16:9", 1920, 1080),
    SQUARE("1:1", 1080, 1080);

    companion object {
        fun fromLabel(label: String): VideoAspect =
            entries.find { it.label == label } ?: PORTRAIT
    }
}

enum class VideoConcatMode {
    RANDOM, SEQUENTIAL
}

enum class VideoSource {
    PEXELS, PIXABAY, LOCAL, OPENAI_IMAGE
}

enum class BgmType {
    RANDOM, NONE, LOCAL, AI
}

enum class TaskState {
    PENDING, PROCESSING, COMPLETE, FAILED
}

/**
 * Main parameters for video generation – mirrors original VideoParams.
 */
data class VideoParams(
    val videoSubject: String = "",
    val videoScript: String = "",                 // empty = AI generate
    val videoLanguage: String = "id",             // default Indonesian
    val videoAspect: VideoAspect = VideoAspect.PORTRAIT,
    val videoSource: VideoSource = VideoSource.PEXELS,
    val voiceName: String = "id-ID-ArdiNeural",   // Edge TTS style name
    val voiceVolume: Float = 1.0f,
    val voiceRate: Float = 1.1f,
    val bgmType: BgmType = BgmType.RANDOM,
    val bgmVolume: Float = 0.25f,
    val subtitleEnabled: Boolean = true,
    val subtitlePosition: String = "bottom",      // top, bottom, center
    val fontSize: Int = 48,
    val textForeColor: String = "#FFFFFF",
    val strokeColor: String = "#000000",
    val strokeWidth: Float = 2f,
    val paragraphNumber: Int = 1,
    val clipDuration: Float = 3.5f,               // average material duration
    val videoConcatMode: VideoConcatMode = VideoConcatMode.RANDOM,
    val customSystemPrompt: String = ""
)

/**
 * Task entity for Room + runtime state.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val state: String = TaskState.PENDING.name,
    val progress: Int = 0,
    val failedStage: String? = null,
    val error: String? = null,

    // Input
    val videoSubject: String = "",
    val videoScript: String = "",
    val videoLanguage: String = "id",
    val videoAspect: String = VideoAspect.PORTRAIT.label,
    val videoSource: String = VideoSource.PEXELS.name,
    val voiceName: String = "id-ID-ArdiNeural",
    val bgmType: String = BgmType.RANDOM.name,
    val subtitleEnabled: Boolean = true,

    // Intermediate results
    val generatedScript: String? = null,
    val searchTerms: String? = null,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0,
    val subtitlePath: String? = null,
    val materialPathsJson: String? = null,       // JSON list of local video paths

    // Final output
    val finalVideoPath: String? = null,
    val thumbnailPath: String? = null
)

data class MaterialInfo(
    val provider: String,
    val url: String,
    val localPath: String? = null,
    val durationMs: Long = 0
)

data class PipelineProgress(
    val taskId: String,
    val stage: String,           // script, terms, audio, subtitle, materials, video
    val progress: Int,           // 0-100
    val message: String = ""
)
