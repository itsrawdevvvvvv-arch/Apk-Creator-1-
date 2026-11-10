package com.rawedit.app.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rawedit.app.RawEditApplication
import com.rawedit.app.data.FixAttemptEntity
import com.rawedit.app.data.FrameFlag
import com.rawedit.app.data.KeyframeSheetEntity
import com.rawedit.app.data.KeyframeTileMeta
import com.rawedit.app.data.ProjectEntity
import com.rawedit.app.data.ProjectStatus
import com.rawedit.app.data.UserPrefsState
import com.rawedit.app.data.VideoEntity
import com.rawedit.app.domain.AutofitReport
import com.rawedit.app.domain.CodeValidator
import com.rawedit.app.domain.PromptGenerator
import com.rawedit.app.domain.SrtCue
import com.rawedit.app.domain.SrtParser
import com.rawedit.app.domain.ValidationReport
import com.rawedit.app.render.KeyframeSheetGenerator
import com.rawedit.app.render.KeyframeSheetProgress
import com.rawedit.app.render.RenderProgressState
import com.rawedit.app.render.RenderService
import com.rawedit.app.render.RenderStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class CreateFlowUiState(
    val project: ProjectEntity? = null,
    val stepIndex: Int = 0,
    val audioDisplayName: String = "Built-in 6.0s Sample Timing",
    val srtCues: List<SrtCue> = emptyList(),
    val srtWarnings: List<String> = emptyList(),
    val srtPromptSnippet: String = "",
    val scriptText: String = "",
    val generatedPrompt: String = "",
    val htmlCode: String = "",
    val isValidating: Boolean = false,
    val validationReport: ValidationReport? = null,
    val currentPreviewTimeMs: Long = 0L,
    val autofitReport: AutofitReport = AutofitReport(),
    val isGeneratingSheet: Boolean = false,
    val sheetProgress: KeyframeSheetProgress? = null,
    val latestSheet: KeyframeSheetEntity? = null,
    val sheetTiles: List<KeyframeTileMeta> = emptyList(),
    val flags: List<FrameFlag> = emptyList(),
    val fixAttempts: List<FixAttemptEntity> = emptyList(),
    val fixPromptText: String = "",
    val includeFullHtmlInFixPrompt: Boolean = true,
    val statusBanner: String? = null
)

class RawEditViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RawEditApplication
    private val repo = app.repository
    private val prefsRepo = app.userPreferences
    val cacheManager = app.cacheManager
    val deviceProfile = app.deviceProfile

    private val sheetGenerator = KeyframeSheetGenerator(
        context = app.applicationContext,
        cacheManager = cacheManager,
        deviceProfile = deviceProfile
    )

    val userPrefs: StateFlow<UserPrefsState> = prefsRepo.prefsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPrefsState()
    )

    val allProjects: StateFlow<List<ProjectEntity>> = repo.allProjectsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allVideos: StateFlow<List<VideoEntity>> = repo.allVideosFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val renderState: StateFlow<RenderProgressState> = RenderStateHolder.state

    private val _createState = MutableStateFlow(CreateFlowUiState())
    val createState: StateFlow<CreateFlowUiState> = _createState.asStateFlow()

    fun clearBanner() {
        _createState.update { it.copy(statusBanner = null) }
    }

    fun startNewProject(onReady: (String) -> Unit = {}) {
        viewModelScope.launch {
            val created = repo.createNewProject("UNTITLED REEL")
            loadProjectIntoFlow(created.id)
            onReady(created.id)
        }
    }

    fun loadProjectIntoFlow(projectId: String) {
        viewModelScope.launch {
            val proj = repo.getProject(projectId) ?: repo.createNewProject("UNTITLED REEL")
            val resolvedScript = cacheManager.resolveTextWithOverflow(proj.scriptText)
            val resolvedHtml = cacheManager.resolveTextWithOverflow(proj.htmlCode)
            val decodedFlags = repo.decodeFlags(proj.flagsJson)
            val latestSheet = repo.getLatestSheet(proj.id)
            val tiles = latestSheet?.let { repo.decodeTilesManifest(it.tilesManifestJson) } ?: emptyList()

            val prompt = PromptGenerator.buildAuthoringPrompt(
                project = proj,
                srtSnippet = _createState.value.srtPromptSnippet,
                resolvedScript = resolvedScript
            )
            val fixPrompt = PromptGenerator.buildFixPrompt(
                project = proj,
                flags = decodedFlags,
                codeHash = proj.codeHash,
                htmlSourceOrSlice = resolvedHtml,
                includeFullHtml = true
            )

            _createState.update {
                it.copy(
                    project = proj,
                    stepIndex = proj.stepIndex.coerceIn(0, 4),
                    scriptText = resolvedScript,
                    htmlCode = resolvedHtml,
                    generatedPrompt = prompt,
                    latestSheet = latestSheet,
                    sheetTiles = tiles,
                    flags = decodedFlags,
                    fixPromptText = fixPrompt,
                    statusBanner = null
                )
            }

            // Observe fix attempts for this project
            launch {
                repo.observeFixAttempts(proj.id).collect { attempts ->
                    _createState.update { state -> state.copy(fixAttempts = attempts) }
                }
            }
        }
    }

    fun setCreateStep(step: Int) {
        val clamped = step.coerceIn(0, 4)
        _createState.update { it.copy(stepIndex = clamped) }
        viewModelScope.launch {
            val current = _createState.value.project ?: return@launch
            val saved = repo.saveProjectStepState(
                project = current.copy(stepIndex = clamped),
                rawScriptText = _createState.value.scriptText,
                rawHtmlCode = _createState.value.htmlCode
            )
            val updatedPrompt = PromptGenerator.buildAuthoringPrompt(
                project = saved,
                srtSnippet = _createState.value.srtPromptSnippet,
                resolvedScript = _createState.value.scriptText
            )
            _createState.update { it.copy(project = saved, generatedPrompt = updatedPrompt) }
        }
    }

    fun updateProjectTitle(newTitle: String) {
        val current = _createState.value.project ?: return
        val updated = current.copy(title = newTitle.uppercase())
        _createState.update { it.copy(project = updated) }
        viewModelScope.launch {
            repo.saveProjectStepState(updated)
        }
    }

    fun updateScriptText(newScript: String) {
        _createState.update { it.copy(scriptText = newScript) }
        viewModelScope.launch {
            val current = _createState.value.project ?: return@launch
            val saved = repo.saveProjectStepState(current, rawScriptText = newScript)
            val prompt = PromptGenerator.buildAuthoringPrompt(
                project = saved,
                srtSnippet = _createState.value.srtPromptSnippet,
                resolvedScript = newScript
            )
            _createState.update { it.copy(project = saved, generatedPrompt = prompt) }
        }
    }

    fun importVoiceAudio(uri: Uri) {
        val current = _createState.value.project ?: return
        viewModelScope.launch {
            val res = cacheManager.importAudioFile(current.id, uri)
            res.onSuccess { info ->
                val updated = current.copy(
                    audioUri = info.localUri,
                    audioDurationMs = info.durationMs
                )
                val saved = repo.saveProjectStepState(updated)
                val prompt = PromptGenerator.buildAuthoringPrompt(
                    project = saved,
                    srtSnippet = _createState.value.srtPromptSnippet,
                    resolvedScript = _createState.value.scriptText
                )
                _createState.update {
                    it.copy(
                        project = saved,
                        audioDisplayName = "${info.displayName} (${SrtParser.formatTimecode(info.durationMs)})",
                        generatedPrompt = prompt,
                        statusBanner = "VOICE IMPORTED: ${info.displayName}"
                    )
                }
            }.onFailure { err ->
                _createState.update {
                    it.copy(statusBanner = "AUDIO ERROR: ${err.message ?: "Invalid file"}")
                }
            }
        }
    }

    fun importSrtFile(uri: Uri) {
        val current = _createState.value.project ?: return
        viewModelScope.launch {
            cacheManager.readSrtUriCapped(uri).onSuccess { rawSrt ->
                val parsed = withContext(Dispatchers.Default) { SrtParser.parse(rawSrt) }
                val saved = repo.saveProjectStepState(current.copy(srtUri = uri.toString()))
                val prompt = PromptGenerator.buildAuthoringPrompt(
                    project = saved,
                    srtSnippet = parsed.promptSnippet,
                    resolvedScript = _createState.value.scriptText
                )
                _createState.update {
                    it.copy(
                        project = saved,
                        srtCues = parsed.cues,
                        srtWarnings = parsed.warnings,
                        srtPromptSnippet = parsed.promptSnippet,
                        generatedPrompt = prompt,
                        statusBanner = "PARSED ${parsed.cues.size} SRT CUES"
                    )
                }
            }.onFailure { err ->
                _createState.update {
                    it.copy(statusBanner = "SRT ERROR: ${err.message}")
                }
            }
        }
    }

    fun copyTextToClipboard(label: String, text: String) {
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        _createState.update { it.copy(statusBanner = "COPIED $label TO CLIPBOARD") }
    }

    fun pasteHtmlFromClipboard() {
        viewModelScope.launch {
            val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip == null || clip.itemCount == 0) {
                _createState.update { it.copy(statusBanner = "CLIPBOARD IS EMPTY") }
                return@launch
            }
            val item = clip.getItemAt(0)
            val text = item.text?.toString() ?: item.uri?.let { u ->
                cacheManager.readHtmlOrTextUri(u).getOrNull()
            } ?: ""

            if (text.isBlank()) {
                _createState.update { it.copy(statusBanner = "NO TEXT IN CLIPBOARD") }
                return@launch
            }
            applyAndValidateHtml(text, isFromFixLoop = false)
        }
    }

    fun importHtmlFromUri(uri: Uri) {
        viewModelScope.launch {
            cacheManager.readHtmlOrTextUri(uri).onSuccess { raw ->
                applyAndValidateHtml(raw, isFromFixLoop = false)
            }.onFailure { err ->
                _createState.update { it.copy(statusBanner = "FILE READ ERROR: ${err.message}") }
            }
        }
    }

    fun loadBuiltInSampleHtml() {
        viewModelScope.launch {
            val sample = withContext(Dispatchers.IO) {
                app.assets.open("sample.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            applyAndValidateHtml(sample, isFromFixLoop = false)
        }
    }

    fun applyAndValidateHtml(rawInput: String, isFromFixLoop: Boolean = false) {
        val current = _createState.value.project ?: return
        val flagsBeforeCount = _createState.value.flags.size
        viewModelScope.launch {
            _createState.update { it.copy(isValidating = true, statusBanner = "RUNNING STATIC + 20S SANDBOX VALIDATION...") }
            val durationSec = (current.audioDurationMs.coerceAtLeast(1000L)) / 1000.0
            val report = CodeValidator.validateFull(
                context = app.applicationContext,
                rawInput = rawInput,
                fallbackDurationSec = durationSec
            )

            val newStatus = if (report.passed) ProjectStatus.READY.name else ProjectStatus.DRAFT.name
            val saved = repo.saveProjectStepState(
                project = current.copy(status = newStatus),
                rawHtmlCode = report.sanitizedHtml
            )

            if (isFromFixLoop) {
                val flagsAfter = report.issues.count { it.isFatal }
                repo.recordFixAttempt(
                    projectId = saved.id,
                    flagsBefore = flagsBeforeCount,
                    flagsAfter = flagsAfter,
                    codeHash = saved.codeHash,
                    summaryNote = if (report.passed) "PASSED VALIDATION" else "${report.issues.size} ISSUES REMAIN"
                )
            }

            _createState.update {
                it.copy(
                    project = saved,
                    htmlCode = report.sanitizedHtml,
                    isValidating = false,
                    validationReport = report,
                    statusBanner = if (report.passed) {
                        if (report.bridgedProjectContract) "VALIDATED ✓ (AUTO-BRIDGED WINDOW.PROJECT)" else "VALIDATED ✓ (OFFLINE CONTRACT READY)"
                    } else {
                        "VALIDATION FAILED (${report.issues.size} ISSUES)"
                    }
                )
            }
        }
    }

    fun updatePreviewTimestampAndReport(timeMs: Long, report: AutofitReport) {
        _createState.update {
            it.copy(
                currentPreviewTimeMs = timeMs,
                autofitReport = report
            )
        }
    }

    fun downloadStylePackToDevice() {
        viewModelScope.launch {
            app.stylePackExporter.exportToDownloads().onSuccess {
                _createState.update { s ->
                    s.copy(statusBanner = "SAVED kinetic-explainer-style.zip TO DOWNLOADS/raw_edit")
                }
            }.onFailure { err ->
                _createState.update { s ->
                    s.copy(statusBanner = "EXPORT ERROR: ${err.message}")
                }
            }
        }
    }

    fun shareStylePack(context: Context) {
        viewModelScope.launch {
            app.stylePackExporter.getShareableStylePackUri().onSuccess { uri ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Style Pack ZIP"))
            }
        }
    }

    fun generateKeyframeContactSheet(onlyFlaggedTimestamps: Boolean = false) {
        val state = _createState.value
        val project = state.project ?: return
        if (state.isGeneratingSheet) return

        val prevFlagsCount = state.flags.size
        val targetTimestamps = if (onlyFlaggedTimestamps && state.flags.isNotEmpty()) {
            state.flags.map { it.timeMs }.distinct()
        } else {
            null
        }

        viewModelScope.launch {
            _createState.update {
                it.copy(
                    isGeneratingSheet = true,
                    sheetProgress = KeyframeSheetProgress(0, 1, "00:00:00.000", "STARTING LAYER A ENGINE")
                )
            }

            val interval = userPrefs.value.sampleIntervalMs
            val result = sheetGenerator.generateSheet(
                project = project,
                resolvedHtml = state.htmlCode,
                resolvedScript = state.scriptText,
                srtCues = state.srtCues,
                existingFlags = state.flags,
                intervalMs = interval,
                onlyTimestampsMs = targetTimestamps,
                onProgress = { prog ->
                    _createState.update { s -> s.copy(sheetProgress = prog) }
                }
            )

            result.onSuccess { (sheetEntity, discoveredFlags) ->
                repo.recordKeyframeSheet(sheetEntity, discoveredFlags)
                if (onlyFlaggedTimestamps) {
                    repo.recordFixAttempt(
                        projectId = project.id,
                        flagsBefore = prevFlagsCount,
                        flagsAfter = discoveredFlags.size,
                        codeHash = project.codeHash,
                        summaryNote = "PARTIAL RE-CHECK (${targetTimestamps?.size ?: 0} FRAMES)"
                    )
                }
                val tiles = repo.decodeTilesManifest(sheetEntity.tilesManifestJson)
                val fixPrompt = PromptGenerator.buildFixPrompt(
                    project = project,
                    flags = discoveredFlags,
                    codeHash = project.codeHash,
                    htmlSourceOrSlice = extractHtmlOrSlice(state.htmlCode, state.includeFullHtmlInFixPrompt),
                    includeFullHtml = state.includeFullHtmlInFixPrompt
                )
                _createState.update {
                    it.copy(
                        isGeneratingSheet = false,
                        sheetProgress = null,
                        latestSheet = sheetEntity,
                        sheetTiles = tiles,
                        flags = discoveredFlags,
                        fixPromptText = fixPrompt,
                        statusBanner = "SHEET READY: ${tiles.size} TILES (${discoveredFlags.size} FLAGS) SAVED TO DOWNLOADS"
                    )
                }
            }.onFailure { err ->
                _createState.update {
                    it.copy(
                        isGeneratingSheet = false,
                        sheetProgress = null,
                        statusBanner = "SHEET FAILED: ${err.message}"
                    )
                }
            }
        }
    }

    fun toggleFixPromptSliceMode(includeFull: Boolean) {
        val state = _createState.value
        val project = state.project ?: return
        val newFixPrompt = PromptGenerator.buildFixPrompt(
            project = project,
            flags = state.flags,
            codeHash = project.codeHash,
            htmlSourceOrSlice = extractHtmlOrSlice(state.htmlCode, includeFull),
            includeFullHtml = includeFull
        )
        _createState.update {
            it.copy(
                includeFullHtmlInFixPrompt = includeFull,
                fixPromptText = newFixPrompt
            )
        }
    }

    private fun extractHtmlOrSlice(html: String, full: Boolean): String {
        if (full || html.length <= 4000) return html
        val styleMatch = Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE).find(html)?.value ?: ""
        val scriptMatch = Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE).findAll(html)
            .joinToString("\n") { it.value }
        return (styleMatch + "\n\n" + scriptMatch).ifBlank { html.take(8000) }
    }

    fun shareFilePath(context: Context, path: String, mimeType: String, chooserTitle: String) {
        val file = File(path)
        if (!file.exists()) {
            _createState.update { it.copy(statusBanner = "FILE NOT FOUND ON DISK") }
            return
        }
        val uri = cacheManager.getShareableUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun startFinalRender(context: Context) {
        val project = _createState.value.project ?: return
        RenderService.startRender(context, project.id)
    }

    fun cancelActiveRender(context: Context) {
        RenderService.cancelRender(context)
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch { repo.renameProject(projectId, newTitle) }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch { repo.duplicateProject(projectId) }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch { repo.deleteProject(projectId) }
    }

    fun completeOnboarding(name: String, avatarPath: String) {
        viewModelScope.launch { prefsRepo.completeOnboarding(name, avatarPath) }
    }

    fun updateProfile(name: String, avatarPath: String) {
        viewModelScope.launch { prefsRepo.updateProfile(name, avatarPath) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setReducedMotion(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    }

    fun setSampleIntervalMs(intervalMs: Long) {
        viewModelScope.launch { prefsRepo.setSampleIntervalMs(intervalMs) }
    }

    fun setUltraLowEndMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setUltraLowEndMode(enabled) }
    }
}
