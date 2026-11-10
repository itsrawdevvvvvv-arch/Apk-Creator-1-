package com.rawedit.app.ui.mascots

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rawedit.app.ui.theme.LocalRawMotion
import com.rawedit.app.ui.theme.RawColors
import kotlin.math.roundToInt

/**
 * Stepped pixel mascot animator (2-6 fps) with `Modifier.graphicsLayer` movement so zero layout
 * or composition passes occur during animation loops. Freezes cleanly to frame 0 when
 * `reducedMotion` is enabled in UserPreferences.
 */
@Composable
fun AnimatedPixelMascot(
    mascotId: MascotId,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    mood: MascotMood = MascotMood.IDLE,
    fpsOverride: Int? = null,
    alpha: Float = 1f,
    drawShadow: Boolean = false
) {
    val definition = remember(mascotId) { MascotLibrary.byId(mascotId) }
    val motion = LocalRawMotion.current

    if (motion.reducedMotion) {
        PixelSpriteCanvas(
            sprite = definition.frames.first(),
            modifier = modifier,
            size = size,
            alpha = alpha,
            drawShadowGrid = drawShadow
        )
        return
    }

    val targetFps = (fpsOverride ?: if (motion.isLowRamDevice) 3 else definition.defaultFps).coerceIn(2, 6)
    val frameCount = definition.frames.size.coerceAtLeast(1)
    val cycleMs = (1000f / targetFps * frameCount).roundToInt().coerceAtLeast(300)

    val transition = rememberInfiniteTransition(label = "mascot_${mascotId.name}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascotPhase"
    )

    val frameIdx = ((phase * frameCount).toInt()).coerceIn(0, frameCount - 1)
    val steppedBobPx = if (frameIdx % 2 == 0) 0f else -3f
    val moodTilt = when (mood) {
        MascotMood.IDLE -> 0f
        MascotMood.HAPPY -> if (frameIdx % 2 == 0) -6f else 6f
        MascotMood.DIZZY -> if (frameIdx % 2 == 0) -14f else 14f
        MascotMood.WORKING -> if (frameIdx == 1) 4f else -4f
    }
    val tint = if (mood == MascotMood.DIZZY) RawColors.ErrorCrimson else RawColors.TextPrimary

    PixelSpriteCanvas(
        sprite = definition.frames[frameIdx],
        modifier = modifier.graphicsLayer {
            translationY = steppedBobPx
            rotationZ = moodTilt
        },
        size = size,
        primaryColor = tint,
        alpha = alpha,
        drawShadowGrid = drawShadow
    )
}
