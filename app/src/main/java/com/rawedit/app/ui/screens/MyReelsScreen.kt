package com.rawedit.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rawedit.app.data.ProjectEntity
import com.rawedit.app.data.ProjectStatus
import com.rawedit.app.domain.SrtParser
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.components.RawButton
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.theme.RawColors

enum class ReelFilterChip(val label: String) {
    ALL("ALL"),
    DRAFTS("DRAFTS"),
    RENDERING("RENDERING"),
    DONE("DONE")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyReelsScreen(
    projects: List<ProjectEntity>,
    onStartCreating: () -> Unit,
    onOpenProject: (ProjectEntity) -> Unit,
    onOpenResult: (ProjectEntity) -> Unit,
    onOpenKeyframeSheet: (ProjectEntity) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(ReelFilterChip.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var actionTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameTarget by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    val filteredProjects by remember(projects, selectedFilter, searchQuery) {
        derivedStateOf {
            projects.filter { proj ->
                val matchesFilter = when (selectedFilter) {
                    ReelFilterChip.ALL -> true
                    ReelFilterChip.DRAFTS -> proj.status == ProjectStatus.DRAFT.name || proj.status == ProjectStatus.READY.name
                    ReelFilterChip.RENDERING -> proj.status == ProjectStatus.RENDERING.name
                    ReelFilterChip.DONE -> proj.status == ProjectStatus.DONE.name
                }
                val matchesQuery = searchQuery.isBlank() || proj.title.contains(searchQuery.trim(), ignoreCase = true)
                matchesFilter && matchesQuery
            }
        }
    }

    DotGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // EduWave-style top greeting + avatar badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MY REELS // ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                    Text(
                        text = "Projects & Renders",
                        style = MaterialTheme.typography.headlineLarge,
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
                    AnimatedPixelMascot(mascotId = MascotId.FLOPPY_GHOST, size = 24.dp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rounded Stadium Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search project titles...",
                        style = MaterialTheme.typography.labelMedium,
                        color = RawColors.TextSecondary
                    )
                },
                leadingIcon = {
                    RawIcon(type = RawIconType.FILM, size = 18.dp, tint = RawColors.TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.clickable { searchQuery = "" }.padding(8.dp)) {
                            RawIcon(type = RawIconType.CLOSE, size = 16.dp)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RawColors.TextPrimary,
                    unfocusedBorderColor = RawColors.Divider,
                    focusedContainerColor = RawColors.Surface,
                    unfocusedContainerColor = RawColors.Surface,
                    focusedTextColor = RawColors.TextPrimary,
                    unfocusedTextColor = RawColors.TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // EduWave-style Stadium Filter Chips with inline count circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReelFilterChip.entries.forEach { chip ->
                    val active = chip == selectedFilter
                    val count = when (chip) {
                        ReelFilterChip.ALL -> projects.size
                        ReelFilterChip.DRAFTS -> projects.count { it.status == ProjectStatus.DRAFT.name || it.status == ProjectStatus.READY.name }
                        ReelFilterChip.RENDERING -> projects.count { it.status == ProjectStatus.RENDERING.name }
                        ReelFilterChip.DONE -> projects.count { it.status == ProjectStatus.DONE.name }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) RawColors.TextPrimary else RawColors.Surface)
                            .border(1.dp, if (active) RawColors.TextPrimary else RawColors.Divider, RoundedCornerShape(50))
                            .clickable { selectedFilter = chip }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chip.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) RawColors.Background else RawColors.TextSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) RawColors.SurfaceElevated else RawColors.PillSurface)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = RawColors.TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredProjects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(RawColors.Surface)
                        .border(1.dp, RawColors.Divider, RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedPixelMascot(
                        mascotId = MascotId.PHANTOM_TV,
                        size = 72.dp,
                        drawShadow = true
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "NO REELS IN THIS CHANNEL YET.",
                        style = MaterialTheme.typography.titleMedium,
                        color = RawColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    RawButton(
                        label = "START CREATING",
                        onClick = onStartCreating,
                        leadingIcon = RawIconType.PLUS,
                        trailingIcon = RawIconType.ARROW_RIGHT
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { proj ->
                        val mascotForCard = remember(proj.id) {
                            val idx = (proj.id.hashCode() and 0x7FFFFFFF) % MascotId.entries.size
                            MascotId.entries[idx]
                        }
                        val cardShape = RoundedCornerShape(24.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(RawColors.SurfaceElevated)
                                .border(1.dp, RawColors.Divider, cardShape)
                                .combinedClickable(
                                    onClick = {
                                        if (proj.status == ProjectStatus.DONE.name) {
                                            onOpenResult(proj)
                                        } else {
                                            onOpenProject(proj)
                                        }
                                    },
                                    onLongClick = { actionTarget = proj }
                                )
                                .padding(12.dp)
                        ) {
                            // Top status pill + sheet icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (proj.status == ProjectStatus.DONE.name) RawColors.TextPrimary else RawColors.PillSurface)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = proj.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (proj.status == ProjectStatus.DONE.name) RawColors.Background else RawColors.TextPrimary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (proj.lastSheetId.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(RawColors.Surface)
                                                .clickable { onOpenKeyframeSheet(proj) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            RawIcon(type = RawIconType.SHEET, size = 13.dp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(RawColors.Surface)
                                            .clickable { actionTarget = proj },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        RawIcon(type = RawIconType.EDIT, size = 13.dp, tint = RawColors.TextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 16:9 Thumbnail preview inside rounded frame
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(RawColors.Background),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedPixelMascot(
                                    mascotId = mascotForCard,
                                    size = 32.dp,
                                    alpha = 0.90f
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = RawColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${SrtParser.formatTimecode(proj.audioDurationMs)} · 1080p/30",
                                style = MaterialTheme.typography.labelSmall,
                                color = RawColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // EduWave nested dark stadium action pill at bottom of card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(RawColors.Background)
                                    .padding(start = 12.dp, end = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (proj.status == ProjectStatus.DONE.name) "Play Reel" else "Resume",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RawColors.TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(RawColors.TextPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RawIcon(
                                        type = RawIconType.PLAY,
                                        tint = RawColors.Background,
                                        size = 12.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            containerColor = RawColors.Surface,
            title = {
                Text(target.title, style = MaterialTheme.typography.titleMedium, color = RawColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RawButton(
                        label = "RESUME IN EDITOR",
                        onClick = {
                            actionTarget = null
                            onOpenProject(target)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = true,
                        leadingIcon = RawIconType.EDIT,
                        trailingIcon = RawIconType.ARROW_RIGHT
                    )
                    if (target.lastSheetId.isNotBlank()) {
                        RawButton(
                            label = "OPEN KEYFRAME SHEET",
                            onClick = {
                                actionTarget = null
                                onOpenKeyframeSheet(target)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            inverted = false,
                            leadingIcon = RawIconType.SHEET
                        )
                    }
                    RawButton(
                        label = "RENAME PROJECT",
                        onClick = {
                            actionTarget = null
                            renameText = target.title
                            renameTarget = target
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.EDIT
                    )
                    RawButton(
                        label = "DUPLICATE PROJECT",
                        onClick = {
                            actionTarget = null
                            onDuplicateProject(target.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.COPY
                    )
                    RawButton(
                        label = "DELETE PROJECT",
                        onClick = {
                            actionTarget = null
                            deleteTarget = target
                        },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.TRASH
                    )
                }
            },
            confirmButton = {}
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = RawColors.Surface,
            title = { Text("RENAME REEL", style = MaterialTheme.typography.titleMedium, color = RawColors.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                RawButton(
                    label = "SAVE",
                    onClick = {
                        onRenameProject(target.id, renameText)
                        renameTarget = null
                    }
                )
            },
            dismissButton = {
                RawButton(label = "CANCEL", inverted = false, onClick = { renameTarget = null })
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = RawColors.Surface,
            title = { Text("CONFIRM DELETE", style = MaterialTheme.typography.titleMedium, color = RawColors.ErrorCrimson) },
            text = {
                Text(
                    text = "Permanently delete '${target.title}' and its generated keyframes/renders?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RawColors.TextPrimary
                )
            },
            confirmButton = {
                RawButton(
                    label = "DELETE",
                    onClick = {
                        onDeleteProject(target.id)
                        deleteTarget = null
                    },
                    leadingIcon = RawIconType.TRASH
                )
            },
            dismissButton = {
                RawButton(label = "CANCEL", inverted = false, onClick = { deleteTarget = null })
            }
        )
    }
}
