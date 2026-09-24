package com.moneyprinter.turbo.service

import java.io.File
import java.util.UUID

/**
 * Simple subtitle generator (mirrors subtitle.py at a basic level).
 * Creates an SRT file by splitting the script into sentences and
 * distributing time proportionally to character count.
 *
 * For word-by-word / advanced timing you would integrate a forced aligner
 * or use the timestamps returned by some TTS engines.
 */
class SubtitleService {

    fun generateSrt(
        script: String,
        totalDurationMs: Long,
        outputDir: File
    ): String {
        outputDir.mkdirs()
        val outFile = File(outputDir, "subtitle_${UUID.randomUUID()}.srt")

        val sentences = script
            .split(Regex("(?<=[.!?。！？])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            outFile.writeText("")
            return outFile.absolutePath
        }

        val totalChars = sentences.sumOf { it.length }.coerceAtLeast(1)
        var currentMs = 0L
        val sb = StringBuilder()

        sentences.forEachIndexed { index, sentence ->
            val portion = sentence.length.toDouble() / totalChars
            val duration = (portion * totalDurationMs).toLong().coerceAtLeast(800)
            val start = currentMs
            val end = (currentMs + duration).coerceAtMost(totalDurationMs)
            currentMs = end

            sb.appendLine(index + 1)
            sb.appendLine("${formatTime(start)} --> ${formatTime(end)}")
            sb.appendLine(sentence)
            sb.appendLine()
        }

        outFile.writeText(sb.toString())
        return outFile.absolutePath
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        val millis = ms % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
