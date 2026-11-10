package com.rawedit.app.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ProjectStatus {
    DRAFT,
    READY,
    RENDERING,
    DONE
}

@Immutable
@Serializable
data class FrameFlag(
    val timeMs: Long,
    val frameIndex: Int,
    val tileId: String,
    val rule: String,
    val severity: String, // "CLEAN" | "WARN" | "ERROR"
    val detail: String,
    val domSnapshot: String = ""
)

@Immutable
@Entity(
    tableName = "projects",
    indices = [Index(value = ["updatedAt"]), Index(value = ["status"])]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val status: String = ProjectStatus.DRAFT.name,
    val audioUri: String = "",
    val audioDurationMs: Long = 6000L,
    val srtUri: String = "",
    val scriptText: String = "", // Or file:// pointer if > 100 KB
    val htmlCode: String = "",   // Or file:// pointer if > 100 KB
    val codeHash: String = "",
    val stepIndex: Int = 0,
    val flagsJson: String = "[]",
    val lastSheetId: String = ""
)

@Immutable
@Entity(
    tableName = "keyframe_sheets",
    indices = [Index(value = ["projectId"]), Index(value = ["createdAt"])]
)
data class KeyframeSheetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val createdAt: Long,
    val intervalMs: Long,
    val tileCount: Int,
    val pageCount: Int,
    val pdfUri: String,
    val zipUri: String,
    val flagsJson: String,
    val tilesManifestJson: String = "[]"
)

@Immutable
@Entity(
    tableName = "fix_attempts",
    indices = [Index(value = ["projectId"]), Index(value = ["timestamp"])]
)
data class FixAttemptEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val timestamp: Long,
    val flagsBefore: Int,
    val flagsAfter: Int,
    val codeHash: String,
    val summaryNote: String = ""
)

@Immutable
@Entity(
    tableName = "videos",
    indices = [Index(value = ["projectId"]), Index(value = ["createdAt"])]
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val uri: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val createdAt: Long,
    val thumbnailUri: String = ""
)

@Immutable
@Serializable
data class KeyframeTileMeta(
    val index: Int,
    val sheetIndex: Int,
    val timeMs: Long,
    val timecode: String,
    val frameIndex: Int,
    val totalFrames: Int,
    val tileId: String,
    val imagePath: String,
    val status: String, // "CLEAN" | "WARN" | "ERROR"
    val primaryFlag: String,
    val flags: List<FrameFlag> = emptyList()
)
