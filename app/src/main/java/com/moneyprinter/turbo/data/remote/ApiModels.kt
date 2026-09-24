package com.moneyprinter.turbo.data.remote

/**
 * Minimal OpenAI-compatible chat request/response.
 * Works with OpenAI, DeepSeek, Moonshot, Groq, Ollama (with openai compatible), etc.
 */
data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 2000
)

data class ChatChoice(
    val message: ChatMessage
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

/**
 * Pexels video search response (simplified).
 */
data class PexelsVideoResponse(
    val videos: List<PexelsVideo>
)

data class PexelsVideo(
    val id: Long,
    val duration: Int,              // seconds
    val image: String,
    val video_files: List<PexelsVideoFile>
)

data class PexelsVideoFile(
    val id: Long,
    val quality: String,            // hd, sd, uhd
    val file_type: String,
    val width: Int,
    val height: Int,
    val link: String
)
