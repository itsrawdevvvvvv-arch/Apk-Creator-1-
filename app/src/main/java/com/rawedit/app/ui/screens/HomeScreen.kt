package com.rawedit.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rawedit.app.data.ProjectEntity
import com.rawedit.app.data.ProjectStatus
import com.rawedit.app.data.VideoEntity
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.mascots.MascotRain
import com.rawedit.app.ui.mascots.MascotTicker
import com.rawedit.app.ui.theme.LocalRawMotion
import com.rawedit.app.ui.theme.RawColors

/**
 * §4.1 Home — Combines EduWave's rounded hero-card interface with the "Hello again, rawdev!"
 * charcoal monochrome palette and giant split-counter telemetry card.
 * Contains ZERO project/draft/reel lists — only the typographic hero, one primary CTA, and counter.
 */
@Composable
fun HomeScreen(
    projects: List<ProjectEntity>,
    videos: List<VideoEntity>,
    onStartCreating: () -> Unit,
    onOpenMyReels: () -> Unit
) {
    val motion = LocalRawMotion.current
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val heroReveal by animateFloatAsState(
        targetValue = if (entered || motion.reducedMotion) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heroReveal"
    )

    val doneCount = remember(projects, videos) {
        maxOf(videos.size, projects.count { it.status == ProjectStatus.DONE.name })
    }
    val draftCount = remember(projects) {
        projects.count { it.status != ProjectStatus.DONE.name }
    }

    DotGridBackground {
        MascotRain(count = 5, alpha = 0.14f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Greeting Header (EduWave + rawdev style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RAW_EDIT // OFFLINE ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hello again, creator!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RawColors.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RawColors.SurfaceElevated)
                        .border(1.dp, RawColors.Divider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedPixelMascot(mascotId = MascotId.CRT_BOT, size = 24.dp)
                }
            }

            // EduWave-style 28dp Rounded Hero Feature Card with Typography Poster + Stadium CTA Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = heroReveal
                        translationY = (1f - heroReveal) * 32f
                    }
                    .clip(RoundedCornerShape(28.dp))
                    .background(RawColors.SurfaceElevated)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(RawColors.PillSurface)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "LAYER A · 1080P/30 OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = RawColors.TextPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimatedPixelMascot(mascotId = MascotId.VOLT_BOLT, size = 24.dp)
                        AnimatedPixelMascot(mascotId = MascotId.CASSETTE_PAL, size = 24.dp)
                        AnimatedPixelMascot(mascotId = MascotId.TINY_UFO, size = 24.dp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "YOUR
VOICE
BECOMES
TYPE.",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        lineHeight = 49.sp
                    ),
                    color = RawColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                MascotTicker(alpha = 0.45f)

                Spacer(modifier = Modifier.height(14.dp))

                // EduWave nested dark stadium CTA pill ("Start Creating" + circular white play button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(50))
                        .background(RawColors.Background)
                        .border(1.dp, RawColors.Divider, RoundedCornerShape(50))
                        .clickable { onStartCreating() }
                        .padding(start = 22.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "START CREATING",
                        style = MaterialTheme.typography.labelLarge,
                        color = RawColors.TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(RawColors.TextPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        RawIcon(
                            type = RawIconType.PLAY,
                            tint = RawColors.Background,
                            size = 16.dp
                        )
                    }
                }
            }

            // "Hello again, rawdev!" 24h | 0m style quiet counter card (tapping opens My Reels)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(24.dp))
                    .clickable { onOpenMyReels() }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$doneCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = RawColors.TextPrimary
                        )
                        Text(
                            text = "reels",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "|",
                            style = MaterialTheme.typography.headlineLarge,
                            color = RawColors.Divider
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "$draftCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = RawColors.TextPrimary
                        )
                        Text(
                            text = "drafts",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Offline Render",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RawColors.TextSecondary
                        )
                        Text(
                            text = "1080p / 30fps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RawColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = RawColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MY REELS →",
                        style = MaterialTheme.typography.labelMedium,
                        color = RawColors.TextSecondary
                    )
                }
            }
        }
    }
}
