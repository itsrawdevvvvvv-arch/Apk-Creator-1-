package com.rawedit.app.render

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.rawedit.app.data.CacheManager
import com.rawedit.app.data.FrameFlag
import com.rawedit.app.data.KeyframeSheetEntity
import com.rawedit.app.data.KeyframeTileMeta
import com.rawedit.app.data.ProjectEntity
import com.rawedit.app.domain.DeviceBudgetProfile
import com.rawedit.app.domain.DomAutofitEngine
import com.rawedit.app.domain.LocalAssetServer
import com.rawedit.app.domain.PromptGenerator
import com.rawedit.app.domain.SrtCue
import com.rawedit.app.domain.SrtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.TreeSet
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume

@Serializable
data class SheetManifestJson(
    val projectId: String,
    val projectTitle: String,
    val fps: Int = 30,
    val durationMs: Long,
    val codeHash: String,
    val sampleIntervalMs: Long,
    val sheetCount: Int,
    val frames: List<KeyframeTileMeta>
)

data class KeyframeSheetProgress(
    val currentTile: Int,
    val totalTiles: Int,
    val currentTimecode: String,
    val phaseLabel: String
)

class KeyframeSheetGenerator(
    private val context: Context,
    private val cacheManager: CacheManager,
    private val deviceProfile: DeviceBudgetProfile
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    companion object {
        const val MAX_TILES_PER_SHEET = 400
        const val TILES_PER_PDF_PAGE = 12 // 4 columns x 3 rows on A4 landscape
        const val PDF_PAGE_WIDTH = 842    // A4 landscape pt
        const val PDF_PAGE_HEIGHT = 595
    }

    /**
     * Builds the ordered, deduplicated list of sample timestamps in milliseconds.
     * Force-includes t=0, last frame, every SRT cue boundary, and any flagged timestamps.
     */
    fun computeSampleTimestamps(
        durationMs: Long,
        intervalMs: Long,
        srtCues: List<SrtCue>,
        existingFlags: List<FrameFlag>,
        onlyTimestampsMs: List<Long>? = null
    ): List<Long> {
        val safeDuration = durationMs.coerceAtLeast(1000L)
        if (!onlyTimestampsMs.isNullOrEmpty()) {
            return onlyTimestampsMs.map { it.coerceIn(0L, safeDuration) }.distinct().sorted()
        }

        val set = TreeSet<Long>()
        set.add(0L)
        val step = if (safeDuration <= 10_000L && intervalMs <= 34L) 33L else intervalMs.coerceAtLeast(100L)
        var t = 0L
        while (t < safeDuration) {
            set.add(t)
            t += step
        }
        set.add((safeDuration - 33L).coerceAtLeast(0L))

        for (cue in srtCues) {
            set.add(cue.startMs.coerceIn(0L, safeDuration))
            set.add(cue.endMs.coerceIn(0L, safeDuration))
        }
        for (flag in existingFlags) {
            set.add(flag.timeMs.coerceIn(0L, safeDuration))
        }
        return set.toList()
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun generateSheet(
        project: ProjectEntity,
        resolvedHtml: String,
        resolvedScript: String,
        srtCues: List<SrtCue>,
        existingFlags: List<FrameFlag>,
        intervalMs: Long,
        onlyTimestampsMs: List<Long>? = null,
        onProgress: (KeyframeSheetProgress) -> Unit
    ): Result<Pair<KeyframeSheetEntity, List<FrameFlag>>> = withContext(Dispatchers.Default) {
        runCatching {
            val sheetId = UUID.randomUUID().toString()
            val outDir = File(cacheManager.getSheetsDirForProject(project.id), sheetId).apply { mkdirs() }
            val keyframesDir = File(outDir, "keyframes").apply { mkdirs() }
            val sheetSubDir = File(outDir, "sheet").apply { mkdirs() }

            val durationMs = project.audioDurationMs.coerceAtLeast(1000L)
            val totalFramesInTimeline = ((durationMs / 1000.0) * 30.0).toInt().coerceAtLeast(1)
            val timestamps = computeSampleTimestamps(
                durationMs = durationMs,
                intervalMs = intervalMs,
                srtCues = srtCues,
                existingFlags = existingFlags,
                onlyTimestampsMs = onlyTimestampsMs
            )

            val totalTiles = timestamps.size.coerceAtLeast(1)
            val sheetCount = ((totalTiles + MAX_TILES_PER_SHEET - 1) / MAX_TILES_PER_SHEET).coerceAtLeast(1)

            val thumbW = deviceProfile.tileThumbnailWidth
            val thumbH = deviceProfile.tileThumbnailHeight
            val captionH = 44
            val tileFullH = thumbH + captionH

            val allTilesMeta = ArrayList<KeyframeTileMeta>(totalTiles)
            val discoveredFlags = ArrayList<FrameFlag>()

            // Single reusable full-HD capture bitmap & tile bitmap for flat memory footprint
            var captureBitmap: Bitmap? = null
            var tileBitmap: Bitmap? = null

            try {
                captureBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
                tileBitmap = Bitmap.createBitmap(thumbW, tileFullH, Bitmap.Config.ARGB_8888)
                val captureCanvas = Canvas(captureBitmap)
                val tileCanvas = Canvas(tileBitmap)

                val assetServer = LocalAssetServer(context.applicationContext, resolvedHtml)

                withContext(Dispatchers.Main) {
                    val webView = WebView(context.applicationContext)
                    try {
                        webView.settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = false
                            allowFileAccess = false
                            allowContentAccess = false
                            blockNetworkLoads = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        webView.layout(0, 0, 1920, 1080)

                        suspendCancellableCoroutine<Unit> { cont ->
                            webView.webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    return if (request != null) assetServer.intercept(request) else null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                            webView.loadUrl(LocalAssetServer.BASE_URL)
                        }

                        for ((idx, timeMs) in timestamps.withIndex()) {
                            val tcStr = SrtParser.formatTimecode(timeMs)
                            onProgress(
                                KeyframeSheetProgress(
                                    currentTile = idx + 1,
                                    totalTiles = totalTiles,
                                    currentTimecode = tcStr,
                                    phaseLabel = "CAPTURING KEYFRAMES"
                                )
                            )

                            val sheetIdx = (idx / MAX_TILES_PER_SHEET) + 1
                            val posInPage = idx % TILES_PER_PDF_PAGE
                            val rowOneBased = (posInPage / 4) + 1
                            val colOneBased = (posInPage % 4) + 1
                            val tileId = String.format("R%02dC%02d", rowOneBased, colOneBased)
                            val frameNumber = ((timeMs / 1000.0) * 30.0).toInt()

                            // Seek + measure DOM + wait 2 rAF ticks
                            val report = DomAutofitEngine.seekAndMeasure(
                                webView = webView,
                                timeSeconds = timeMs / 1000.0,
                                applyAutofitTransform = true
                            )
                            awaitTwoRafAndVisualState(webView, idx.toLong() + 1L)

                            // Draw 1920x1080 WebView into reused captureBitmap
                            captureCanvas.drawColor(Color.parseColor("#0F0F0F"))
                            webView.draw(captureCanvas)

                            val frameFlags = ArrayList<FrameFlag>()
                            for (v in report.violations) {
                                val sev = when (v.kind) {
                                    "BLANK", "JSERR", "MISSINGIMG" -> "ERROR"
                                    else -> "WARN"
                                }
                                val f = FrameFlag(
                                    timeMs = timeMs,
                                    frameIndex = frameNumber,
                                    tileId = tileId,
                                    rule = v.kind,
                                    severity = sev,
                                    detail = "${v.selector}: ${v.detail}",
                                    domSnapshot = report.domSnapshotSummary
                                )
                                frameFlags.add(f)
                                discoveredFlags.add(f)
                            }

                            val primaryFlag = frameFlags.firstOrNull()?.rule ?: "OK"
                            val status = when {
                                frameFlags.any { it.severity == "ERROR" } -> "ERROR"
                                frameFlags.isNotEmpty() -> "WARN"
                                else -> "CLEAN"
                            }

                            // Render burned-in tile (thumbnail + permanent caption bar)
                            renderBurnedTile(
                                source1080p = captureBitmap,
                                targetTile = tileBitmap,
                                targetCanvas = tileCanvas,
                                thumbW = thumbW,
                                thumbH = thumbH,
                                timecode = tcStr,
                                frameNumber = frameNumber,
                                totalFrames = totalFramesInTimeline,
                                tileId = tileId,
                                status = status,
                                flagName = primaryFlag
                            )

                            val safeTcFile = tcStr.replace(':', '-')
                            val fileName = String.format("frame_%06d_%s.png", frameNumber, safeTcFile)
                            val pngFile = File(keyframesDir, fileName)
                            withContext(Dispatchers.IO) {
                                FileOutputStream(pngFile).use { fos ->
                                    tileBitmap.compress(Bitmap.CompressFormat.PNG, 92, fos)
                                }
                            }

                            allTilesMeta.add(
                                KeyframeTileMeta(
                                    index = idx,
                                    sheetIndex = sheetIdx,
                                    timeMs = timeMs,
                                    timecode = tcStr,
                                    frameIndex = frameNumber,
                                    totalFrames = totalFramesInTimeline,
                                    tileId = tileId,
                                    imagePath = pngFile.absolutePath,
                                    status = status,
                                    primaryFlag = primaryFlag,
                                    flags = frameFlags
                                )
                            )
                        }
                    } finally {
                        runCatching {
                            webView.stopLoading()
                            webView.loadUrl("about:blank")
                            webView.destroy()
                        }
                    }
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                throw IllegalStateException("Low memory during keyframe capture. Try a 1.0s or 2.0s sample interval.")
            } finally {
                captureBitmap?.recycle()
                tileBitmap?.recycle()
            }

            // Build PDF contact sheet(s) + Appendix table
            onProgress(
                KeyframeSheetProgress(
                    currentTile = totalTiles,
                    totalTiles = totalTiles,
                    currentTimecode = SrtParser.formatTimecode(durationMs),
                    phaseLabel = "WRITING PDF CONTACT SHEET"
                )
            )
            val pdfFiles = buildPdfSheets(
                project = project,
                sheetSubDir = sheetSubDir,
                allTiles = allTilesMeta,
                allFlags = discoveredFlags,
                sheetCount = sheetCount
            )
            val primaryPdfFile = pdfFiles.first()

            // Write manifest.json, index.html, and FIX_PROMPT.txt
            val manifestObj = SheetManifestJson(
                projectId = project.id,
                projectTitle = project.title,
                fps = 30,
                durationMs = durationMs,
                codeHash = project.codeHash,
                sampleIntervalMs = intervalMs,
                sheetCount = sheetCount,
                frames = allTilesMeta
            )
            val manifestFile = File(sheetSubDir, "manifest.json")
            manifestFile.writeText(json.encodeToString(manifestObj), Charsets.UTF_8)

            val htmlIndexFile = File(sheetSubDir, "index.html")
            htmlIndexFile.writeText(buildOfflineHtmlSheet(project, allTilesMeta), Charsets.UTF_8)

            val fixPromptText = PromptGenerator.buildFixPrompt(
                project = project,
                flags = discoveredFlags,
                codeHash = project.codeHash,
                htmlSourceOrSlice = resolvedHtml,
                includeFullHtml = true
            )
            val fixPromptFile = File(outDir, "FIX_PROMPT.txt")
            fixPromptFile.writeText(fixPromptText, Charsets.UTF_8)

            // Bundle everything into ZIP
            onProgress(
                KeyframeSheetProgress(
                    currentTile = totalTiles,
                    totalTiles = totalTiles,
                    currentTimecode = SrtParser.formatTimecode(durationMs),
                    phaseLabel = "PACKAGING ARCHIVE ZIP"
                )
            )
            val zipFile = File(outDir, "keyframe_sheet_${project.id.take(8)}.zip")
            createSheetZip(
                zipFile = zipFile,
                keyframesDir = keyframesDir,
                pdfFiles = pdfFiles,
                manifestFile = manifestFile,
                htmlIndexFile = htmlIndexFile,
                fixPromptFile = fixPromptFile
            )

            // Export PDF & ZIP to Downloads/raw_edit on Android 10+ (or app external Downloads on Android 9)
            cacheManager.exportFileToDownloads(
                sourceFile = primaryPdfFile,
                displayFileName = "${project.title.lowercase().replace(' ', '_')}_sheet_01.pdf",
                mimeType = "application/pdf"
            )
            cacheManager.exportFileToDownloads(
                sourceFile = zipFile,
                displayFileName = "${project.title.lowercase().replace(' ', '_')}_keyframes.zip",
                mimeType = "application/zip"
            )

            val totalPages = ((allTilesMeta.size + TILES_PER_PDF_PAGE - 1) / TILES_PER_PDF_PAGE) + 1
            val entity = KeyframeSheetEntity(
                id = sheetId,
                projectId = project.id,
                createdAt = System.currentTimeMillis(),
                intervalMs = intervalMs,
                tileCount = allTilesMeta.size,
                pageCount = totalPages,
                pdfUri = primaryPdfFile.absolutePath,
                zipUri = zipFile.absolutePath,
                flagsJson = json.encodeToString(discoveredFlags),
                tilesManifestJson = json.encodeToString(allTilesMeta)
            )
            entity to discoveredFlags
        }
    }

    private suspend fun awaitTwoRafAndVisualState(webView: WebView, requestId: Long) =
        suspendCancellableCoroutine<Unit> { cont ->
            val rafJs = "requestAnimationFrame(function(){ requestAnimationFrame(function(){}); });"
            webView.evaluateJavascript(rafJs) {
                webView.postVisualStateCallback(requestId, object : WebView.VisualStateCallback() {
                    override fun onComplete(completedRequestId: Long) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
            }
        }

    private fun renderBurnedTile(
        source1080p: Bitmap,
        targetTile: Bitmap,
        targetCanvas: Canvas,
        thumbW: Int,
        thumbH: Int,
        timecode: String,
        frameNumber: Int,
        totalFrames: Int,
        tileId: String,
        status: String,
        flagName: String
    ) {
        val bgPaint = Paint().apply { color = Color.parseColor("#0F0F0F"); style = Paint.Style.FILL }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#2A2A2A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.MONOSPACE
            textSize = 15f
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A8A8A")
            typeface = Typeface.MONOSPACE
            textSize = 13f
        }
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = when (status) {
                "ERROR" -> Color.parseColor("#FF3B30")
                "WARN" -> Color.parseColor("#FFB020")
                else -> Color.WHITE
            }
        }

        targetCanvas.drawRect(0f, 0f, targetTile.width.toFloat(), targetTile.height.toFloat(), bgPaint)

        val srcRect = Rect(0, 0, source1080p.width, source1080p.height)
        val dstRect = Rect(1, 1, thumbW - 1, thumbH - 1)
        val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        targetCanvas.drawBitmap(source1080p, srcRect, dstRect, bmpPaint)

        // Caption bar divider + text
        targetCanvas.drawLine(0f, thumbH.toFloat(), thumbW.toFloat(), thumbH.toFloat(), borderPaint)
        targetCanvas.drawText(timecode, 8f, thumbH + 18f, textPaint)
        targetCanvas.drawText("F $frameNumber/$totalFrames · $tileId", 8f, thumbH + 36f, subTextPaint)

        // Right-aligned status dot + short flag name
        val flagLabel = if (status == "CLEAN") "CLEAN" else flagName.take(11)
        val labelWidth = textPaint.measureText(flagLabel)
        val dotCx = thumbW - labelWidth - 18f
        val dotCy = thumbH + 22f
        targetCanvas.drawCircle(dotCx, dotCy, 5f, dotPaint)
        targetCanvas.drawText(flagLabel, dotCx + 10f, thumbH + 27f, textPaint)

        // 1px outer border
        targetCanvas.drawRect(
            0.5f,
            0.5f,
            targetTile.width - 0.5f,
            targetTile.height - 0.5f,
            borderPaint
        )
    }

    private fun buildPdfSheets(
        project: ProjectEntity,
        sheetSubDir: File,
        allTiles: List<KeyframeTileMeta>,
        allFlags: List<FrameFlag>,
        sheetCount: Int
    ): List<File> {
        val outFiles = ArrayList<File>(sheetCount)
        val durationStr = SrtParser.formatTimecode(project.audioDurationMs)
        val hashShort = project.codeHash.take(12).ifBlank { "000000000000" }

        val bgPaint = Paint().apply { color = Color.parseColor("#0F0F0F"); style = Paint.Style.FILL }
        val cardPaint = Paint().apply { color = Color.parseColor("#1C1C1C"); style = Paint.Style.FILL }
        val rulePaint = Paint().apply { color = Color.parseColor("#2A2A2A"); style = Paint.Style.STROKE; strokeWidth = 1f }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 10f
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A8A8A")
            typeface = Typeface.MONOSPACE
            textSize = 8.5f
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.MONOSPACE
            textSize = 7.5f
        }

        for (sheetIdx in 1..sheetCount) {
            val sheetTiles = allTiles.filter { it.sheetIndex == sheetIdx }
            val pdf = PdfDocument()
            val pagesForTiles = ((sheetTiles.size + TILES_PER_PDF_PAGE - 1) / TILES_PER_PDF_PAGE).coerceAtLeast(1)
            val totalPdfPages = pagesForTiles + 1 // +1 Appendix page

            for (pageIdx in 0 until pagesForTiles) {
                val pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageIdx + 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawRect(0f, 0f, PDF_PAGE_WIDTH.toFloat(), PDF_PAGE_HEIGHT.toFloat(), bgPaint)

                val pageSlice = sheetTiles.drop(pageIdx * TILES_PER_PDF_PAGE).take(TILES_PER_PDF_PAGE)
                val rangeStart = pageSlice.firstOrNull()?.timecode ?: "00:00:00.000"
                val rangeEnd = pageSlice.lastOrNull()?.timecode ?: durationStr

                // Header
                val headerText = "raw_edit · ${project.title} · 1920x1080 · 30 fps · $durationStr · sheet $sheetIdx/$sheetCount"
                canvas.drawText(headerText, 28f, 24f, headerPaint)
                canvas.drawLine(28f, 32f, (PDF_PAGE_WIDTH - 28).toFloat(), 32f, rulePaint)

                // 4x3 Grid
                val gridLeft = 28f
                val gridTop = 42f
                val cellW = 190f
                val cellH = 156f
                val gapX = 8f
                val gapY = 10f

                for ((cellPos, tile) in pageSlice.withIndex()) {
                    val col = cellPos % 4
                    val row = cellPos / 4
                    val x = gridLeft + col * (cellW + gapX)
                    val y = gridTop + row * (cellH + gapY)

                    canvas.drawRect(x, y, x + cellW, y + cellH, cardPaint)
                    val bmpFile = File(tile.imagePath)
                    if (bmpFile.exists()) {
                        val bmp = BitmapFactory.decodeFile(bmpFile.absolutePath)
                        if (bmp != null) {
                            val dst = Rect(x.toInt() + 2, y.toInt() + 2, (x + cellW - 2).toInt(), (y + cellH - 22).toInt())
                            canvas.drawBitmap(bmp, null, dst, null)
                            bmp.recycle()
                        }
                    }
                    // Searchable PDF text caption underneath burned-in bitmap
                    val searchableLine = "${tile.timecode} | F${tile.frameIndex}/${tile.totalFrames} | ${tile.tileId} | ${tile.primaryFlag}"
                    canvas.drawText(searchableLine, x + 4f, y + cellH - 7f, cellTextPaint)
                    canvas.drawRect(x, y, x + cellW, y + cellH, rulePaint)
                }

                // Footer
                canvas.drawLine(28f, (PDF_PAGE_HEIGHT - 28).toFloat(), (PDF_PAGE_WIDTH - 28).toFloat(), (PDF_PAGE_HEIGHT - 28).toFloat(), rulePaint)
                val footerText = "page ${pageIdx + 1} of $totalPdfPages · range $rangeStart–$rangeEnd · code sha256 $hashShort"
                canvas.drawText(footerText, 28f, (PDF_PAGE_HEIGHT - 14).toFloat(), footerPaint)

                pdf.finishPage(page)
            }

            // Final Appendix Page listing every flagged frame
            val appendixInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, totalPdfPages).create()
            val appendixPage = pdf.startPage(appendixInfo)
            val ac = appendixPage.canvas
            ac.drawRect(0f, 0f, PDF_PAGE_WIDTH.toFloat(), PDF_PAGE_HEIGHT.toFloat(), bgPaint)
            ac.drawText("APPENDIX // FLAGGED FRAMES TABLE · ${project.title} · SHA256 $hashShort", 28f, 24f, headerPaint)
            ac.drawLine(28f, 32f, (PDF_PAGE_WIDTH - 28).toFloat(), 32f, rulePaint)

            ac.drawText("TIMECODE        | FRAME   | TILE   | FLAG         | DETAIL", 28f, 52f, headerPaint)
            ac.drawLine(28f, 58f, (PDF_PAGE_WIDTH - 28).toFloat(), 58f, rulePaint)

            var rowY = 74f
            if (allFlags.isEmpty()) {
                ac.drawText("00:00:00.000    | ALL     | NONE   | CLEAN        | Zero layout or runtime violations detected across timeline.", 28f, rowY, cellTextPaint)
            } else {
                for (flag in allFlags.take(32)) {
                    val tc = SrtParser.formatTimecode(flag.timeMs).padEnd(15)
                    val fr = ("F" + flag.frameIndex).padEnd(7)
                    val tl = flag.tileId.padEnd(6)
                    val rl = flag.rule.padEnd(12)
                    val dt = flag.detail.take(78)
                    ac.drawText("$tc | $fr | $tl | $rl | $dt", 28f, rowY, cellTextPaint)
                    rowY += 14f
                }
            }

            ac.drawLine(28f, (PDF_PAGE_HEIGHT - 28).toFloat(), (PDF_PAGE_WIDTH - 28).toFloat(), (PDF_PAGE_HEIGHT - 28).toFloat(), rulePaint)
            ac.drawText("page $totalPdfPages of $totalPdfPages · appendix summary · code sha256 $hashShort", 28f, (PDF_PAGE_HEIGHT - 14).toFloat(), footerPaint)
            pdf.finishPage(appendixPage)

            val pdfFile = File(sheetSubDir, String.format("sheet_%02d.pdf", sheetIdx))
            FileOutputStream(pdfFile).use { fos -> pdf.writeTo(fos) }
            pdf.close()
            outFiles.add(pdfFile)
        }
        return outFiles
    }

    private fun buildOfflineHtmlSheet(project: ProjectEntity, tiles: List<KeyframeTileMeta>): String {
        return buildString {
            appendLine("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>raw_edit Contact Sheet - ${project.title}</title>")
            appendLine("<style>body{background:#0F0F0F;color:#FFF;font-family:monospace;padding:24px;} .grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;} .card{background:#1C1C1C;border:1px solid #2A2A2A;padding:6px;} img{width:100%;display:block;}</style></head><body>")
            appendLine("<h2>RAW_EDIT // CONTACT SHEET // ${project.title}</h2>")
            appendLine("<p>Total sampled frames: ${tiles.size} | Code SHA-256: ${project.codeHash.take(12)}</p>")
            appendLine("<div class='grid'>")
            for (t in tiles) {
                val fileName = File(t.imagePath).name
                appendLine("<div class='card'><img src='../keyframes/$fileName' /><div>${t.timecode} · F${t.frameIndex} · ${t.tileId} · ${t.primaryFlag}</div></div>")
            }
            appendLine("</div></body></html>")
        }
    }

    private fun createSheetZip(
        zipFile: File,
        keyframesDir: File,
        pdfFiles: List<File>,
        manifestFile: File,
        htmlIndexFile: File,
        fixPromptFile: File
    ) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            keyframesDir.listFiles()?.sortedBy { it.name }?.forEach { png ->
                zos.putNextEntry(ZipEntry("keyframes/${png.name}"))
                FileInputStream(png).use { it.copyTo(zos, 32 * 1024) }
                zos.closeEntry()
            }
            for (pdf in pdfFiles) {
                zos.putNextEntry(ZipEntry("sheet/${pdf.name}"))
                FileInputStream(pdf).use { it.copyTo(zos, 32 * 1024) }
                zos.closeEntry()
            }
            zos.putNextEntry(ZipEntry("sheet/manifest.json"))
            FileInputStream(manifestFile).use { it.copyTo(zos, 16 * 1024) }
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("sheet/index.html"))
            FileInputStream(htmlIndexFile).use { it.copyTo(zos, 16 * 1024) }
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("FIX_PROMPT.txt"))
            FileInputStream(fixPromptFile).use { it.copyTo(zos, 16 * 1024) }
            zos.closeEntry()
        }
    }
}
