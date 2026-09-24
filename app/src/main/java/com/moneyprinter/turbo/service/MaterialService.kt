package com.moneyprinter.turbo.service

import android.content.Context
import com.moneyprinter.turbo.data.model.MaterialInfo
import com.moneyprinter.turbo.data.model.VideoAspect
import com.moneyprinter.turbo.data.remote.ApiClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Downloads stock footage from Pexels (mirrors material.py).
 * Pixabay can be added later with the same pattern.
 */
class MaterialService(
    private val context: Context,
    private val pexelsApiKey: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File
        get() = File(context.cacheDir, "materials").also { it.mkdirs() }

    suspend fun searchAndDownload(
        terms: List<String>,
        aspect: VideoAspect,
        targetDurationMs: Long,
        maxClips: Int = 12
    ): Result<List<MaterialInfo>> = withContext(Dispatchers.IO) {
        try {
            val orientation = when (aspect) {
                VideoAspect.PORTRAIT -> "portrait"
                VideoAspect.LANDSCAPE -> "landscape"
                VideoAspect.SQUARE -> "square"
            }

            val allVideos = mutableListOf<MaterialInfo>()
            var remainingMs = targetDurationMs

            for (term in terms) {
                if (allVideos.size >= maxClips || remainingMs <= 0) break

                val response = ApiClients.pexels.searchVideos(
                    apiKey = pexelsApiKey,
                    query = term,
                    perPage = 8,
                    orientation = orientation
                )

                val candidates = response.videos
                    .filter { it.duration in 2..15 }
                    .sortedByDescending { it.duration }

                for (video in candidates) {
                    if (allVideos.size >= maxClips || remainingMs <= 0) break

                    // Prefer HD file closest to target resolution
                    val file = video.video_files
                        .filter { it.file_type.contains("mp4", ignoreCase = true) }
                        .sortedByDescending { it.width * it.height }
                        .firstOrNull() ?: continue

                    val local = downloadFile(file.link, "pexels_${video.id}.mp4")
                    if (local != null) {
                        val durationMs = video.duration * 1000L
                        allVideos.add(
                            MaterialInfo(
                                provider = "pexels",
                                url = file.link,
                                localPath = local.absolutePath,
                                durationMs = durationMs
                            )
                        )
                        remainingMs -= durationMs
                    }
                }
            }

            if (allVideos.isEmpty()) {
                Result.failure(Exception("No suitable materials found for terms: $terms"))
            } else {
                Result.success(allVideos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadFile(url: String, fileName: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val dest = File(cacheDir, fileName)
                if (dest.exists() && dest.length() > 10_000) return@withContext dest

                val request = Request.Builder().url(url).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (dest.exists() && dest.length() > 10_000) dest else null
            } catch (e: Exception) {
                null
            }
        }
}
