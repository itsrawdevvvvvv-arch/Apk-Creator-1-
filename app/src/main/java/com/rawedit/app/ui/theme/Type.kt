package com.rawedit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val RawTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 58.8.sp, // 0.92x tight brutalist poster stacking
        letterSpacing = (-2.56).sp,
        color = RawColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.68).sp,
        color = RawColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.96).sp,
        color = RawColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.44).sp,
        color = RawColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.68.sp,
        color = RawColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
        color = RawColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.48.sp,
        color = RawColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.82.sp,
        color = RawColors.TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.32.sp,
        color = RawColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.0.sp,
        color = RawColors.TextSecondary
    )
)
