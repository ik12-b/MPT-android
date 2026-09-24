package com.moneyprinter.turbo.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneyprinter.turbo.data.SettingsRepository
import com.moneyprinter.turbo.data.model.VideoParams
import com.moneyprinter.turbo.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val isGenerating: Boolean = false,
    val progressMessage: String = "",
    val lastError: String? = null,
    val lastVideoPath: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun startGeneration(params: VideoParams) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, progressMessage = "Menyiapkan...", lastError = null, lastVideoPath = null) }

            val llmKey = settings.llmApiKey.first()
            val llmUrl = settings.llmBaseUrl.first()
            val llmModel = settings.llmModel.first()
            val pexelsKey = settings.pexelsApiKey.first()

            if (llmKey.isBlank()) {
                _uiState.update {
                    it.copy(isGenerating = false, lastError = "LLM API Key belum diisi di Pengaturan")
                }
                return@launch
            }
            if (pexelsKey.isBlank()) {
                _uiState.update {
                    it.copy(isGenerating = false, lastError = "Pexels API Key belum diisi di Pengaturan")
                }
                return@launch
            }

            val llm = LlmService(llmUrl, llmKey, llmModel)
            val tts = TtsService(getApplication())
            tts.init()
            val material = MaterialService(getApplication(), pexelsKey)
            val subtitle = SubtitleService()
            val composer = VideoComposer(getApplication())
            val bgm = BgmService(getApplication())

            val pipeline = PipelineService(
                context = getApplication(),
                llmService = llm,
                ttsService = tts,
                materialService = material,
                subtitleService = subtitle,
                videoComposer = composer,
                bgmService = bgm
            )

            launch {
                pipeline.progress.collect { p ->
                    _uiState.update {
                        it.copy(progressMessage = "${p.stage}: ${p.message} (${p.progress}%)")
                    }
                }
            }

            val result = pipeline.run(params)
            result.onSuccess { task ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        progressMessage = "Selesai!",
                        lastVideoPath = task.finalVideoPath
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        lastError = e.message,
                        progressMessage = "Gagal: ${e.message}"
                    )
                }
            }

            tts.shutdown()
        }
    }
}
