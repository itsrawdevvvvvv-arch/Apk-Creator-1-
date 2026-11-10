package com.rawedit.app.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Typography FontFamily resolver for `raw_edit`.
 *
 * ZERO-BINARY POLICY:
 * This project ships without binary `.ttf` files so the repository contains 100% inspectable source.
 * It resolves intentionally to tuned system `FontFamily.SansSerif` (for heavy tight-tracked editorial
 * headlines) and `FontFamily.Monospace` (for technical labels, timecodes, counters, and buttons).
 *
 * OPTIONAL DROP-IN TTF UPGRADE GUIDE:
 * To upgrade to custom bundled fonts, drop any of the following TTF files into `app/src/main/res/font/`:
 *   - `res/font/inter_black.ttf`            (Display Hero 900 weight)
 *   - `res/font/inter_bold.ttf`             (Headline 700 weight)
 *   - `res/font/space_grotesk_bold.ttf`     (Section Titles)
 *   - `res/font/jetbrains_mono_regular.ttf` (Technical Metadata & Code)
 *   - `res/font/jetbrains_mono_bold.ttf`    (Buttons, Badges & Timecodes)
 * And wire them via `Font(R.font.inter_black, FontWeight.Black)` inside `Display` and `Mono` below.
 */
object RawFonts {
    val Display: FontFamily = FontFamily.SansSerif
    val Mono: FontFamily = FontFamily.Monospace
}
