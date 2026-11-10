package com.rawedit.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

data class ImportedAudioInfo(
    val localFile: File,
    val localUri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val displayName: String
)

class CacheManager(private val context: Context) {

    private val overflowDir = File(context.filesDir, "overflow_blobs").apply { mkdirs() }
    private val mediaDir = File(context.filesDir, "imported_media").apply { mkdirs() }
    private val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }
    private val sheetsDir = File(context.filesDir, "keyframe_sheets").apply { mkdirs() }
    private val tempRenderDir = File(context.cacheDir, "render_temp").apply { mkdirs() }

    companion object {
        const val MAX_INLINE_CHARS = 90_000 // < 100 KB CursorWindow safety on Android 9+
        const val MAX_AUDIO_BYTES = 100L * 1024L * 1024L // 100 MB
        const val MIN_AUDIO_DURATION_MS = 1_000L
        const val MAX_AUDIO_DURATION_MS = 20L * 60L * 1_000L // 20 minutes
        const val MAX_SRT_BYTES = 4L * 1024L * 1024L // 4 MB
        private const val OVERFLOW_PREFIX = "overflow://"
    }

    suspend fun cleanupStaleTempFiles() = withContext(Dispatchers.IO) {
        runCatching {
            val cutoff = System.currentTimeMillis() - 12L * 3600L * 1000L
            tempRenderDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) {
                    file.deleteRecursively()
                }
            }
        }
    }

    fun getSheetsDirForProject(projectId: String): File {
        return File(sheetsDir, projectId).apply { mkdirs() }
    }

    fun getTempRenderDirForJob(jobId: String): File {
        return File(tempRenderDir, jobId).apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
    }

    suspend fun persistTextWithOverflow(projectId: String, kind: String, rawText: String): String =
        withContext(Dispatchers.IO) {
            if (rawText.length <= MAX_INLINE_CHARS) {
                return@withContext rawText
            }
            val file = File(overflowDir, "${projectId}_${kind}.txt")
            file.writeText(rawText, Charsets.UTF_8)
            "$OVERFLOW_PREFIX${file.absolutePath}"
        }

    suspend fun resolveTextWithOverflow(storedValue: String): String =
        withContext(Dispatchers.IO) {
            if (!storedValue.startsWith(OVERFLOW_PREFIX)) {
                return@withContext storedValue
            }
            val path = storedValue.removePrefix(OVERFLOW_PREFIX)
            val file = File(path)
            if (file.exists() && file.canRead()) {
                file.readText(Charsets.UTF_8)
            } else {
                ""
            }
        }

    fun computeSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    suspend fun importAudioFile(projectId: String, sourceUri: Uri): Result<ImportedAudioInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                var displayName = "voiceover.m4a"
                var reportedSize = -1L

                resolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) reportedSize = cursor.getLong(sizeIdx)
                    }
                }

                if (reportedSize > MAX_AUDIO_BYTES) {
                    throw IllegalArgumentException("Audio exceeds 100 MB limit (${reportedSize / (1024 * 1024)} MB).")
                }

                val ext = displayName.substringAfterLast('.', "m4a").lowercase()
                val allowedExts = setOf("mp3", "m4a", "wav", "aac", "ogg", "mp4")
                val safeExt = if (ext in allowedExts) ext else "m4a"

                val destFile = File(mediaDir, "audio_${projectId}.$safeExt")
                var copiedBytes = 0L
                resolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            copiedBytes += read
                            if (copiedBytes > MAX_AUDIO_BYTES) {
                                destFile.delete()
                                throw IllegalArgumentException("Audio file exceeds 100 MB limit.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                } ?: throw IllegalArgumentException("Unable to read selected audio file.")

                val retriever = MediaMetadataRetriever()
                val durationMs = try {
                    retriever.setDataSource(destFile.absolutePath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        ?: 6000L
                } finally {
                    runCatching { retriever.release() }
                }

                if (durationMs < MIN_AUDIO_DURATION_MS || durationMs > MAX_AUDIO_DURATION_MS) {
                    destFile.delete()
                    throw IllegalArgumentException("Audio duration must be between 1 second and 20 minutes.")
                }

                ImportedAudioInfo(
                    localFile = destFile,
                    localUri = Uri.fromFile(destFile).toString(),
                    durationMs = durationMs,
                    sizeBytes = copiedBytes,
                    displayName = displayName
                )
            }
        }

    suspend fun readSrtUriCapped(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            resolver.openInputStream(sourceUri)?.use { input ->
                val buffer = ByteArray(16 * 1024)
                val out = java.io.ByteArrayOutputStream()
                var total = 0L
                while (true) {
                    val r = input.read(buffer)
                    if (r <= 0) break
                    total += r
                    if (total > MAX_SRT_BYTES) {
                        throw IllegalArgumentException("SRT file exceeds 4 MB safety limit.")
                    }
                    out.write(buffer, 0, r)
                }
                out.toString(Charsets.UTF_8.name())
            } ?: throw IllegalArgumentException("Unable to open SRT file.")
        }
    }

    suspend fun readHtmlOrTextUri(sourceUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: throw IllegalArgumentException("Unable to read file stream.")
        }
    }

    suspend fun importAndCropAvatar512(sourceUri: Uri, zoom: Float = 1f, panX: Float = 0f, panY: Float = 0f): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
                var sampleSize = 1
                while (maxDim / sampleSize > 1024) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBitmap = resolver.openInputStream(sourceUri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: throw IllegalArgumentException("Could not decode image.")

                val targetSize = 512
                val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                canvas.drawARGB(0, 0, 0, 0)
                canvas.drawCircle(targetSize / 2f, targetSize / 2f, targetSize / 2f, paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

                val baseScale = targetSize.toFloat() / min(rawBitmap.width, rawBitmap.height).toFloat()
                val finalScale = baseScale * zoom.coerceIn(1f, 4f)
                val matrix = Matrix().apply {
                    postTranslate(-rawBitmap.width / 2f, -rawBitmap.height / 2f)
                    postScale(finalScale, finalScale)
                    postTranslate(targetSize / 2f + panX, targetSize / 2f + panY)
                }
                canvas.drawBitmap(rawBitmap, matrix, paint)
                rawBitmap.recycle()

                val outFile = File(avatarDir, "avatar_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { fos ->
                    output.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }
                output.recycle()
                outFile.absolutePath
            }
        }

    suspend fun exportFileToDownloads(
        sourceFile: File,
        displayFileName: String,
        mimeType: String,
        fallbackSafUri: Uri? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/raw_edit")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore failed to create Downloads entry.")
                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out, bufferSize = 32 * 1024)
                    }
                }
                val doneValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(itemUri, doneValues, null, null)
                itemUri
            } else if (fallbackSafUri != null) {
                context.contentResolver.openOutputStream(fallbackSafUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out, bufferSize = 32 * 1024)
                    }
                }
                fallbackSafUri
            } else {
                // Android 9 app-scoped external Downloads fallback + FileProvider URI
                val legacyDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "raw_edit"
                ).apply { mkdirs() }
                val dest = File(legacyDir, displayFileName)
                sourceFile.copyTo(dest, overwrite = true)
                getShareableUri(dest)
            }
        }
    }

    suspend fun exportVideoToMovies(sourceFile: File, displayFileName: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, displayFileName)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/raw_edit")
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("MediaStore failed to create Movies entry.")
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out, bufferSize = 64 * 1024)
                        }
                    }
                    val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                    resolver.update(uri, done, null, null)
                    uri
                } else {
                    val moviesDir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                        "raw_edit"
                    ).apply { mkdirs() }
                    val dest = File(moviesDir, displayFileName)
                    sourceFile.copyTo(dest, overwrite = true)
                    getShareableUri(dest)
                }
            }
        }

    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
