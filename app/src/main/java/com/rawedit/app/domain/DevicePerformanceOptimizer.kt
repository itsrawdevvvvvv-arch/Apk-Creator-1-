package com.rawedit.app.domain

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable

@Immutable
data class DeviceBudgetProfile(
    val isLowRamDevice: Boolean,
    val totalRamMb: Int,
    val cpuCores: Int,
    val maxAmbientMascots: Int,
    val mascotTargetFps: Int,
    val previewDebounceMs: Long,
    val tileThumbnailWidth: Int,
    val tileThumbnailHeight: Int
)

object DevicePerformanceOptimizer {

    fun detectProfile(context: Context, forceUltraLowEnd: Boolean = false): DeviceBudgetProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRamFlag = am?.isLowRamDevice ?: false
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).toInt().coerceAtLeast(512)
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val isConstrained = forceUltraLowEnd || isLowRamFlag || totalRamMb <= 3072 || cores <= 4 ||
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

        return if (isConstrained) {
            DeviceBudgetProfile(
                isLowRamDevice = true,
                totalRamMb = totalRamMb,
                cpuCores = cores,
                maxAmbientMascots = 3,
                mascotTargetFps = 3,
                previewDebounceMs = 48L,
                tileThumbnailWidth = 384,
                tileThumbnailHeight = 216
            )
        } else {
            DeviceBudgetProfile(
                isLowRamDevice = false,
                totalRamMb = totalRamMb,
                cpuCores = cores,
                maxAmbientMascots = 5,
                mascotTargetFps = 5,
                previewDebounceMs = 24L,
                tileThumbnailWidth = 480,
                tileThumbnailHeight = 270
            )
        }
    }
}
