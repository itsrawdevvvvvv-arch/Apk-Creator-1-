package com.rawedit.app.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rawedit.app.data.CacheManager
import com.rawedit.app.data.UserPrefsState
import com.rawedit.app.domain.DeviceBudgetProfile
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.components.RawButton
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.mascots.MascotLibrary
import com.rawedit.app.ui.theme.RawColors
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * §4.5 Onboarding modeled after EduWave's onboarding screen (floating illustration elements in the
 * upper dark canvas + 32dp rounded bottom card with headline, subtitle, pill page dots, and
 * "Next ( > )" stadium button), followed by Profile Setup (512px circle avatar cropper).
 */
@Composable
fun OnboardingScreen(
    cacheManager: CacheManager,
    onComplete: (String, String) -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("rawdev") }
    var avatarPath by remember { mutableStateOf("") }
    var cropZoom by remember { mutableFloatStateOf(1f) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            scope.launch {
                cacheManager.importAndCropAvatar512(uri, cropZoom, 0f, 0f).onSuccess { path ->
                    avatarPath = path
                }
            }
        }
    }

    val pages = listOf(
        Triple(
            MascotId.CRT_BOT,
            "Turn voiceovers into kinetic reels!",
            "100% offline 1920×1080 @ 30fps typography engine that runs smoothly in airplane mode."
        ),
        Triple(
            MascotId.SHELL_SNAIL,
            "Inspect every frame before encoding!",
            "Layer A live DOM autofit & timestamped PDF/ZIP keyframe contact sheets catch overflow early."
        ),
        Triple(
            MascotId.VOLT_BOLT,
            "Zero cloud lock-in, pure copy-paste!",
            "Copy layout-guarded prompts out, paste AI HTML back in, and render MP4 directly on your phone."
        )
    )

    DotGridBackground {
        if (page < 3) {
            val (mascot, title, subtitle) = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(RawColors.SurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        RawIcon(type = RawIconType.PLAY, size = 14.dp)
                    }
                    Text(
                        text = "raw_edit",
                        style = MaterialTheme.typography.headlineMedium,
                        color = RawColors.TextPrimary
                    )
                }

                // Floating pixel art cluster in upper canvas (EduWave style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(RawColors.Surface)
                            .border(1.dp, RawColors.Divider, RoundedCornerShape(24.dp))
                            .padding(28.dp)
                    ) {
                        AnimatedPixelMascot(mascotId = mascot, size = 92.dp, drawShadow = true)
                    }
                }

                // EduWave 32dp rounded bottom sheet card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(RawColors.Surface)
                        .border(1.dp, RawColors.Divider, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .padding(horizontal = 26.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = RawColors.TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = RawColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom row: page dots (━━ • •) + Next ( > ) stadium pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 0..2) {
                                Box(
                                    modifier = Modifier
                                        .height(7.dp)
                                        .width(if (i == page) 22.dp else 7.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (i == page) RawColors.TextPrimary else RawColors.Divider)
                                )
                            }
                        }

                        RawButton(
                            label = "Next",
                            onClick = { page++ },
                            inverted = true,
                            trailingIcon = RawIconType.ARROW_RIGHT
                        )
                    }
                }
            }
        } else {
            // Page 4: Operator Profile Setup (512px circle cropper)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RAW_EDIT // PROFILE PASS SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = RawColors.TextSecondary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(RawColors.SurfaceElevated)
                        .border(1.dp, RawColors.Divider, RoundedCornerShape(28.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(RawColors.Background)
                            .border(2.dp, RawColors.TextPrimary, CircleShape)
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = remember(avatarPath) {
                            if (avatarPath.isNotBlank() && File(avatarPath).exists()) {
                                BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
                            } else null
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AnimatedPixelMascot(mascotId = MascotId.PIXEL_CAT, size = 48.dp)
                        }
                    }

                    if (pendingUri != null) {
                        Text("CIRCLE CROP ZOOM (${String.format("%.1fx", cropZoom)})", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = cropZoom,
                            onValueChange = { z ->
                                cropZoom = z
                                val u = pendingUri
                                if (u != null) {
                                    scope.launch {
                                        cacheManager.importAndCropAvatar512(u, z, 0f, 0f).onSuccess {
                                            avatarPath = it
                                        }
                                    }
                                }
                            },
                            valueRange = 1f..3f
                        )
                    }

                    RawButton(
                        label = "CHOOSE AVATAR IMAGE",
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.CAMERA
                    )

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("ID CARD HANDLE", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RawColors.TextPrimary,
                            unfocusedBorderColor = RawColors.Divider,
                            focusedContainerColor = RawColors.Surface,
                            unfocusedContainerColor = RawColors.Surface
                        )
                    )
                }

                RawButton(
                    label = "ENTER RAW_EDIT STUDIO",
                    onClick = { onComplete(userName, avatarPath) },
                    modifier = Modifier.fillMaxWidth(),
                    inverted = true,
                    trailingIcon = RawIconType.ARROW_RIGHT
                )
            }
        }
    }
}

/**
 * §4.5 Profile Screen modeled directly after "Hello again, rawdev!" (`260248.jpg`):
 * - Greeting "Hello again, <userName>!"
 * - Horizontal ID Card Pass (`PROFILE USR#50841819894`, circular avatar, `ID CARD OF`, handle,
 *   `joined at ...`, and vertical right-edge strip `CARD • rawdev's`)
 * - Giant split stat card (`12r | 4d` / `Total Reels & Drafts`)
 * - Version & engine card (`2.0.0` + `Offline` pill button)
 * - Rounded pill rows for Haptics, Reduced Motion, Low-End Device Turbo Mode, and the 16 pixel mascots.
 */
@Composable
fun ProfileScreen(
    prefs: UserPrefsState,
    deviceProfile: DeviceBudgetProfile,
    projectCount: Int,
    renderCount: Int,
    cacheManager: CacheManager,
    onUpdateProfile: (String, String) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleReducedMotion: (Boolean) -> Unit,
    onToggleUltraLowEnd: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showEditHandleDialog by remember { mutableStateOf(false) }
    var editingName by remember(prefs.userName) { mutableStateOf(prefs.userName) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                cacheManager.importAndCropAvatar512(uri, 1f, 0f, 0f).onSuccess { path ->
                    onUpdateProfile(editingName, path)
                }
            }
        }
    }

    val joinDateStr = remember(prefs.joinDateMs) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(if (prefs.joinDateMs > 0L) prefs.joinDateMs else System.currentTimeMillis()))
    }

    DotGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Play emblem + "Hello again, rawdev!"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RawColors.SurfaceElevated)
                        .border(1.dp, RawColors.TextPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    RawIcon(type = RawIconType.PLAY, size = 15.dp)
                }
                Text(
                    text = "Hello again, ${prefs.userName}!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = RawColors.TextPrimary
                )
            }

            // "Hello again, rawdev!" ID CARD PASS (#262626 rounded 26dp card with vertical right badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(RawColors.SurfaceElevated)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PROFILE USR#50841819894",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextSecondary
                        )
                        RawIcon(type = RawIconType.EDIT, size = 12.dp, tint = RawColors.TextSecondary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(RawColors.Background)
                                .border(2.dp, RawColors.TextPrimary, CircleShape)
                                .clickable { avatarPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = remember(prefs.avatarPath) {
                                if (prefs.avatarPath.isNotBlank() && File(prefs.avatarPath).exists()) {
                                    BitmapFactory.decodeFile(prefs.avatarPath)?.asImageBitmap()
                                } else null
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AnimatedPixelMascot(mascotId = MascotId.CRT_BOT, size = 42.dp)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "ID CARD OF",
                                style = MaterialTheme.typography.labelSmall,
                                color = RawColors.TextSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { showEditHandleDialog = true }
                            ) {
                                Text(
                                    text = prefs.userName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = RawColors.TextPrimary
                                )
                                RawIcon(type = RawIconType.EDIT, size = 14.dp, tint = RawColors.TextSecondary)
                            }
                            Text(
                                text = "joined at $joinDateStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RawColors.TextPrimary
                            )
                        }
                    }
                }

                // Right vertical pass strip ("CARD • rawdev's")
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(148.dp)
                        .background(RawColors.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CARD · ${prefs.userName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary,
                        modifier = Modifier.graphicsLayer { rotationZ = 90f }
                    )
                }
            }

            // "24h | 0m" style giant stats card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$renderCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = RawColors.TextPrimary
                        )
                        Text(
                            text = "mp4",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextPrimary,
                            modifier = Modifier.padding(start = 3.dp, bottom = 5.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("|", style = MaterialTheme.typography.headlineLarge, color = RawColors.Divider)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$projectCount",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = RawColors.TextPrimary
                        )
                        Text(
                            text = "proj",
                            style = MaterialTheme.typography.labelMedium,
                            color = RawColors.TextPrimary,
                            modifier = Modifier.padding(start = 3.dp, bottom = 5.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Rendered", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextSecondary)
                        Text("& Active Reels", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = RawColors.Divider)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RAM ${deviceProfile.totalRamMb}MB · ${deviceProfile.cpuCores} Cores · API 28+ Smooth Engine  >",
                    style = MaterialTheme.typography.labelSmall,
                    color = RawColors.TextSecondary
                )
            }

            // "Version & about app" 8.3.2-style card -> 2.0.0 Offline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(24.dp))
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Version & offline build",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RawColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2.0.0",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = RawColors.TextPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(RawColors.PillSurface)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "100% Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = RawColors.TextPrimary
                    )
                }
            }

            // Rounded pill toggle rows ("meet da dev" pill style)
            TogglePillRow(
                mascot = MascotId.VOLT_BOLT,
                title = "Haptic feedback ticks",
                checked = prefs.hapticsEnabled,
                onCheckedChange = onToggleHaptics
            )
            TogglePillRow(
                mascot = MascotId.FLOPPY_GHOST,
                title = "Reduced motion (freeze frame 0)",
                checked = prefs.reducedMotion,
                onCheckedChange = onToggleReducedMotion
            )
            TogglePillRow(
                mascot = MascotId.ARCADE_STICK,
                title = "Low-end Android 9 turbo mode",
                checked = prefs.ultraLowEndMode,
                onCheckedChange = onToggleUltraLowEnd
            )

            // 16 Procedural White Pixel Mascot Roster Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RawColors.Surface)
                    .border(1.dp, RawColors.Divider, RoundedCornerShape(24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PROCEDURAL PIXEL MASCOT ROSTER (16/16 ACTIVE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = RawColors.TextSecondary
                )
                val chunks = remember { MascotLibrary.ALL.chunked(4) }
                chunks.forEach { rowMascots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowMascots.forEach { def ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RawColors.Background)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                AnimatedPixelMascot(mascotId = def.id, size = 26.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = def.id.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RawColors.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditHandleDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEditHandleDialog = false },
            containerColor = RawColors.Surface,
            title = { Text("UPDATE CALLSIGN", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = editingName,
                    onValueChange = { editingName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                RawButton(
                    label = "SAVE",
                    onClick = {
                        onUpdateProfile(editingName, prefs.avatarPath)
                        showEditHandleDialog = false
                    }
                )
            }
        )
    }
}

@Composable
private fun TogglePillRow(
    mascot: MascotId,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(RawColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                AnimatedPixelMascot(mascotId = mascot, size = 20.dp)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = RawColors.TextPrimary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = RawColors.Background,
                checkedTrackColor = RawColors.TextPrimary,
                uncheckedThumbColor = RawColors.TextSecondary,
                uncheckedTrackColor = RawColors.Background
            )
        )
    }
}
