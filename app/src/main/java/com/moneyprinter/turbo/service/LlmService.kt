package com.moneyprinter.turbo.service

import com.moneyprinter.turbo.data.remote.ApiClients
import com.moneyprinter.turbo.data.remote.ChatMessage
import com.moneyprinter.turbo.data.remote.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LLM service that mirrors original llm.py behaviour.
 * Supports any OpenAI-compatible endpoint (OpenAI, DeepSeek, Moonshot, Groq, Ollama, etc.)
 */
class LlmService(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) {
    private val api = ApiClients.openAiCompatible(baseUrl)

    private val defaultScriptSystemPrompt = """
# Role: Video Script Generator

## Goals:
Generate a script for a video, depending on the subject of the video.

## Constraints:
1. the script is to be returned as a string with the specified number of paragraphs.
2. do not under any circumstance reference this prompt in your response.
3. get straight to the point, don't start with unnecessary things like, "welcome to this video".
4. you must not include any type of markdown or formatting in the script, never use a title.
5. only return the raw content of the script.
6. do not include "voiceover", "narrator" or similar indicators of what should be spoken at the beginning of each paragraph or line.
7. you must not mention the prompt, or anything about the script itself. also, never talk about the amount of paragraphs or lines. just write the script.
8. respond in the same language as the video subject.
""".trimIndent()

    private val termsSystemPrompt = """
You are a video material search expert.
Given a video script, extract 5-8 short English search keywords that best represent the visual content.
Return only the keywords separated by commas. No explanation.
""".trimIndent()

    suspend fun generateScript(
        subject: String,
        language: String = "id",
        paragraphNumber: Int = 1,
        customSystemPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val system = customSystemPrompt.ifBlank { defaultScriptSystemPrompt }
            val user = buildString {
                append("Video subject: $subject\n")
                append("Language: $language\n")
                append("Number of paragraphs: $paragraphNumber\n")
                append("Write the complete narration script now.")
            }

            val response = api.chatCompletions(
                authorization = "Bearer $apiKey",
                body = ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", system),
                        ChatMessage("user", user)
                    ),
                    temperature = 0.8f
                )
            )
            val content = response.choices.firstOrNull()?.message?.content?.trim()
            if (content.isNullOrBlank()) {
                Result.failure(Exception("Empty script from LLM"))
            } else {
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTerms(script: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.chatCompletions(
                authorization = "Bearer $apiKey",
                body = ChatRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", termsSystemPrompt),
                        ChatMessage("user", script)
                    ),
                    temperature = 0.5f,
                    max_tokens = 200
                )
            )
            val content = response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            val terms = content.split(",", "\n")
                .map { it.trim().removePrefix("-").trim() }
                .filter { it.isNotBlank() && it.length > 2 }
                .take(8)
            if (terms.isEmpty()) {
                Result.failure(Exception("No search terms generated"))
            } else {
                Result.success(terms)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
