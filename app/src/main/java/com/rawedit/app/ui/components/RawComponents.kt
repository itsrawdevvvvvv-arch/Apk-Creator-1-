package com.rawedit.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.theme.LocalRawMotion
import com.rawedit.app.ui.theme.RawColors
import com.rawedit.app.ui.theme.RawFonts
import kotlin.math.roundToInt

/**
 * Rawdev-inspired pitch-black background with subtle 20dp dot-matrix texture in upper canvas.
 * Zero per-frame allocations.
 */
@Composable
fun DotGridBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RawColors.Background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepPx = 20.dp.toPx()
            val dotRadius = 1.15.dp.toPx()
            val w = size.width
            val h = size.height
            var y = stepPx * 0.5f
            while (y < h) {
                var x = stepPx * 0.5f
                while (x < w) {
                    drawCircle(
                        color = RawColors.DotGridWhite,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    x += stepPx
                }
                y += stepPx
            }
        }
        content()
    }
}

/**
 * EduWave + rawdev stadium pill button with circular trailing icon badge and spring press physics.
 */
@Composable
fun RawButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = true,
    enabled: Boolean = true,
    leadingIcon: RawIconType? = null,
    trailingIcon: RawIconType? = null
) {
    val view = LocalView.current
    val motion = LocalRawMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !motion.reducedMotion) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    val pillShape = RoundedCornerShape(50)
    val bg = when {
        !enabled -> RawColors.Surface
        inverted -> RawColors.TextPrimary
        else -> RawColors.SurfaceElevated
    }
    val fg = when {
        !enabled -> RawColors.TextSecondary
        inverted -> RawColors.Background
        else -> RawColors.TextPrimary
    }

    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pillShape)
            .background(bg)
            .border(1.dp, if (inverted && enabled) RawColors.TextPrimary else RawColors.Divider, pillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                if (motion.hapticsEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (leadingIcon != null) {
                RawIcon(type = leadingIcon, tint = fg, size = 17.dp, isActive = isPressed)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (inverted) RawColors.Background else RawColors.TextPrimary),
                contentAlignment = Alignment.Center
            ) {
                RawIcon(
                    type = trailingIcon,
                    tint = if (inverted) RawColors.TextPrimary else RawColors.Background,
                    size = 14.dp,
                    isActive = isPressed
                )
            }
        }
    }
}

enum class MainDockTab(val route: String, val label: String, val icon: RawIconType) {
    HOME("home", "HOME", RawIconType.HOME),
    CREATE("create", "CREATE", RawIconType.PLUS),
    MY_REELS("my_reels", "REELS", RawIconType.FILM),
    PROFILE("profile", "PROFILE", RawIconType.PERSON)
}

/**
 * Floating stadium bottom dock modeled directly after the "Hello again, rawdev!" bottom bar:
 * #161616 stadium container with a gliding #2E2E2E rounded pill indicator.
 */
@Composable
fun LiquidBottomDock(
    currentRoute: String,
    onSelectTab: (MainDockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val motion = LocalRawMotion.current
    val tabs = MainDockTab.entries
    val selectedIndex = tabs.indexOfFirst { currentRoute.startsWith(it.route) }.coerceAtLeast(0)
    val dockShape = RoundedCornerShape(32.dp)
    val pillShape = RoundedCornerShape(26.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .height(64.dp)
            .clip(dockShape)
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider, dockShape)
    ) {
        val tabWidth: Dp = maxWidth / tabs.size
        val pillOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "liquidPillOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(6.dp)
                .clip(pillShape)
                .background(RawColors.DockActivePill)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { idx, tab ->
                val isSelected = idx == selectedIndex
                val fg = if (isSelected) RawColors.TextPrimary else RawColors.TextSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (motion.hapticsEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            onSelectTab(tab)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RawIcon(
                        type = tab.icon,
                        tint = fg,
                        size = 20.dp,
                        isActive = isSelected
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp
                        ),
                        color = fg
                    )
                }
            }
        }
    }
}

/**
 * Windowed read-only monospace code viewer inside a 20dp rounded charcoal card.
 */
@Composable
fun WindowedCodeViewer(
    code: String,
    modifier: Modifier = Modifier
) {
    val lines = remember(code) {
        if (code.isEmpty()) listOf("// Empty HTML buffer") else code.lines()
    }
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RawColors.SurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WINDOWED BUFFER · ${lines.size} LINES",
                style = MaterialTheme.typography.labelSmall,
                color = RawColors.TextSecondary
            )
            Text(
                text = "${code.length} CHARS",
                style = MaterialTheme.typography.labelSmall,
                color = RawColors.TextPrimary
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            itemsIndexed(lines, key = { idx, _ -> idx }) { idx, line ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = String.format("%04d ", idx + 1),
                        fontFamily = RawFonts.Mono,
                        fontSize = 11.sp,
                        color = RawColors.TextSecondary
                    )
                    Text(
                        text = if (line.length > 240) line.take(240) + "…" else line,
                        fontFamily = RawFonts.Mono,
                        fontSize = 11.sp,
                        color = RawColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

/**
 * EduWave "Swipe to unlock >>> $126" stadium pill slider adapted in rawdev monochrome
 * for starting the 1080p30 render pipeline ("Swipe to render >>> 1080p30").
 */
@Composable
fun SwipeToConfirmSlider(
    label: String,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val motion = LocalRawMotion.current
    val density = LocalDensity.current
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val pillShape = RoundedCornerShape(50)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(pillShape)
            .background(RawColors.SurfaceElevated)
            .border(1.dp, RawColors.Divider, pillShape)
    ) {
        val thumbSize = 48.dp
        val maxTravelPx = with(density) { (maxWidth - thumbSize - 12.dp).toPx().coerceAtLeast(1f) }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(dragProgress.coerceIn(0f, 1f))
                .background(Color(0x28FFFFFF))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 66.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = RawColors.TextPrimary
            )
            Text(
                text = "1080p30",
                style = MaterialTheme.typography.titleMedium,
                color = RawColors.TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .padding(6.dp)
                .offset { IntOffset((dragProgress * maxTravelPx).roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(RawColors.TextPrimary)
                .pointerInput(maxTravelPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragProgress >= 0.85f) {
                                if (motion.hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                dragProgress = 0f
                                onConfirmed()
                            } else {
                                dragProgress = 0f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragProgress = ((dragProgress * maxTravelPx + dragAmount) / maxTravelPx).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            RawIcon(
                type = RawIconType.LOCK,
                tint = RawColors.Background,
                size = 18.dp
            )
        }
    }
}


/**
 * Custom Compose Canvas renderer for the official `raw_edit` tilted 3D retro CRT TV logo
 * (bent rabbit-ear antennas, dome base, 2 rotary tuner knobs, 4 speaker louver vents, and
 * pixel "RAW / EDIT" screen).
 */
@Composable
fun RawAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = -8f }
    ) {
        val w = this.size.width
        val h = this.size.height
        // Rabbit-ear antennas
        drawLine(Color(0xFFD0D0D0), Offset(w * 0.45f, h * 0.24f), Offset(w * 0.30f, h * 0.12f), strokeWidth = w * 0.04f)
        drawLine(Color(0xFFD0D0D0), Offset(w * 0.58f, h * 0.24f), Offset(w * 0.76f, h * 0.12f), strokeWidth = w * 0.04f)
        drawCircle(Color.White, radius = w * 0.035f, center = Offset(w * 0.30f, h * 0.12f))
        drawCircle(Color.White, radius = w * 0.035f, center = Offset(w * 0.76f, h * 0.12f))
        // Outer CRT Cabinet
        drawRoundRect(
            color = Color(0xFF242424),
            topLeft = Offset(w * 0.12f, h * 0.24f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.62f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Inner dark CRT screen
        drawRoundRect(
            color = Color(0xFF0A0A0A),
            topLeft = Offset(w * 0.17f, h * 0.30f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.50f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f, w * 0.04f)
        )
        // Two rotary tuner knobs on right column
        drawCircle(Color(0xFF9A9A9A), radius = w * 0.065f, center = Offset(w * 0.74f, h * 0.44f))
        drawCircle(Color(0xFF9A9A9A), radius = w * 0.065f, center = Offset(w * 0.74f, h * 0.60f))
        // Speaker vents
        drawRect(Color(0xFF787878), topLeft = Offset(w * 0.68f, h * 0.70f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.025f))
        drawRect(Color(0xFF787878), topLeft = Offset(w * 0.68f, h * 0.75f), size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.025f))
        // Pixel RAW / EDIT bars inside screen
        drawRect(Color.White, topLeft = Offset(w * 0.22f, h * 0.38f), size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.12f))
        drawRect(Color.White, topLeft = Offset(w * 0.22f, h * 0.56f), size = androidx.compose.ui.geometry.Size(w * 0.34f, h * 0.12f))
    }
}
