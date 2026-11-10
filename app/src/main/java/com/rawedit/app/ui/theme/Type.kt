package com.rawedit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val RawTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 58.8.sp, // 0.92x tight brutalist poster stacking
        letterSpacing = (-0.04).em,
        color = RawColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.035).em,
        color = RawColors.TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.03).em,
        color = RawColors.TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).em,
        color = RawColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.12.em,
        color = RawColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = RawFonts.Display,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.em,
        color = RawColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.04.em,
        color = RawColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.14.em,
        color = RawColors.TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.12.em,
        color = RawColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontFamily = RawFonts.Mono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.10.em,
        color = RawColors.TextSecondary
    )
)
