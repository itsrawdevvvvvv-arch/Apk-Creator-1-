package com.rawedit.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rawedit.app.domain.DomAutofitEngine
import com.rawedit.app.domain.LocalAssetServer
import com.rawedit.app.domain.SrtParser
import com.rawedit.app.render.Mp4FrameMuxer
import com.rawedit.app.ui.CreateFlowUiState
import com.rawedit.app.ui.RawEditViewModel
import com.rawedit.app.ui.components.DotGridBackground
import com.rawedit.app.ui.components.RawButton
import com.rawedit.app.ui.components.SwipeToConfirmSlider
import com.rawedit.app.ui.components.WindowedCodeViewer
import com.rawedit.app.ui.icons.RawIcon
import com.rawedit.app.ui.icons.RawIconType
import com.rawedit.app.ui.mascots.AnimatedPixelMascot
import com.rawedit.app.ui.mascots.MascotId
import com.rawedit.app.ui.mascots.MascotMood
import com.rawedit.app.ui.theme.RawColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val STEP_TITLES = listOf(
    "01 // VOICE & SCRIPT",
    "02 // AI PROMPT",
    "03 // PASTE & VALIDATE",
    "04 // LAYER A PREVIEW & SHEET",
    "05 // FINAL 1080P30 RENDER"
)

private val STEP_MASCOTS = listOf(
    MascotId.BOOMBOX_BUDDY,
    MascotId.DISH_SCOUT,
    MascotId.ANTENNA_BUG,
    MascotId.SHELL_SNAIL,
    MascotId.VOLT_BOLT
)

@Composable
fun CreateFlowScreen(
    state: CreateFlowUiState,
    sampleIntervalMs: Long,
    viewModel: RawEditViewModel,
    onOpenSheetReview: () -> Unit,
    onLaunchRenderScreen: () -> Unit
) {
    val context = LocalContext.current
    val step = state.stepIndex.coerceIn(0, 4)

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importVoiceAudio(uri)
    }

    val srtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importSrtFile(uri)
    }

    val htmlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importHtmlFromUri(uri)
    }

    val stepMood = remember(state.validationReport, state.isValidating, state.isGeneratingSheet) {
        when {
            state.isValidating || state.isGeneratingSheet -> MascotMood.WORKING
            state.validationReport?.passed == false -> MascotMood.DIZZY
            state.validationReport?.passed == true -> MascotMood.HAPPY
            else -> MascotMood.IDLE
        }
    }

    DotGridBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Step header bar + reacting mascot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CREATE REEL // STEP ${step + 1} OF 5",
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextSecondary
                    )
                    Text(
                        text = STEP_TITLES[step],
                        style = MaterialTheme.typography.headlineMedium,
                        color = RawColors.TextPrimary
                    )
                }
                AnimatedPixelMascot(
                    mascotId = STEP_MASCOTS[step],
                    size = 34.dp,
                    mood = stepMood,
                    drawShadow = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5-step selector tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0..4) {
                    val active = i == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .background(if (active) RawColors.TextPrimary else RawColors.Surface)
                            .border(1.dp, if (active) RawColors.TextPrimary else RawColors.Divider)
                            .clickable { viewModel.setCreateStep(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "0${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) RawColors.Background else RawColors.TextSecondary
                        )
                    }
                }
            }

            // Status banner if present
            state.statusBanner?.let { banner ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RawColors.SurfaceElevated)
                        .border(1.dp, RawColors.TextPrimary)
                        .clickable { viewModel.clearBanner() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelSmall,
                        color = RawColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    RawIcon(type = RawIconType.CLOSE, size = 14.dp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step content body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    0 -> Step1VoiceAndScript(
                        state = state,
                        onTitleChange = viewModel::updateProjectTitle,
                        onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
                        onPickSrt = { srtPicker.launch(arrayOf("*/*")) },
                        onScriptChange = viewModel::updateScriptText,
                        onNext = { viewModel.setCreateStep(1) }
                    )
                    1 -> Step2GetPrompt(
                        state = state,
                        onCopyPrompt = {
                            viewModel.copyTextToClipboard("RAW_EDIT_PROMPT", state.generatedPrompt)
                        },
                        onSharePrompt = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, state.generatedPrompt)
                            }
                            context.startActivity(Intent.createChooser(send, "Share Prompt"))
                        },
                        onDownloadStylePack = viewModel::downloadStylePackToDevice,
                        onShareStylePack = { viewModel.shareStylePack(context) },
                        onNext = { viewModel.setCreateStep(2) }
                    )
                    2 -> Step3PasteAndValidate(
                        state = state,
                        onPasteClipboard = viewModel::pasteHtmlFromClipboard,
                        onPickHtmlFile = { htmlPicker.launch(arrayOf("text/html", "text/plain", "*/*")) },
                        onLoadSample = viewModel::loadBuiltInSampleHtml,
                        onRevalidate = { viewModel.applyAndValidateHtml(state.htmlCode, false) },
                        onNext = { viewModel.setCreateStep(3) }
                    )
                    3 -> Step4LayerAPreviewAndSheet(
                        state = state,
                        sampleIntervalMs = sampleIntervalMs,
                        onSelectInterval = viewModel::setSampleIntervalMs,
                        onUpdateAutofit = viewModel::updatePreviewTimestampAndReport,
                        onGenerateSheet = { onlyFlagged -> viewModel.generateKeyframeContactSheet(onlyFlagged) },
                        onOpenSheetReview = onOpenSheetReview,
                        onCopyFixPrompt = {
                            viewModel.copyTextToClipboard("RAW_EDIT_FIX_PROMPT", state.fixPromptText)
                        },
                        onPasteCorrectedHtml = {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipText = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clipText.isNotBlank()) {
                                viewModel.applyAndValidateHtml(clipText, isFromFixLoop = true)
                            }
                        },
                        onToggleFullHtmlSlice = viewModel::toggleFixPromptSliceMode,
                        onSharePdf = {
                            state.latestSheet?.let {
                                viewModel.shareFilePath(context, it.pdfUri, "application/pdf", "Share Contact Sheet PDF")
                            }
                        },
                        onShareZip = {
                            state.latestSheet?.let {
                                viewModel.shareFilePath(context, it.zipUri, "application/zip", "Share Keyframes ZIP")
                            }
                        },
                        onNext = { viewModel.setCreateStep(4) }
                    )
                    4 -> Step5ConfirmAndRender(
                        state = state,
                        onStartRender = {
                            viewModel.startFinalRender(context)
                            onLaunchRenderScreen()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1VoiceAndScript(
    state: CreateFlowUiState,
    onTitleChange: (String) -> Unit,
    onPickAudio: () -> Unit,
    onPickSrt: () -> Unit,
    onScriptChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val project = state.project

    OutlinedTextField(
        value = project?.title ?: "UNTITLED REEL",
        onValueChange = onTitleChange,
        label = { Text("PROJECT TITLE", style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RawColors.TextPrimary,
            unfocusedBorderColor = RawColors.Divider,
            focusedContainerColor = RawColors.Surface,
            unfocusedContainerColor = RawColors.Surface
        )
    )

    // Voiceover Card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "VOICEOVER AUDIO (MP3 / M4A / WAV / AAC · 1S–20MIN · <=100MB)",
            style = MaterialTheme.typography.labelMedium,
            color = RawColors.TextSecondary
        )
        Text(
            text = state.audioDisplayName,
            style = MaterialTheme.typography.titleMedium,
            color = RawColors.TextPrimary
        )
        RawButton(
            label = "SELECT VOICEOVER FILE",
            onClick = onPickAudio,
            modifier = Modifier.fillMaxWidth(),
            inverted = false,
            leadingIcon = RawIconType.MIC
        )
    }

    // Optional SRT Subtitles Card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "OPTIONAL .SRT SUBTITLES (<=4MB · <=20,000 CUES)",
            style = MaterialTheme.typography.labelMedium,
            color = RawColors.TextSecondary
        )
        Text(
            text = if (state.srtCues.isEmpty()) "No SRT loaded (optional)" else "${state.srtCues.size} cues parsed (${state.srtWarnings.size} warnings)",
            style = MaterialTheme.typography.bodyMedium,
            color = RawColors.TextPrimary
        )
        RawButton(
            label = "IMPORT .SRT FILE",
            onClick = onPickSrt,
            modifier = Modifier.fillMaxWidth(),
            inverted = false,
            leadingIcon = RawIconType.SHEET
        )
    }

    // Unlimited-length script / topic notes (automatically overflows to disk >100KB)
    OutlinedTextField(
        value = state.scriptText.take(12_000),
        onValueChange = onScriptChange,
        label = {
            Text(
                "OPTIONAL SCRIPT / TOPIC NOTES (AUTO-OVERFLOWS >100KB SAFELY)",
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RawColors.TextPrimary,
            unfocusedBorderColor = RawColors.Divider,
            focusedContainerColor = RawColors.Surface,
            unfocusedContainerColor = RawColors.Surface
        )
    )

    RawButton(
        label = "CONTINUE TO STEP 02 // PROMPT",
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        inverted = true,
        trailingIcon = RawIconType.ARROW_RIGHT
    )
}

@Composable
private fun Step2GetPrompt(
    state: CreateFlowUiState,
    onCopyPrompt: () -> Unit,
    onSharePrompt: () -> Unit,
    onDownloadStylePack: () -> Unit,
    onShareStylePack: () -> Unit,
    onNext: () -> Unit
) {
    // Offline guarantee card + 3-line explainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "THIS APP IS OFFLINE. NOTHING IS SENT ANYWHERE — YOU COPY AND PASTE.",
            style = MaterialTheme.typography.labelLarge,
            color = RawColors.TextPrimary
        )
        HorizontalDivider(color = RawColors.Divider)
        Text("1. Download `kinetic-explainer-style.zip` below and attach/upload it to Claude or ChatGPT.", style = MaterialTheme.typography.bodyMedium)
        Text("2. Copy the detailed prompt below (includes SRT timings, Cream & Crimson style rules, single-file HTML5/JS contract & Claude credit-saver guardrails).", style = MaterialTheme.typography.bodyMedium)
        Text("3. Copy the AI's HTML response and paste it into Step 03 for offline validation.", style = MaterialTheme.typography.bodyMedium)
    }

    WindowedCodeViewer(code = state.generatedPrompt)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RawButton(
            label = "COPY PROMPT",
            onClick = onCopyPrompt,
            modifier = Modifier.weight(1f),
            inverted = true,
            leadingIcon = RawIconType.COPY
        )
        RawButton(
            label = "SHARE",
            onClick = onSharePrompt,
            modifier = Modifier.weight(1f),
            inverted = false,
            leadingIcon = RawIconType.SHARE
        )
    }

    // Style pack export card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "KINETIC EXPLAINER STYLE PACK (SKILL.MD + STYLE_BIBLE + TEMPLATES)",
            style = MaterialTheme.typography.labelMedium,
            color = RawColors.TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RawButton(
                label = "DOWNLOAD STYLE PACK",
                onClick = onDownloadStylePack,
                modifier = Modifier.weight(1f),
                inverted = false,
                leadingIcon = RawIconType.DOWNLOAD
            )
            RawButton(
                label = "SHARE ZIP",
                onClick = onShareStylePack,
                modifier = Modifier.weight(1f),
                inverted = false,
                leadingIcon = RawIconType.SHARE
            )
        }
    }

    RawButton(
        label = "CONTINUE TO STEP 03 // PASTE CODE",
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        inverted = true,
        trailingIcon = RawIconType.ARROW_RIGHT
    )
}

@Composable
private fun Step3PasteAndValidate(
    state: CreateFlowUiState,
    onPasteClipboard: () -> Unit,
    onPickHtmlFile: () -> Unit,
    onLoadSample: () -> Unit,
    onRevalidate: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RawButton(
            label = "PASTE CLIPBOARD",
            onClick = onPasteClipboard,
            modifier = Modifier.weight(1f),
            inverted = true,
            leadingIcon = RawIconType.PASTE
        )
        RawButton(
            label = "PICK .HTML/.TXT",
            onClick = onPickHtmlFile,
            modifier = Modifier.weight(1f),
            inverted = false,
            leadingIcon = RawIconType.LAYERS
        )
    }

    RawButton(
        label = "RUN BUILT-IN 6.0S SAMPLE TEST",
        onClick = onLoadSample,
        modifier = Modifier.fillMaxWidth(),
        inverted = false,
        leadingIcon = RawIconType.SPARKLE
    )

    // Zero-lag windowed viewer (never puts >20k chars in a TextField)
    WindowedCodeViewer(code = state.htmlCode)

    RawButton(
        label = if (state.isValidating) "VALIDATING IN OFFLINE SANDBOX..." else "RUN VALIDATOR (STATIC + 20S DRY-RUN)",
        onClick = onRevalidate,
        enabled = !state.isValidating,
        modifier = Modifier.fillMaxWidth(),
        inverted = false,
        leadingIcon = RawIconType.CHECK
    )

    state.validationReport?.let { rep ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RawColors.Surface)
                .border(
                    1.dp,
                    if (rep.passed) RawColors.TextPrimary else RawColors.ErrorCrimson
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rep.passed) "VALIDATOR STATUS: PASS ✓" else "VALIDATOR STATUS: FAIL ✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rep.passed) RawColors.TextPrimary else RawColors.ErrorCrimson
                )
                Text(
                    text = "DURATION: ${String.format("%.2fs", rep.detectedDurationSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = RawColors.TextSecondary
                )
            }

            if (rep.issues.isEmpty()) {
                Text(
                    text = "All offline rules passed (0 external URLs, 0 network APIs, window.RAW.seek verified).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RawColors.TextSecondary
                )
            } else {
                rep.issues.forEach { issue ->
                    Text(
                        text = "• [${issue.rule}] ${issue.detail}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RawColors.ErrorCrimson
                    )
                }
            }

            if (rep.consoleLogs.isNotEmpty()) {
                HorizontalDivider(color = RawColors.Divider)
                Text("CAPTURED JS CONSOLE:", style = MaterialTheme.typography.labelSmall)
                rep.consoleLogs.take(6).forEach { log ->
                    Text(log, style = MaterialTheme.typography.labelSmall, color = RawColors.TextSecondary)
                }
            }
        }
    }

    RawButton(
        label = "CONTINUE TO STEP 04 // LAYER A PREVIEW",
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        inverted = true,
        trailingIcon = RawIconType.ARROW_RIGHT
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun Step4LayerAPreviewAndSheet(
    state: CreateFlowUiState,
    sampleIntervalMs: Long,
    onSelectInterval: (Long) -> Unit,
    onUpdateAutofit: (Long, com.rawedit.app.domain.AutofitReport) -> Unit,
    onGenerateSheet: (Boolean) -> Unit,
    onOpenSheetReview: () -> Unit,
    onCopyFixPrompt: () -> Unit,
    onPasteCorrectedHtml: () -> Unit,
    onToggleFullHtmlSlice: (Boolean) -> Unit,
    onSharePdf: () -> Unit,
    onShareZip: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val durationMs = (state.project?.audioDurationMs ?: 6000L).coerceAtLeast(1000L)
    var currentTimeMs by remember { mutableLongStateOf(state.currentPreviewTimeMs) }
    var isPlaying by remember { mutableStateOf(false) }
    var showViolationsDialog by remember { mutableStateOf(false) }
    var showFixLoopModal by remember { mutableStateOf(false) }

    val assetServer = remember(context) {
        LocalAssetServer(context.applicationContext, state.htmlCode)
    }
    LaunchedEffect(state.htmlCode) {
        assetServer.currentHtmlContent = state.htmlCode
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Debounced seek + live DOM autofit measurement
    LaunchedEffect(currentTimeMs, state.htmlCode) {
        val wv = webViewRef ?: return@LaunchedEffect
        delay(24L)
        val rep = DomAutofitEngine.seekAndMeasure(
            webView = wv,
            timeSeconds = currentTimeMs / 1000.0,
            applyAutofitTransform = true
        )
        onUpdateAutofit(currentTimeMs, rep)
    }

    // Play loop at ~20fps for interactive scrubbing
    LaunchedEffect(isPlaying, durationMs) {
        while (isPlaying) {
            delay(50L)
            val next = currentTimeMs + 50L
            currentTimeMs = if (next >= durationMs) 0L else next
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                runCatching {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.destroy()
                }
            }
            webViewRef = null
        }
    }

    // Stage 1: 16:9 Live DOM Autofit Preview Box
    Text(
        text = "STAGE 1 // LIVE DOM AUTOFIT PREVIEW (1920X1080 VIRTUAL CANVAS)",
        style = MaterialTheme.typography.labelMedium,
        color = RawColors.TextSecondary
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(RawColors.Background)
            .border(1.dp, RawColors.TextPrimary)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = false
                        allowFileAccess = false
                        allowContentAccess = false
                        blockNetworkLoads = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                    }
                    setInitialScale(100)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            return if (request != null) assetServer.intercept(request) else null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (view != null) {
                                scope.launch {
                                    val rep = DomAutofitEngine.seekAndMeasure(
                                        webView = view,
                                        timeSeconds = currentTimeMs / 1000.0,
                                        applyAutofitTransform = true
                                    )
                                    onUpdateAutofit(currentTimeMs, rep)
                                }
                            }
                        }
                    }
                    loadUrl(LocalAssetServer.BASE_URL)
                    webViewRef = this
                }
            },
            update = { view ->
                if (assetServer.currentHtmlContent != state.htmlCode) {
                    assetServer.currentHtmlContent = state.htmlCode
                    view.loadUrl(LocalAssetServer.BASE_URL)
                }
            }
        )
    }

    // Autofit report strip (tap to inspect violations)
    val af = state.autofitReport
    val maxOverflow = maxOf(af.overflowPx.l, af.overflowPx.t, af.overflowPx.r, af.overflowPx.b)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, if (af.fits) RawColors.Divider else RawColors.WarningAmber)
            .clickable { showViolationsDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (af.fits) {
                "FITS ✓ · ${af.textNodesChecked} TEXT NODES · SCALE ${af.scaleApplied}X"
            } else {
                "OVERFLOW ${maxOverflow}PX · AUTOFIT SCALE ${af.scaleApplied}X · ${af.violations.size} FLAGS"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (af.fits) RawColors.TextPrimary else RawColors.WarningAmber
        )
        Text(
            text = "DETAILS →",
            style = MaterialTheme.typography.labelSmall,
            color = RawColors.TextPrimary
        )
    }

    // Transport scrubber + frame-step controls
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SrtParser.formatTimecode(currentTimeMs),
                style = MaterialTheme.typography.labelLarge,
                color = RawColors.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RawButton(
                    label = "-1F",
                    onClick = {
                        isPlaying = false
                        currentTimeMs = (currentTimeMs - 33L).coerceAtLeast(0L)
                    },
                    inverted = false
                )
                RawButton(
                    label = if (isPlaying) "PAUSE" else "PLAY",
                    onClick = { isPlaying = !isPlaying },
                    inverted = true,
                    leadingIcon = if (isPlaying) RawIconType.PAUSE else RawIconType.PLAY
                )
                RawButton(
                    label = "+1F",
                    onClick = {
                        isPlaying = false
                        currentTimeMs = (currentTimeMs + 33L).coerceAtMost(durationMs)
                    },
                    inverted = false
                )
            }
        }
        Slider(
            value = currentTimeMs.toFloat(),
            onValueChange = {
                isPlaying = false
                currentTimeMs = it.toLong().coerceIn(0L, durationMs)
            },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = RawColors.TextPrimary,
                activeTrackColor = RawColors.TextPrimary,
                inactiveTrackColor = RawColors.Divider
            )
        )
    }

    // Stage 2: Pre-Render Keyframe Contact Sheet controls
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "STAGE 2 // PRE-RENDER KEYFRAME CONTACT SHEET (PDF + ZIP)",
            style = MaterialTheme.typography.labelMedium,
            color = RawColors.TextSecondary
        )

        // Sampling interval selector
        val intervals = listOf(
            250L to "0.25S",
            500L to "0.5S",
            1000L to "1.0S",
            2000L to "2.0S",
            33L to "1F (<=10S)"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            intervals.forEach { (ms, label) ->
                val active = sampleIntervalMs == ms
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .background(if (active) RawColors.TextPrimary else RawColors.Background)
                        .border(1.dp, if (active) RawColors.TextPrimary else RawColors.Divider)
                        .clickable { onSelectInterval(ms) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) RawColors.Background else RawColors.TextSecondary
                    )
                }
            }
        }

        if (state.isGeneratingSheet) {
            val prog = state.sheetProgress
            val frac = if (prog != null && prog.totalTiles > 0) {
                prog.currentTile.toFloat() / prog.totalTiles.toFloat()
            } else 0f
            LinearProgressIndicator(
                progress = { frac },
                modifier = Modifier.fillMaxWidth(),
                color = RawColors.TextPrimary,
                trackColor = RawColors.Divider
            )
            Text(
                text = "${prog?.phaseLabel ?: "CAPTURING"} · ${prog?.currentTile ?: 0}/${prog?.totalTiles ?: 0} (${prog?.currentTimecode ?: ""})",
                style = MaterialTheme.typography.labelSmall,
                color = RawColors.TextPrimary
            )
        } else {
            RawButton(
                label = "GENERATE KEYFRAME SHEET (PDF + ZIP)",
                onClick = { onGenerateSheet(false) },
                modifier = Modifier.fillMaxWidth(),
                inverted = true,
                leadingIcon = RawIconType.SHEET
            )
        }

        state.latestSheet?.let { sheet ->
            HorizontalDivider(color = RawColors.Divider)
            Text(
                text = "LATEST SHEET: ${sheet.tileCount} TILES · ${sheet.pageCount} PDF PAGES · ${state.flags.size} FLAGS",
                style = MaterialTheme.typography.labelSmall,
                color = RawColors.TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RawButton(
                    label = "REVIEW SHEET",
                    onClick = onOpenSheetReview,
                    modifier = Modifier.weight(1f),
                    inverted = true,
                    leadingIcon = RawIconType.GRID
                )
                RawButton(
                    label = "AI FIX LOOP",
                    onClick = { showFixLoopModal = true },
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.BOLT
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RawButton(
                    label = "SHARE PDF",
                    onClick = onSharePdf,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.SHARE
                )
                RawButton(
                    label = "SHARE ZIP",
                    onClick = onShareZip,
                    modifier = Modifier.weight(1f),
                    inverted = false,
                    leadingIcon = RawIconType.DOWNLOAD
                )
            }
        }
    }

    RawButton(
        label = "CONTINUE TO STEP 05 // FINAL RENDER",
        onClick = onNext,
        modifier = Modifier.fillMaxWidth(),
        inverted = true,
        trailingIcon = RawIconType.ARROW_RIGHT
    )

    // Violations detail dialog
    if (showViolationsDialog) {
        AlertDialog(
            onDismissRequest = { showViolationsDialog = false },
            containerColor = RawColors.Surface,
            title = {
                Text("DOM AUTOFIT INSPECTION", style = MaterialTheme.typography.titleMedium, color = RawColors.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Snapshot: ${af.domSnapshotSummary}", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Overflow L=${af.overflowPx.l}px T=${af.overflowPx.t}px R=${af.overflowPx.r}px B=${af.overflowPx.b}px",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (af.violations.isEmpty()) {
                        Text("Zero violations at current timestamp.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        af.violations.forEach { v ->
                            Text("• [${v.kind}] ${v.selector}: ${v.detail}", style = MaterialTheme.typography.bodyMedium, color = RawColors.WarningAmber)
                        }
                    }
                }
            },
            confirmButton = {
                RawButton(label = "CLOSE", onClick = { showViolationsDialog = false })
            }
        )
    }

    // Stage 4: Manual Cloud-AI Fix Loop Modal
    if (showFixLoopModal) {
        AlertDialog(
            onDismissRequest = { showFixLoopModal = false },
            containerColor = RawColors.Surface,
            title = {
                Text("STAGE 4 // MANUAL CLOUD-AI FIX LOOP", style = MaterialTheme.typography.titleMedium, color = RawColors.TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "This app is offline. Nothing is sent anywhere — you copy and paste.",
                        style = MaterialTheme.typography.labelMedium,
                        color = RawColors.TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RawButton(
                            label = if (state.includeFullHtmlInFixPrompt) "FULL HTML ✓" else "FULL HTML",
                            onClick = { onToggleFullHtmlSlice(true) },
                            modifier = Modifier.weight(1f),
                            inverted = state.includeFullHtmlInFixPrompt
                        )
                        RawButton(
                            label = if (!state.includeFullHtmlInFixPrompt) "CSS/JS SLICE ✓" else "CSS/JS SLICE",
                            onClick = { onToggleFullHtmlSlice(false) },
                            modifier = Modifier.weight(1f),
                            inverted = !state.includeFullHtmlInFixPrompt
                        )
                    }
                    RawButton(
                        label = "1. COPY AI FIX PROMPT (${state.flags.size} FLAGS)",
                        onClick = onCopyFixPrompt,
                        modifier = Modifier.fillMaxWidth(),
                        inverted = true,
                        leadingIcon = RawIconType.COPY
                    )
                    RawButton(
                        label = "2. PASTE CORRECTED HTML & RE-VALIDATE",
                        onClick = onPasteCorrectedHtml,
                        modifier = Modifier.fillMaxWidth(),
                        inverted = false,
                        leadingIcon = RawIconType.PASTE
                    )
                    if (state.flags.isNotEmpty()) {
                        RawButton(
                            label = "3. FAST RE-CHECK FLAGGED FRAMES ONLY",
                            onClick = {
                                showFixLoopModal = false
                                onGenerateSheet(true)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            inverted = false,
                            leadingIcon = RawIconType.BOLT
                        )
                    }
                    if (state.fixAttempts.isNotEmpty()) {
                        HorizontalDivider(color = RawColors.Divider)
                        Text("FIX ATTEMPT HISTORY:", style = MaterialTheme.typography.labelSmall)
                        state.fixAttempts.take(5).forEach { att ->
                            Text(
                                text = "SHA ${att.codeHash.take(8)} · FLAGS ${att.flagsBefore}→${att.flagsAfter} · ${att.summaryNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = RawColors.TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                RawButton(label = "DONE", onClick = { showFixLoopModal = false })
            }
        )
    }
}

@Composable
private fun Step5ConfirmAndRender(
    state: CreateFlowUiState,
    onStartRender: () -> Unit
) {
    val durationMs = state.project?.audioDurationMs ?: 6000L
    val bitrateMbps = Mp4FrameMuxer.selectTargetBitrate(durationMs) / 1_000_000.0
    val totalFrames = ((durationMs / 1000.0) * 30.0).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RawColors.Surface)
            .border(1.dp, RawColors.Divider)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("STAGE 5 // HARDWARE MP4 ENCODER", style = MaterialTheme.typography.titleMedium)
            AnimatedPixelMascot(mascotId = MascotId.CRT_BOT, size = 28.dp)
        }
        HorizontalDivider(color = RawColors.Divider)
        Text("RESOLUTION: 1920 × 1080 (16:9 FULL HD)", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextPrimary)
        Text("FRAME RATE: 30 FPS LOCKED ($totalFrames FRAMES)", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextPrimary)
        Text("VIDEO CODEC: H.264 / AVC (${String.format("%.1f", bitrateMbps)} MBPS TARGET)", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextPrimary)
        Text("AUDIO CODEC: 128 KBPS AAC (MEDIA3 TRANSFORMER)", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextPrimary)
        Text("CODE SHA-256: ${state.project?.codeHash?.take(16) ?: "NONE"}", style = MaterialTheme.typography.bodyMedium, color = RawColors.TextSecondary)
    }

    Spacer(modifier = Modifier.height(8.dp))

    SwipeToConfirmSlider(
        label = "SWIPE TO RENDER 1080P30 MP4 →",
        onConfirmed = onStartRender
    )
}
