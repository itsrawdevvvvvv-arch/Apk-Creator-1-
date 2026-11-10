package com.rawedit.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RawEditDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT * FROM keyframe_sheets WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeSheetsForProject(projectId: String): Flow<List<KeyframeSheetEntity>>

    @Query("SELECT * FROM keyframe_sheets WHERE id = :sheetId LIMIT 1")
    suspend fun getSheetById(sheetId: String): KeyframeSheetEntity?

    @Query("SELECT * FROM keyframe_sheets WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSheetForProject(projectId: String): KeyframeSheetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyframeSheet(sheet: KeyframeSheetEntity)

    @Query("DELETE FROM keyframe_sheets WHERE projectId = :projectId")
    suspend fun deleteSheetsForProject(projectId: String)

    @Query("SELECT * FROM fix_attempts WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun observeFixAttempts(projectId: String): Flow<List<FixAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixAttempt(attempt: FixAttemptEntity)

    @Query("DELETE FROM fix_attempts WHERE projectId = :projectId")
    suspend fun deleteFixAttemptsForProject(projectId: String)

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun observeAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeVideosForProject(projectId: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestVideoForProject(projectId: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :videoId LIMIT 1")
    suspend fun getVideoById(videoId: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE projectId = :projectId")
    suspend fun deleteVideosForProject(projectId: String)
}
