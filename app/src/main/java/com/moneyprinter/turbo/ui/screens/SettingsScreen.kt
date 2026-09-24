package com.moneyprinter.turbo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneyprinter.turbo.data.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onSaved: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var llmBaseUrl by remember { mutableStateOf("https://api.openai.com") }
    var llmApiKey by remember { mutableStateOf("") }
    var llmModel by remember { mutableStateOf("gpt-4o-mini") }
    var pexelsApiKey by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    // Load existing values
    LaunchedEffect(Unit) {
        settings.llmBaseUrl.collect { llmBaseUrl = it }
    }
    LaunchedEffect(Unit) {
        settings.llmApiKey.collect { llmApiKey = it }
    }
    LaunchedEffect(Unit) {
        settings.llmModel.collect { llmModel = it }
    }
    LaunchedEffect(Unit) {
        settings.pexelsApiKey.collect { pexelsApiKey = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pengaturan API", style = MaterialTheme.typography.headlineSmall)

        Text("LLM (OpenAI-compatible)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = llmBaseUrl,
            onValueChange = { llmBaseUrl = it },
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://api.openai.com") }
        )
        OutlinedTextField(
            value = llmApiKey,
            onValueChange = { llmApiKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = llmModel,
            onValueChange = { llmModel = it },
            label = { Text("Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("gpt-4o-mini / deepseek-chat / ...") }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Pexels (materi video)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = pexelsApiKey,
            onValueChange = { pexelsApiKey = it },
            label = { Text("Pexels API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(
            "Dapatkan gratis di https://www.pexels.com/api/",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    settings.saveLlm(llmBaseUrl.trim(), llmApiKey.trim(), llmModel.trim())
                    settings.savePexels(pexelsApiKey.trim())
                    savedMessage = "Tersimpan ✓"
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan")
        }

        savedMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Tips:\n" +
            "• DeepSeek: baseUrl = https://api.deepseek.com , model = deepseek-chat\n" +
            "• Groq: baseUrl = https://api.groq.com/openai , model = llama-3.1-70b-versatile\n" +
            "• Ollama lokal: baseUrl = http://192.168.x.x:11434 , model = llama3",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
