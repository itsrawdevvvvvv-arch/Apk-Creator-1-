package com.rawedit.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class RawRepository(
    private val context: Context,
    private val dao: RawEditDao,
    val cacheManager: CacheManager,
    val stylePackExporter: StylePackExporter
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val allProjectsFlow: Flow<List<ProjectEntity>> = dao.observeAllProjects()
    val allVideosFlow: Flow<List<VideoEntity>> = dao.observeAllVideos()

    fun observeProject(projectId: String): Flow<ProjectEntity?> = dao.observeProjectById(projectId)
    fun observeSheets(projectId: String): Flow<List<KeyframeSheetEntity>> = dao.observeSheetsForProject(projectId)
    fun observeFixAttempts(projectId: String): Flow<List<FixAttemptEntity>> = dao.observeFixAttempts(projectId)
    fun observeVideosForProject(projectId: String): Flow<List<VideoEntity>> = dao.observeVideosForProject(projectId)

    suspend fun getProject(projectId: String): ProjectEntity? = dao.getProjectById(projectId)
    suspend fun getSheetById(sheetId: String): KeyframeSheetEntity? = dao.getSheetById(sheetId)
    suspend fun getLatestSheet(projectId: String): KeyframeSheetEntity? = dao.getLatestSheetForProject(projectId)
    suspend fun getLatestVideo(projectId: String): VideoEntity? = dao.getLatestVideoForProject(projectId)

    suspend fun createNewProject(title: String = "UNTITLED REEL"): ProjectEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sampleHtml = runCatching {
            context.assets.open("sample.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrDefault("")
        val hash = cacheManager.computeSha256(sampleHtml)
        val entity = ProjectEntity(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "UNTITLED REEL" }.uppercase(),
            createdAt = now,
            updatedAt = now,
            status = ProjectStatus.DRAFT.name,
            audioDurationMs = 6000L,
            htmlCode = sampleHtml,
            codeHash = hash,
            stepIndex = 0
        )
        dao.upsertProject(entity)
        entity
    }

    suspend fun saveProjectStepState(
        project: ProjectEntity,
        rawScriptText: String? = null,
        rawHtmlCode: String? = null,
        flags: List<FrameFlag>? = null
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val persistedScript = if (rawScriptText != null) {
            cacheManager.persistTextWithOverflow(project.id, "script", rawScriptText)
        } else {
            project.scriptText
        }
        val persistedHtml = if (rawHtmlCode != null) {
            cacheManager.persistTextWithOverflow(project.id, "html", rawHtmlCode)
        } else {
            project.htmlCode
        }
        val resolvedHtmlForHash = rawHtmlCode ?: cacheManager.resolveTextWithOverflow(persistedHtml)
        val newHash = cacheManager.computeSha256(resolvedHtmlForHash)
        val flagsStr = if (flags != null) json.encodeToString(flags) else project.flagsJson

        val updated = project.copy(
            updatedAt = System.currentTimeMillis(),
            scriptText = persistedScript,
            htmlCode = persistedHtml,
            codeHash = newHash,
            flagsJson = flagsStr
        )
        dao.upsertProject(updated)
        updated
    }

    suspend fun renameProject(projectId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = dao.getProjectById(projectId) ?: return@withContext
        dao.upsertProject(
            existing.copy(
                title = newTitle.trim().ifBlank { "UNTITLED REEL" }.uppercase(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicateProject(projectId: String): ProjectEntity? = withContext(Dispatchers.IO) {
        val src = dao.getProjectById(projectId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val newId = UUID.randomUUID().toString()
        val resolvedScript = cacheManager.resolveTextWithOverflow(src.scriptText)
        val resolvedHtml = cacheManager.resolveTextWithOverflow(src.htmlCode)
        val copyEntity = src.copy(
            id = newId,
            title = "${src.title} COPY",
            createdAt = now,
            updatedAt = now,
            status = ProjectStatus.DRAFT.name,
            scriptText = cacheManager.persistTextWithOverflow(newId, "script", resolvedScript),
            htmlCode = cacheManager.persistTextWithOverflow(newId, "html", resolvedHtml),
            lastSheetId = ""
        )
        dao.upsertProject(copyEntity)
        copyEntity
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        dao.deleteVideosForProject(projectId)
        dao.deleteSheetsForProject(projectId)
        dao.deleteFixAttemptsForProject(projectId)
        dao.deleteProjectById(projectId)
    }

    suspend fun recordKeyframeSheet(sheet: KeyframeSheetEntity, flags: List<FrameFlag>) = withContext(Dispatchers.IO) {
        dao.insertKeyframeSheet(sheet)
        val project = dao.getProjectById(sheet.projectId) ?: return@withContext
        dao.upsertProject(
            project.copy(
                updatedAt = System.currentTimeMillis(),
                lastSheetId = sheet.id,
                flagsJson = json.encodeToString(flags)
            )
        )
    }

    suspend fun recordFixAttempt(
        projectId: String,
        flagsBefore: Int,
        flagsAfter: Int,
        codeHash: String,
        summaryNote: String
    ) = withContext(Dispatchers.IO) {
        val attempt = FixAttemptEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            timestamp = System.currentTimeMillis(),
            flagsBefore = flagsBefore,
            flagsAfter = flagsAfter,
            codeHash = codeHash,
            summaryNote = summaryNote
        )
        dao.insertFixAttempt(attempt)
    }

    suspend fun recordCompletedVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        dao.insertVideo(video)
        val project = dao.getProjectById(video.projectId) ?: return@withContext
        dao.upsertProject(
            project.copy(
                status = ProjectStatus.DONE.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun decodeFlags(flagsJson: String): List<FrameFlag> {
        if (flagsJson.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FrameFlag>>(flagsJson) }.getOrDefault(emptyList())
    }

    fun decodeTilesManifest(manifestJson: String): List<KeyframeTileMeta> {
        if (manifestJson.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<KeyframeTileMeta>>(manifestJson) }.getOrDefault(emptyList())
    }
}
