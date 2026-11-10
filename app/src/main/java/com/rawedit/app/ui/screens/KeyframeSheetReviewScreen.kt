package com.rawedit.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.rawedit.app.data.KeyframeTileMeta
import com.rawedit.app.ui.CreateFlowUiState
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.components.RawButton
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.theme.RawColors
import java.io.File

/**
 * §6.3 Stage 3 — In-app paged viewer of the pre-render keyframe contact sheet
 * (12 tiles per page, pinch-to-zoom, ALL/FLAGGED filter, tap a tile to inspect or jump to frame).
 */
@Composable
fun KeyframeSheetReviewScreen(
    state: CreateFlowUiState,
    onJumpToTimestampMs: (Long) -> Unit,
    onCopyFixPrompt: () -> Unit,
    onSharePdf: () -> Unit,
    onShareZip: () -> Unit,
    onBack: () -> Unit
) {
    var showFlaggedOnly by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var selectedTile by remember { mutableStateOf<KeyframeTileMeta?>(null) }

    val filteredTiles by remember(state.sheetTiles, showFlaggedOnly) {
        derivedStateOf {
            if (showFlaggedOnly) {
                state.sheetTiles.filter { it.status != "CLEAN" }
            } else {
                state.sheetTiles
            }
        }
    }

    val totalPages = ((filteredTiles.size + 11) / 12).coerceAtLeast(1)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageTiles = remember(filteredTiles, safePage) {
        filteredTiles.drop(safePage * 12).take(12)
    }

    DotGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, RawColors.Divider)
                            .background(RawColors.Surface)
                            .clickable { onBack() }
                            .padding(10.dp)
                    ) {
                        RawIcon(type = RawIconType.BACK, size = 18.dp)
                    }
                    Column {
                        Text(
                            text = "STAGE 3 // CONTACT SHEET REVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = RawColors.TextSecondary
                        )
                        Text(
                            text = "${filteredTiles.size} TILES · PAGE ${safePage + 1}/$totalPages",
                            style = MaterialTheme.typography.titleMedium,
                            color = RawColors.TextPrimary
                        )
                    }
                }
                AnimatedPixelMascot(mascotId = MascotId.CURSOR_WALKER, size = 28.dp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row: ALL · FLAGGED + Share buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RawButton(
                    label = "ALL (${state.sheetTiles.size})",
                    onClick = {
                        showFlaggedOnly = false
                        currentPage = 0
                    },
                    modifier = Modifier.weight(1f),
                    inverted = !showFlaggedOnly
                )
                RawButton(
                    label = "FLAGGED (${state.sheetTiles.count { it.status != "CLEAN" }})",
                    onClick = {
                        showFlaggedOnly = true
                        currentPage = 0
                    },
                    modifier = Modifier.weight(1f),
                    inverted = showFlaggedOnly
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 12-per-page LazyVerticalGrid with pinch-to-zoom
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(1f, 2.5f)
                        }
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                        },
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageTiles, key = { it.index }) { tile ->
                        val bmp = remember(tile.imagePath) {
                            val f = File(tile.imagePath)
                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                        }
                        val borderColor = when (tile.status) {
                            "ERROR" -> RawColors.ErrorCrimson
                            "WARN" -> RawColors.WarningAmber
                            else -> RawColors.Divider
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RawColors.Surface)
                                .border(1.dp, borderColor)
                                .clickable { selectedTile = tile }
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = tile.tileId,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 10.8f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(tile.timecode, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Pager + Export controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RawButton(
                    label = "PREV",
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = safePage > 0,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.BACKWARD
                )
                RawButton(
                    label = "PDF",
                    onClick = onSharePdf,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.SHARE
                )
                RawButton(
                    label = "ZIP",
                    onClick = onShareZip,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.DOWNLOAD
                )
                RawButton(
                    label = "NEXT",
                    onClick = { if (currentPage < totalPages - 1) currentPage++ },
                    enabled = safePage < totalPages - 1,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    trailingIcon = RawIconType.FORWARD
                )
            }
        }
    }

    // Tile flag detail modal
    selectedTile?.let { tile ->
        AlertDialog(
            onDismissRequest = { selectedTile = null },
            containerColor = RawColors.Surface,
            title = {
                Text(
                    text = "${tile.tileId} · ${tile.timecode} · F${tile.frameIndex}",
                    style = MaterialTheme.typography.titleMedium,
                    color = RawColors.TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "STATUS: ${tile.status} (${tile.primaryFlag})",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (tile.status == "CLEAN") RawColors.TextPrimary else RawColors.WarningAmber
                    )
                    if (tile.flags.isEmpty()) {
                        Text(
                            text = "No DOM overflow or contrast violations on this frame.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        tile.flags.forEach { fl ->
                            Text(
                                text = "• [${fl.rule}] ${fl.detail}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RawColors.WarningAmber
                            )
                            if (fl.domSnapshot.isNotBlank()) {
                                Text(
                                    text = "DOM: ${fl.domSnapshot}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RawColors.TextSecondary
                                )
                            }
                        }
                    }
                    RawButton(
                        label = "JUMP TO FRAME IN LIVE PREVIEW",
                        onClick = {
                            val targetMs = tile.timeMs
                            selectedTile = null
                            onJumpToTimestampMs(targetMs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = true,
                        leadingIcon = RawIconType.PLAY
                    )
                    RawButton(
                        label = "ADD TO / COPY AI FIX PROMPT",
                        onClick = {
                            onCopyFixPrompt()
                            selectedTile = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.COPY
                    )
                }
            },
            confirmButton = {
                RawButton(label = "CLOSE", onClick = { selectedTile = null })
            }
        )
    }
}
