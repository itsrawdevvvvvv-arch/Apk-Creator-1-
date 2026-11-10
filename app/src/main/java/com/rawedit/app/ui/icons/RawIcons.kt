package com.rawedit.app.ui.icons

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rawedit.app.ui.theme.LocalRawMotion
import com.rawedit.app.ui.theme.RawColors

@Immutable
enum class RawIconType {
    HOME, PLUS, FILM, PERSON, PLAY, PAUSE, COPY, PASTE, CHECK, ARROW_RIGHT,
    BACK, IMAGE, LAYERS, TRASH, EYE, EYE_OFF, LOCK, UNLOCK, SHARE, DOWNLOAD,
    EDIT, MIC, CLOCK, BOLT, CAMERA, CLOSE, UNDO, REDO, FORWARD, BACKWARD,
    GRID, SHEET, WARNING, SPARKLE
}

/**
 * Hand-drawn Compose Canvas icon set (34 glyphs).
 * Zero emojis, zero vector XML drawables, zero per-frame heap allocations where possible.
 * Includes spring-driven graphicsLayer morph/scale transitions when activeState changes.
 */
@Composable
fun RawIcon(
    type: RawIconType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = RawColors.TextPrimary,
    strokeWidthDp: Dp = 1.8.dp,
    isActive: Boolean = false
) {
    val motion = LocalRawMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isActive && !motion.reducedMotion) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (isActive && !motion.reducedMotion && (type == RawIconType.PLUS || type == RawIconType.SPARKLE)) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconRotation"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
    ) {
        val sw = strokeWidthDp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Square, join = StrokeJoin.Miter)
        drawGlyph(type, tint, sw, stroke)
    }
}

private fun DrawScope.drawGlyph(
    type: RawIconType,
    tint: Color,
    sw: Float,
    stroke: Stroke
) {
    val w = size.width
    val h = size.height

    when (type) {
        RawIconType.HOME -> {
            val p = Path().apply {
                moveTo(w * 0.15f, h * 0.48f)
                lineTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.85f, h * 0.48f)
                lineTo(w * 0.85f, h * 0.86f)
                lineTo(w * 0.15f, h * 0.86f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawRect(tint, topLeft = Offset(w * 0.42f, h * 0.58f), size = Size(w * 0.16f, h * 0.28f))
        }
        RawIconType.PLUS -> {
            drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = sw)
        }
        RawIconType.FILM -> {
            drawRect(tint, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.64f), style = stroke)
            drawLine(tint, Offset(w * 0.14f, h * 0.34f), Offset(w * 0.86f, h * 0.34f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.14f, h * 0.66f), Offset(w * 0.86f, h * 0.66f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.32f, h * 0.18f), Offset(w * 0.32f, h * 0.34f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.68f, h * 0.18f), Offset(w * 0.68f, h * 0.34f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.32f, h * 0.66f), Offset(w * 0.32f, h * 0.82f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.68f, h * 0.82f), strokeWidth = sw)
        }
        RawIconType.PERSON -> {
            drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
            drawRect(tint, Offset(w * 0.22f, h * 0.58f), Size(w * 0.56f, h * 0.26f), style = stroke)
        }
        RawIconType.PLAY -> {
            val p = Path().apply {
                moveTo(w * 0.26f, h * 0.18f)
                lineTo(w * 0.80f, h * 0.50f)
                lineTo(w * 0.26f, h * 0.82f)
                close()
            }
            drawPath(p, tint)
        }
        RawIconType.PAUSE -> {
            drawRect(tint, Offset(w * 0.24f, h * 0.18f), Size(w * 0.16f, h * 0.64f))
            drawRect(tint, Offset(w * 0.60f, h * 0.18f), Size(w * 0.16f, h * 0.64f))
        }
        RawIconType.COPY -> {
            drawRect(tint, Offset(w * 0.32f, h * 0.32f), Size(w * 0.52f, h * 0.52f), style = stroke)
            drawLine(tint, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.18f, h * 0.18f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.68f, h * 0.18f), strokeWidth = sw)
        }
        RawIconType.PASTE -> {
            drawRect(tint, Offset(w * 0.20f, h * 0.24f), Size(w * 0.60f, h * 0.64f), style = stroke)
            drawRect(tint, Offset(w * 0.36f, h * 0.14f), Size(w * 0.28f, h * 0.12f))
            drawLine(tint, Offset(w * 0.32f, h * 0.48f), Offset(w * 0.68f, h * 0.48f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.32f, h * 0.64f), Offset(w * 0.58f, h * 0.64f), strokeWidth = sw)
        }
        RawIconType.CHECK -> {
            val p = Path().apply {
                moveTo(w * 0.18f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.76f)
                lineTo(w * 0.84f, h * 0.24f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.ARROW_RIGHT -> {
            drawLine(tint, Offset(w * 0.16f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), strokeWidth = sw)
            val p = Path().apply {
                moveTo(w * 0.56f, h * 0.24f)
                lineTo(w * 0.82f, h * 0.50f)
                lineTo(w * 0.56f, h * 0.76f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.BACK -> {
            drawLine(tint, Offset(w * 0.84f, h * 0.5f), Offset(w * 0.18f, h * 0.5f), strokeWidth = sw)
            val p = Path().apply {
                moveTo(w * 0.44f, h * 0.24f)
                lineTo(w * 0.18f, h * 0.50f)
                lineTo(w * 0.44f, h * 0.76f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.IMAGE -> {
            drawRect(tint, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.64f), style = stroke)
            drawCircle(tint, radius = w * 0.07f, center = Offset(w * 0.34f, h * 0.36f))
            val p = Path().apply {
                moveTo(w * 0.16f, h * 0.78f)
                lineTo(w * 0.46f, h * 0.50f)
                lineTo(w * 0.64f, h * 0.66f)
                lineTo(w * 0.76f, h * 0.54f)
                lineTo(w * 0.86f, h * 0.78f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.LAYERS -> {
            val p1 = Path().apply {
                moveTo(w * 0.5f, h * 0.16f)
                lineTo(w * 0.86f, h * 0.36f)
                lineTo(w * 0.5f, h * 0.56f)
                lineTo(w * 0.14f, h * 0.36f)
                close()
            }
            drawPath(p1, tint, style = stroke)
            val p2 = Path().apply {
                moveTo(w * 0.14f, h * 0.54f)
                lineTo(w * 0.5f, h * 0.74f)
                lineTo(w * 0.86f, h * 0.54f)
            }
            drawPath(p2, tint, style = stroke)
        }
        RawIconType.TRASH -> {
            drawLine(tint, Offset(w * 0.18f, h * 0.24f), Offset(w * 0.82f, h * 0.24f), strokeWidth = sw)
            drawRect(tint, Offset(w * 0.26f, h * 0.24f), Size(w * 0.48f, h * 0.60f), style = stroke)
            drawLine(tint, Offset(w * 0.42f, h * 0.38f), Offset(w * 0.42f, h * 0.70f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.58f, h * 0.38f), Offset(w * 0.58f, h * 0.70f), strokeWidth = sw)
        }
        RawIconType.EYE -> {
            drawRect(tint, Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.36f), style = stroke)
            drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.5f, h * 0.5f))
        }
        RawIconType.EYE_OFF -> {
            drawRect(tint, Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.36f), style = stroke)
            drawLine(tint, Offset(w * 0.14f, h * 0.84f), Offset(w * 0.86f, h * 0.16f), strokeWidth = sw)
        }
        RawIconType.LOCK -> {
            drawRect(tint, Offset(w * 0.22f, h * 0.44f), Size(w * 0.56f, h * 0.42f), style = stroke)
            drawRect(tint, Offset(w * 0.34f, h * 0.20f), Size(w * 0.32f, h * 0.24f), style = stroke)
        }
        RawIconType.UNLOCK -> {
            drawRect(tint, Offset(w * 0.22f, h * 0.44f), Size(w * 0.56f, h * 0.42f), style = stroke)
            drawLine(tint, Offset(w * 0.34f, h * 0.44f), Offset(w * 0.34f, h * 0.20f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.34f, h * 0.20f), Offset(w * 0.66f, h * 0.20f), strokeWidth = sw)
        }
        RawIconType.SHARE -> {
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.24f, h * 0.50f))
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.24f))
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.74f, h * 0.76f))
            drawLine(tint, Offset(w * 0.24f, h * 0.50f), Offset(w * 0.74f, h * 0.24f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.24f, h * 0.50f), Offset(w * 0.74f, h * 0.76f), strokeWidth = sw)
        }
        RawIconType.DOWNLOAD -> {
            drawLine(tint, Offset(w * 0.5f, h * 0.16f), Offset(w * 0.5f, h * 0.66f), strokeWidth = sw)
            val p = Path().apply {
                moveTo(w * 0.28f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.68f)
                lineTo(w * 0.72f, h * 0.46f)
            }
            drawPath(p, tint, style = stroke)
            drawLine(tint, Offset(w * 0.18f, h * 0.84f), Offset(w * 0.82f, h * 0.84f), strokeWidth = sw)
        }
        RawIconType.EDIT -> {
            val p = Path().apply {
                moveTo(w * 0.20f, h * 0.80f)
                lineTo(w * 0.26f, h * 0.60f)
                lineTo(w * 0.68f, h * 0.18f)
                lineTo(w * 0.82f, h * 0.32f)
                lineTo(w * 0.40f, h * 0.74f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.MIC -> {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.36f, h * 0.14f),
                size = Size(w * 0.28f, h * 0.44f),
                cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                style = stroke
            )
            drawLine(tint, Offset(w * 0.22f, h * 0.48f), Offset(w * 0.22f, h * 0.68f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.78f, h * 0.48f), Offset(w * 0.78f, h * 0.68f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.22f, h * 0.68f), Offset(w * 0.78f, h * 0.68f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.50f, h * 0.68f), Offset(w * 0.50f, h * 0.86f), strokeWidth = sw)
        }
        RawIconType.CLOCK -> {
            drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.28f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), strokeWidth = sw)
        }
        RawIconType.BOLT -> {
            val p = Path().apply {
                moveTo(w * 0.56f, h * 0.12f)
                lineTo(w * 0.24f, h * 0.54f)
                lineTo(w * 0.50f, h * 0.54f)
                lineTo(w * 0.44f, h * 0.88f)
                lineTo(w * 0.76f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.46f)
                close()
            }
            drawPath(p, tint)
        }
        RawIconType.CAMERA -> {
            drawRect(tint, Offset(w * 0.14f, h * 0.28f), Size(w * 0.72f, h * 0.54f), style = stroke)
            drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.55f), style = stroke)
            drawRect(tint, Offset(w * 0.36f, h * 0.18f), Size(w * 0.28f, h * 0.10f))
        }
        RawIconType.CLOSE -> {
            drawLine(tint, Offset(w * 0.20f, h * 0.20f), Offset(w * 0.80f, h * 0.80f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.80f, h * 0.20f), Offset(w * 0.20f, h * 0.80f), strokeWidth = sw)
        }
        RawIconType.UNDO -> {
            drawLine(tint, Offset(w * 0.20f, h * 0.40f), Offset(w * 0.74f, h * 0.40f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.74f, h * 0.40f), Offset(w * 0.74f, h * 0.74f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.74f, h * 0.74f), Offset(w * 0.36f, h * 0.74f), strokeWidth = sw)
            val p = Path().apply {
                moveTo(w * 0.38f, h * 0.22f)
                lineTo(w * 0.18f, h * 0.40f)
                lineTo(w * 0.38f, h * 0.58f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.REDO -> {
            drawLine(tint, Offset(w * 0.80f, h * 0.40f), Offset(w * 0.26f, h * 0.40f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.26f, h * 0.40f), Offset(w * 0.26f, h * 0.74f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.26f, h * 0.74f), Offset(w * 0.64f, h * 0.74f), strokeWidth = sw)
            val p = Path().apply {
                moveTo(w * 0.62f, h * 0.22f)
                lineTo(w * 0.82f, h * 0.40f)
                lineTo(w * 0.62f, h * 0.58f)
            }
            drawPath(p, tint, style = stroke)
        }
        RawIconType.FORWARD -> {
            val p = Path().apply {
                moveTo(w * 0.22f, h * 0.22f)
                lineTo(w * 0.62f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.78f)
                close()
            }
            drawPath(p, tint)
            drawLine(tint, Offset(w * 0.74f, h * 0.22f), Offset(w * 0.74f, h * 0.78f), strokeWidth = sw)
        }
        RawIconType.BACKWARD -> {
            val p = Path().apply {
                moveTo(w * 0.78f, h * 0.22f)
                lineTo(w * 0.38f, h * 0.50f)
                lineTo(w * 0.78f, h * 0.78f)
                close()
            }
            drawPath(p, tint)
            drawLine(tint, Offset(w * 0.26f, h * 0.22f), Offset(w * 0.26f, h * 0.78f), strokeWidth = sw)
        }
        RawIconType.GRID -> {
            drawRect(tint, Offset(w * 0.16f, h * 0.16f), Size(w * 0.28f, h * 0.28f), style = stroke)
            drawRect(tint, Offset(w * 0.56f, h * 0.16f), Size(w * 0.28f, h * 0.28f), style = stroke)
            drawRect(tint, Offset(w * 0.16f, h * 0.56f), Size(w * 0.28f, h * 0.28f), style = stroke)
            drawRect(tint, Offset(w * 0.56f, h * 0.56f), Size(w * 0.28f, h * 0.28f), style = stroke)
        }
        RawIconType.SHEET -> {
            drawRect(tint, Offset(w * 0.14f, h * 0.16f), Size(w * 0.72f, h * 0.68f), style = stroke)
            drawLine(tint, Offset(w * 0.14f, h * 0.40f), Offset(w * 0.86f, h * 0.40f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.14f, h * 0.62f), Offset(w * 0.86f, h * 0.62f), strokeWidth = sw)
            drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.84f), strokeWidth = sw)
        }
        RawIconType.WARNING -> {
            val p = Path().apply {
                moveTo(w * 0.50f, h * 0.14f)
                lineTo(w * 0.88f, h * 0.84f)
                lineTo(w * 0.12f, h * 0.84f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawLine(tint, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.50f, h * 0.62f), strokeWidth = sw)
            drawRect(tint, Offset(w * 0.46f, h * 0.68f), Size(w * 0.08f, h * 0.08f))
        }
        RawIconType.SPARKLE -> {
            val p = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                lineTo(w * 0.60f, h * 0.40f)
                lineTo(w * 0.90f, h * 0.50f)
                lineTo(w * 0.60f, h * 0.60f)
                lineTo(w * 0.50f, h * 0.90f)
                lineTo(w * 0.40f, h * 0.60f)
                lineTo(w * 0.10f, h * 0.50f)
                lineTo(w * 0.40f, h * 0.40f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
    }
}
