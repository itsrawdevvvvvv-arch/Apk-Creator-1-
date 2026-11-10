package com.rawedit.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette extracted from the "Hello again, rawdev!" dark monochrome UI:
 * - Pitch-black outer canvas (#080808) with dot-grid header surface (#121212)
 * - Soft dark charcoal cards (#161616) and elevated ID-pass surface (#262626)
 * - Tactile pill button surface (#343434) and inner stadium pill (#0E0E0E)
 * - Crisp pure white (#FFFFFF) primary typography & muted ash (#9A9A9A) secondary labels
 */
object RawColors {
    val Background = Color(0xFF080808)
    val HeaderSurface = Color(0xFF121212)
    val Surface = Color(0xFF161616)
    val SurfaceElevated = Color(0xFF262626)
    val PillSurface = Color(0xFF343434)
    val DockActivePill = Color(0xFF2E2E2E)
    val Divider = Color(0xFF282828)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9A9A9A)
    val ErrorCrimson = Color(0xFFFF453A)
    val WarningAmber = Color(0xFFFFB340)
    val DotGridWhite = Color(0x12FFFFFF) // Subtle dot-matrix grid like rawdev header
}
