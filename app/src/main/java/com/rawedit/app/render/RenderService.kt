package com.rawedit.app.render

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import com.rawedit.app.MainActivity
import com.rawedit.app.R
import com.rawedit.app.RawEditApplication
import com.rawedit.app.data.ProjectStatus
import com.rawedit.app.data.VideoEntity
import com.rawedit.app.domain.DomAutofitEngine
import com.rawedit.app.domain.LocalAssetServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.coroutines.resume

class RenderService : Service() {

    companion object {
        const val ACTION_START_RENDER = "com.rawedit.app.action.START_RENDER"
        const val ACTION_CANCEL_RENDER = "com.rawedit.app.action.CANCEL_RENDER"
        const val EXTRA_PROJECT_ID = "extra_project_id"
        private const val NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "raw_edit_render_channel"

        fun startRender(context: Context, projectId: String) {
            val intent = Intent(context, RenderService::class.java).apply {
                action = ACTION_START_RENDER
                putExtra(EXTRA_PROJECT_ID, projectId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelRender(context: Context) {
            RenderStateHolder.requestCancel()
            val intent = Intent(context, RenderService::class.java).apply {
                action = ACTION_CANCEL_RENDER
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var renderJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_RENDER -> {
                RenderStateHolder.requestCancel()
                renderJob?.cancel()
                stopRenderForeground()
            }
            ACTION_START_RENDER -> {
                val projectId = intent.getStringExtra(EXTRA_PROJECT_ID) ?: return START_NOT_STICKY
                startForegroundWithType(buildNotification("INITIALIZING ENGINE", 0, 100))
                acquireWakeLock()
                renderJob?.cancel()
                renderJob = serviceScope.launch {
                    executeRenderPipeline(projectId)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        RenderStateHolder.requestCancel()
        renderJob?.cancel()
        RenderStateHolder.update {
            it.copy(
                isRendering = false,
                statusLabel = "TIMEOUT",
                errorMessage = "System foreground service time limit reached."
            )
        }
        stopRenderForeground()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun executeRenderPipeline(projectId: String) {
        val app = application as RawEditApplication
        val repo = app.repository
        val cache = app.cacheManager

        val project = repo.getProject(projectId)
        if (project == null) {
            stopRenderForeground()
            return
        }

        RenderStateHolder.resetCancel()
        val durationMs = project.audioDurationMs.coerceAtLeast(1000L)
        val totalFrames = ((durationMs / 1000.0) * 30.0).toInt().coerceAtLeast(30)
        val startWallMs = System.currentTimeMillis()

        RenderStateHolder.update {
            RenderProgressState(
                isRendering = true,
                projectId = project.id,
                projectTitle = project.title,
                currentFrame = 0,
                totalFrames = totalFrames,
                progressFraction = 0f,
                elapsedMs = 0L,
                etaMs = durationMs * 2L,
                statusLabel = "PREPARING PIPELINE"
            )
        }

        repo.saveProjectStepState(project.copy(status = ProjectStatus.RENDERING.name))
        val tempDir = cache.getTempRenderDirForJob(project.id)
        val resolvedHtml = cache.resolveTextWithOverflow(project.htmlCode)

        var frameBitmap: Bitmap? = null
        var previewThumb: Bitmap? = null

        try {
            frameBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)

            // 1. Transcode voiceover to 128 kbps AAC if present
            val aacFile = if (project.audioUri.isNotBlank()) {
                RenderStateHolder.update { it.copy(statusLabel = "TRANSCODING AUDIO (128K AAC)") }
                Mp4FrameMuxer.transcodeAudioToAac(this, project.audioUri, tempDir)
            } else {
                null
            }

            // 2. Encode 1920x1080 @ 30fps H.264 frames
            val videoOnlyFile = File(tempDir, "video_raw.mp4")
            val bitrate = Mp4FrameMuxer.selectTargetBitrate(durationMs)
            val encoder = Mp4FrameMuxer.SurfaceVideoEncoder(
                outputFile = videoOnlyFile,
                width = 1920,
                height = 1080,
                fps = 30,
                bitrate = bitrate
            )

            val assetServer = LocalAssetServer(applicationContext, resolvedHtml)
            var thumbPath = ""

            withContext(Dispatchers.Main) {
                val webView = WebView(applicationContext)
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

                    for (frameIdx in 0 until totalFrames) {
                        if (RenderStateHolder.cancelRequested) {
                            throw IllegalStateException("Render cancelled by user.")
                        }

                        val tSec = frameIdx / 30.0
                        DomAutofitEngine.seekAndMeasure(
                            webView = webView,
                            timeSeconds = tSec,
                            applyAutofitTransform = true
                        )

                        frameCanvas.drawColor(Color.parseColor("#0F0F0F"))
                        webView.draw(frameCanvas)

                        withContext(Dispatchers.Default) {
                            encoder.encodeBitmapFrame(frameBitmap, frameIdx, 30)
                        }

                        // Save first frame as 16:9 project thumbnail
                        if (frameIdx == 0) {
                            val thumbFile = File(filesDir, "thumb_${project.id}.png")
                            val scaled = Bitmap.createScaledBitmap(frameBitmap, 480, 270, true)
                            withContext(Dispatchers.IO) {
                                FileOutputStream(thumbFile).use { fos ->
                                    scaled.compress(Bitmap.CompressFormat.PNG, 90, fos)
                                }
                            }
                            thumbPath = thumbFile.absolutePath
                            previewThumb = scaled
                        } else if (frameIdx % 10 == 0) {
                            previewThumb = Bitmap.createScaledBitmap(frameBitmap, 480, 270, true)
                        }

                        val elapsed = (System.currentTimeMillis() - startWallMs).coerceAtLeast(1L)
                        val doneRatio = (frameIdx + 1).toFloat() / totalFrames.toFloat()
                        val eta = ((elapsed / doneRatio) - elapsed).toLong().coerceAtLeast(0L)

                        RenderStateHolder.update {
                            it.copy(
                                currentFrame = frameIdx + 1,
                                totalFrames = totalFrames,
                                progressFraction = doneRatio,
                                elapsedMs = elapsed,
                                etaMs = eta,
                                statusLabel = "ENCODING 1080P30 FRAME ${frameIdx + 1}/$totalFrames",
                                previewBitmap = previewThumb
                            )
                        }

                        if (frameIdx % 15 == 0) {
                            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            nm.notify(
                                NOTIFICATION_ID,
                                buildNotification("Rendering ${project.title}", frameIdx + 1, totalFrames)
                            )
                        }
                    }
                } finally {
                    runCatching {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        webView.destroy()
                    }
                }
            }

            encoder.finishAndMuxAudio(aacFile, durationMs * 1000L)

            // 3. Remux video + audio and export to Movies/raw_edit
            RenderStateHolder.update { it.copy(statusLabel = "MUXING MP4 CONTAINER") }
            val finalLocalMp4 = File(filesDir, "reel_${project.id}_${System.currentTimeMillis()}.mp4")
            Mp4FrameMuxer.remuxVideoAndAudio(
                videoOnlyMp4 = videoOnlyFile,
                aacAudioM4a = aacFile,
                finalMp4 = finalLocalMp4,
                maxDurationUs = durationMs * 1000L
            )

            val cleanTitle = project.title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            cache.exportVideoToMovies(finalLocalMp4, "${cleanTitle}_1080p30.mp4")

            val videoId = UUID.randomUUID().toString()
            val videoEntity = VideoEntity(
                id = videoId,
                projectId = project.id,
                uri = finalLocalMp4.absolutePath,
                durationMs = durationMs,
                sizeBytes = finalLocalMp4.length(),
                width = 1920,
                height = 1080,
                fps = 30,
                createdAt = System.currentTimeMillis(),
                thumbnailUri = thumbPath
            )
            repo.recordCompletedVideo(videoEntity)

            RenderStateHolder.update {
                it.copy(
                    isRendering = false,
                    progressFraction = 1f,
                    statusLabel = "COMPLETED",
                    completedVideoId = videoId
                )
            }
        } catch (oom: OutOfMemoryError) {
            System.gc()
            repo.saveProjectStepState(project.copy(status = ProjectStatus.READY.name))
            RenderStateHolder.update {
                it.copy(
                    isRendering = false,
                    statusLabel = "LOW MEMORY",
                    errorMessage = "Out of memory while encoding 1080p30 stream. Close background apps and retry."
                )
            }
        } catch (t: Throwable) {
            repo.saveProjectStepState(project.copy(status = ProjectStatus.READY.name))
            RenderStateHolder.update {
                it.copy(
                    isRendering = false,
                    statusLabel = "STOPPED",
                    errorMessage = t.message ?: "Render interrupted."
                )
            }
        } finally {
            frameBitmap?.recycle()
            runCatching { tempDir.deleteRecursively() }
            stopRenderForeground()
        }
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(status: String, progress: Int, max: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = Intent(this, RenderService::class.java).apply {
            action = ACTION_CANCEL_RENDER
        }
        val pendingCancel = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon( android.R.drawable.ic_media_play )
            .setContentTitle("RAW_EDIT // 1080P30 ENGINE")
            .setContentText(status)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "CANCEL", pendingCancel)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "raw_edit:RenderWakeLock").apply {
            acquire(25 * 60 * 1000L)
        }
    }

    private fun stopRenderForeground() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        renderJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
