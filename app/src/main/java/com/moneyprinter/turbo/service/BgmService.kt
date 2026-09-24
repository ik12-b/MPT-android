package com.moneyprinter.turbo.service

import android.content.Context
import java.io.File

/**
 * Simple BGM helper.
 * Put .mp3 / .m4a files into assets/bgm/ or app external files/bgm/
 * and this service will pick a random one.
 */
class BgmService(private val context: Context) {

    private val bgmDir: File
        get() = File(context.getExternalFilesDir(null), "bgm").also { it.mkdirs() }

    /**
     * Returns a random local BGM path, or null if none available.
     * User can copy music files into Android/data/<package>/files/bgm/
     */
    fun pickRandomBgm(): String? {
        val files = bgmDir.listFiles { f ->
            f.isFile && (f.extension.equals("mp3", true) ||
                    f.extension.equals("m4a", true) ||
                    f.extension.equals("aac", true))
        } ?: emptyArray()

        if (files.isEmpty()) return null
        return files.random().absolutePath
    }

    fun listAvailable(): List<String> {
        return bgmDir.listFiles()?.map { it.name } ?: emptyList()
    }
}
