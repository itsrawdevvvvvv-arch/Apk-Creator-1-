package com.rawedit.app.ui.mascots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rawedit.app.ui.theme.RawColors

/**
 * Pre-compiled pixel sprite frame.
 * To guarantee zero per-frame garbage collection on low-end Android 9 devices, horizontal runs of
 * identical palette pixels (`#FFFFFF` white or `#8A8A8A` shading) are pre-packed into primitive
 * `IntArray` spans once at creation time.
 */
@Immutable
class PixelSprite(
    val rows: List<String>,
    val width: Int = rows.firstOrNull()?.length ?: 16,
    val height: Int = rows.size
) {
    // Packed spans: [y, xStart, runLength, colorType (1=White, 2=Shade)]
    val packedSpans: IntArray

    init {
        val temp = ArrayList<Int>(width * height * 2)
        for (y in rows.indices) {
            val row = rows[y]
            var x = 0
            while (x < row.length) {
                val ch = row[x]
                val type = when (ch) {
                    '#', 'W', '1' -> 1
                    '+', 'S', '2' -> 2
                    else -> 0
                }
                if (type == 0) {
                    x++
                    continue
                }
                var run = 1
                while (x + run < row.length) {
                    val nextCh = row[x + run]
                    val nextType = when (nextCh) {
                        '#', 'W', '1' -> 1
                        '+', 'S', '2' -> 2
                        else -> 0
                    }
                    if (nextType == type) run++ else break
                }
                temp.add(y)
                temp.add(x)
                temp.add(run)
                temp.add(type)
                x += run
            }
        }
        packedSpans = temp.toIntArray()
    }
}

@Composable
fun PixelSpriteCanvas(
    sprite: PixelSprite,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    primaryColor: Color = RawColors.TextPrimary,
    shadeColor: Color = RawColors.TextSecondary,
    drawShadowGrid: Boolean = false,
    alpha: Float = 1f
) {
    val whitePainted = remember(primaryColor, alpha) { primaryColor.copy(alpha = alpha) }
    val shadePainted = remember(shadeColor, alpha) { shadeColor.copy(alpha = alpha * 0.72f) }
    val shadowPainted = remember(alpha) { Color(0xFF2A2A2A).copy(alpha = alpha * 0.55f) }

    Canvas(modifier = modifier.size(size)) {
        val cellW = this.size.width / sprite.width.toFloat()
        val cellH = this.size.height / sprite.height.toFloat()
        val spans = sprite.packedSpans

        if (drawShadowGrid) {
            var i = 0
            while (i < spans.size) {
                val y = spans[i]
                val x = spans[i + 1]
                val run = spans[i + 2]
                drawRect(
                    color = shadowPainted,
                    topLeft = Offset((x + 1) * cellW, (y + 1) * cellH),
                    size = Size(run * cellW, cellH)
                )
                i += 4
            }
        }

        var i = 0
        while (i < spans.size) {
            val y = spans[i]
            val x = spans[i + 1]
            val run = spans[i + 2]
            val type = spans[i + 3]
            drawRect(
                color = if (type == 1) whitePainted else shadePainted,
                topLeft = Offset(x * cellW, y * cellH),
                size = Size(run * cellW + 0.35f, cellH + 0.35f)
            )
            i += 4
        }
    }
}
