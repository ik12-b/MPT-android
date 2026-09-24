package com.moneyprinter.turbo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyprinter.turbo.data.model.BgmType
import com.moneyprinter.turbo.data.model.VideoAspect
import com.moneyprinter.turbo.data.model.VideoParams
import com.moneyprinter.turbo.data.model.VideoSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onStartGeneration: (VideoParams) -> Unit,
    isGenerating: Boolean,
    progressMessage: String,
    lastError: String? = null,
    lastVideoPath: String? = null
) {
    var subject by remember { mutableStateOf("") }
    var customScript by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("id") }
    var aspect by remember { mutableStateOf(VideoAspect.PORTRAIT) }
    var useCustomScript by remember { mutableStateOf(false) }
    var enableBgm by remember { mutableStateOf(true) }
    var enableSubtitle by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Buat Video Baru", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Topik / Keyword") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating,
            singleLine = true
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = useCustomScript,
                onCheckedChange = { useCustomScript = it },
                enabled = !isGenerating
            )
            Text("Gunakan script sendiri")
        }

        if (useCustomScript) {
            OutlinedTextField(
                value = customScript,
                onValueChange = { customScript = it },
                label = { Text("Script narasi") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                enabled = !isGenerating
            )
        }

        Text("Aspect Ratio", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoAspect.entries.forEach { a ->
                FilterChip(
                    selected = aspect == a,
                    onClick = { aspect = a },
                    label = { Text(a.label) },
                    enabled = !isGenerating
                )
            }
        }

        OutlinedTextField(
            value = language,
            onValueChange = { language = it },
            label = { Text("Bahasa (id / en / zh ...)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating,
            singleLine = true
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = enableSubtitle,
                onCheckedChange = { enableSubtitle = it },
                enabled = !isGenerating
            )
            Text("Subtitle")
            Spacer(Modifier.width(16.dp))
            Checkbox(
                checked = enableBgm,
                onCheckedChange = { enableBgm = it },
                enabled = !isGenerating
            )
            Text("Background Music")
        }

        if (isGenerating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(progressMessage, style = MaterialTheme.typography.bodyMedium)
        }

        lastError?.let {
            Text(
                "Error: $it",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        lastVideoPath?.let {
            Text(
                "Video selesai: $it",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                val params = VideoParams(
                    videoSubject = subject.trim(),
                    videoScript = if (useCustomScript) customScript.trim() else "",
                    videoLanguage = language.trim().ifBlank { "id" },
                    videoAspect = aspect,
                    videoSource = VideoSource.PEXELS,
                    bgmType = if (enableBgm) BgmType.RANDOM else BgmType.NONE,
                    subtitleEnabled = enableSubtitle
                )
                onStartGeneration(params)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGenerating && subject.isNotBlank()
        ) {
            Text(if (isGenerating) "Sedang generate..." else "Generate Video")
        }
    }
}
