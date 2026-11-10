package com.rawedit.app.render

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class RenderProgressState(
    val isRendering: Boolean = false,
    val projectId: String = "",
    val projectTitle: String = "",
    val currentFrame: Int = 0,
    val totalFrames: Int = 180,
    val progressFraction: Float = 0f,
    val elapsedMs: Long = 0L,
    val etaMs: Long = 0L,
    val statusLabel: String = "IDLE",
    val previewBitmap: Bitmap? = null,
    val completedVideoId: String? = null,
    val errorMessage: String? = null
)

object RenderStateHolder {
    private val _state = MutableStateFlow(RenderProgressState())
    val state: StateFlow<RenderProgressState> = _state.asStateFlow()

    @Volatile
    var cancelRequested: Boolean = false
        private set

    fun requestCancel() {
        cancelRequested = true
    }

    fun resetCancel() {
        cancelRequested = false
    }

    fun update(transform: (RenderProgressState) -> RenderProgressState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        cancelRequested = false
        _state.value = RenderProgressState()
    }
}
