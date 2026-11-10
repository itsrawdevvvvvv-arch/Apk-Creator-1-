package com.rawedit.app.ui.screens

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rawedit.app.data.VideoEntity
import com.rawedit.app.domain.SrtParser
import com.rawedit.app.render.RenderProgressState
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.components.RawButton
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.mascots.MascotMood
import com.rawedit.app.ui.theme.LocalRawMotion
import com.rawedit.app.ui.theme.RawColors
import java.io.File

/**
 * §4.4 Rendering screen: Progress ring, live frame preview (StateFlow from RenderService),
 * ETA, elapsed, cancel button, and 3 animated pixel mascots "carrying" the render.
 */
@Composable
fun RenderingScreen(
    renderState: RenderProgressState,
    onCancel: () -> Unit,
    onFinished: (String) -> Unit
) {
    LaunchedEffect(renderState.completedVideoId) {
        val doneId = renderState.completedVideoId
        if (doneId != null && !renderState.isRendering) {
            onFinished(doneId)
        }
    }

    DotGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "STAGE 5 // ON-DEVICE 1080P30 ENCODER",
                    style = MaterialTheme.typography.labelMedium,
                    color = RawColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = renderState.projectTitle.ifBlank { "RENDERING REEL" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = RawColors.TextPrimary
                )
            }

            // Custom Canvas Progress Ring + Percentage
            Box(
                modifier = Modifier.size(168.dp),
                contentAlignment = Alignment.Center
            ) {
                val pct = (renderState.progressFraction * 100f).toInt().coerceIn(0, 100)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = RawColors.Divider,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = RawColors.TextPrimary,
                        startAngle = -90f,
                        sweepAngle = 360f * renderState.progressFraction.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Square)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = RawColors.TextPrimary
                    )
                    Text(
                        text = "F ${renderState.currentFrame}/${renderState.totalFrames}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                }
            }

            // Live frame preview + 3 mascots "carrying" the frame
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(RawColors.Surface)
                        .border(1.dp, RawColors.TextPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = renderState.previewBitmap
                    if (bmp != null && !bmp.isRecycled) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Live Render Frame",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = renderState.statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3 animated pixel mascots carrying the frame overhead
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedPixelMascot(mascotId = MascotId.CRT_BOT, size = 32.dp, mood = MascotMood.WORKING)
                    AnimatedPixelMascot(mascotId = MascotId.GYRO_TOP, size = 32.dp, mood = MascotMood.WORKING)
                    AnimatedPixelMascot(mascotId = MascotId.ARCADE_STICK, size = 32.dp, mood = MascotMood.WORKING)
                }
            }

            // Elapsed / ETA / Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = renderState.statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = RawColors.TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ELAPSED: ${SrtParser.formatTimecode(renderState.elapsedMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                    Text(
                        text = "ETA: ${SrtParser.formatTimecode(renderState.etaMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                }
                renderState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RawColors.ErrorCrimson
                    )
                }
            }

            RawButton(
                label = if (renderState.isRendering) "CANCEL RENDER" else "BACK TO EDITOR",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                inverted = false,
                leadingIcon = RawIconType.CLOSE
            )
        }
    }
}

/**
 * §4.4 Result screen: Media3 video player, file size, duration, SHARE (FileProvider),
 * SAVE status (Movies/raw_edit), RE-RENDER, OPEN KEYFRAME SHEET, EDIT PROMPT & RETRY,
 * plus pixel confetti and dancing mascots.
 */
@Composable
fun ResultScreen(
    video: VideoEntity?,
    projectTitle: String,
    onShareVideo: (String) -> Unit,
    onReRender: () -> Unit,
    onOpenKeyframeSheet: () -> Unit,
    onEditPromptAndRetry: () -> Unit,
    onBackToMyReels: () -> Unit
) {
    val context = LocalContext.current
    val motion = LocalRawMotion.current

    val exoPlayer = remember(context, video?.uri) {
        ExoPlayer.Builder(context).build().apply {
            val path = video?.uri ?: ""
            if (path.isNotBlank() && File(path).exists()) {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                prepare()
                playWhenReady = true
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    DotGridBackground {
        // Pixel confetti overlay when reduced motion is off
        if (!motion.reducedMotion) {
            PixelConfettiOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RENDER COMPLETE // 1080P30 MP4",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                    Text(
                        text = projectTitle.ifBlank { "FINISHED REEL" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = RawColors.TextPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimatedPixelMascot(mascotId = MascotId.PIXEL_CAT, size = 28.dp, mood = MascotMood.HAPPY)
                    AnimatedPixelMascot(mascotId = MascotId.BYTE_MUG, size = 28.dp, mood = MascotMood.HAPPY)
                }
            }

            // 16:9 Media3 Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(RawColors.Background)
                    .border(1.dp, RawColors.TextPrimary)
            ) {
                if (video != null && File(video.uri).exists()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = true
                            }
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "MP4 READY IN MOVIES/raw_edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextSecondary
                        )
                    }
                }
            }

            // Metadata card
            val sizeKb = (video?.sizeBytes ?: 0L) / 1024L
            val durStr = SrtParser.formatTimecode(video?.durationMs ?: 6000L)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("DURATION: $durStr · SIZE: ${sizeKb} KB · 1920×1080 @ 30 FPS", style = MaterialTheme.typography.labelMedium, color = RawColors.TextPrimary)
                Text("SAVED AUTOMATICALLY TO: Movies/raw_edit", style = MaterialTheme.typography.labelSmall, color = RawColors.TextSecondary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RawButton(
                    label = "SHARE MP4",
                    onClick = { video?.let { onShareVideo(it.uri) } },
                    modifier = Modifier.weight(1f),
                    inverted = true,
                    leadingIcon = RawIconType.SHARE
                )
                RawButton(
                    label = "RE-RENDER",
                    onClick = onReRender,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.BOLT
                )
            }

            RawButton(
                label = "OPEN KEYFRAME SHEET",
                onClick = onOpenKeyframeSheet,
                modifier = Modifier.fillMaxWidth(),
                inverted = false,
                leadingIcon = RawIconType.SHEET
            )

            RawButton(
                label = "EDIT PROMPT & RETRY",
                onClick = onEditPromptAndRetry,
                modifier = Modifier.fillMaxWidth(),
                inverted = false,
                leadingIcon = RawIconType.EDIT
            )

            RawButton(
                label = "DONE // MY REELS",
                onClick = onBackToMyReels,
                modifier = Modifier.fillMaxWidth(),
                inverted = false,
                leadingIcon = RawIconType.FILM
            )
        }
    }
}

@Composable
private fun PixelConfettiOverlay() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val fall by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiFall"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until 18) {
            val x = ((i * 97 + 31) % 100) / 100f * w
            val y = ((fall + (i * 0.07f)) % 1f) * h
            drawRect(
                color = RawColors.TextPrimary.copy(alpha = 0.25f),
                topLeft = Offset(x, y),
                size = Size(6.dp.toPx(), 6.dp.toPx())
            )
        }
    }
}
