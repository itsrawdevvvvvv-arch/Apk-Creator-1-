package com.rawedit.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class StylePackExporter(
    private val context: Context,
    private val cacheManager: CacheManager
) {
    private val assetEntries = listOf(
        "SKILL.md",
        "README.md",
        "docs/STYLE_BIBLE.md",
        "docs/BEAT_SHEETS.md",
        "style/tokens.css",
        "style/components.css",
        "engine/engine.js",
        "template/index.html",
        "template/scenes.js",
        "tools/render.py"
    )

    suspend fun buildStylePackZip(): File = withContext(Dispatchers.IO) {
        val outFile = File(context.cacheDir, "kinetic-explainer-style.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zos ->
            for (relPath in assetEntries) {
                val assetPath = "style-pack/$relPath"
                zos.putNextEntry(ZipEntry("kinetic-explainer-style/$relPath"))
                context.assets.open(assetPath).use { input ->
                    input.copyTo(zos, bufferSize = 16 * 1024)
                }
                zos.closeEntry()
            }
        }
        outFile
    }

    suspend fun exportToDownloads(safFallbackUri: Uri? = null): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val zipFile = buildStylePackZip()
            cacheManager.exportFileToDownloads(
                sourceFile = zipFile,
                displayFileName = "kinetic-explainer-style.zip",
                mimeType = "application/zip",
                fallbackSafUri = safFallbackUri
            ).getOrThrow()
        }
    }

    suspend fun getShareableStylePackUri(): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val zipFile = buildStylePackZip()
            cacheManager.getShareableUri(zipFile)
        }
    }
}
