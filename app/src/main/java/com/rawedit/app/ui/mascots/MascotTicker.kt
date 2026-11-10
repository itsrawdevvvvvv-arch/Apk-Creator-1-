package com.rawedit.app.ui.mascots

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rawedit.app.ui.theme.LocalRawMotion
import kotlin.math.roundToInt

/**
 * Horizontal marquee strip of retro mascots walking across a screen edge.
 */
@Composable
fun MascotTicker(
    modifier: Modifier = Modifier,
    mascotSize: Dp = 22.dp,
    alpha: Float = 0.55f
) {
    val motion = LocalRawMotion.current
    val mascots = remember { MascotLibrary.ALL.take(8) }

    val transition = rememberInfiniteTransition(label = "ticker")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (motion.reducedMotion) 0f else -180f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tickerShift"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(mascotSize + 8.dp)
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                translationX = shift
            },
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            (mascots + mascots.take(4)).forEach { def ->
                AnimatedPixelMascot(
                    mascotId = def.id,
                    size = mascotSize,
                    alpha = alpha
                )
            }
        }
    }
}

/**
 * Gentle drifting pixel mascot rain used for background ambiance on Home, Rendering, and Result.
 */
@Composable
fun MascotRain(
    modifier: Modifier = Modifier,
    count: Int = 4,
    alpha: Float = 0.16f
) {
    val motion = LocalRawMotion.current
    val actualCount = if (motion.isLowRamDevice) minOf(count, 3) else count
    val chosen = remember(actualCount) { MascotLibrary.ALL.take(actualCount) }

    val transition = rememberInfiniteTransition(label = "mascotRain")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (motion.reducedMotion) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(modifier = modifier.fillMaxSize()) {
        chosen.forEachIndexed { idx, def ->
            val xFraction = 0.12f + (idx * 0.24f) % 0.78f
            val baseTopDp = 48 + idx * 120
            val yOffsetPx = ((drift - 0.5f) * 36f * (if (idx % 2 == 0) 1 else -1)).roundToInt()

            AnimatedPixelMascot(
                mascotId = def.id,
                size = 28.dp,
                alpha = alpha,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((idx + 1) * 64),
                            y = baseTopDp + yOffsetPx
                        )
                    }
                    .graphicsLayer {
                        translationX = xFraction * 180f
                    }
            )
        }
    }
}
