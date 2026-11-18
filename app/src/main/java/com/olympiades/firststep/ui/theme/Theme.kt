package com.olympiades.firststep.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.ApplicationStatus

private val LightColorScheme = lightColorScheme(
    primary = FirstStepPrimary,
    onPrimary = Color.White,
    primaryContainer = FirstStepPrimaryContainer,
    onPrimaryContainer = FirstStepOnPrimaryContainer,
    secondary = FirstStepSecondary,
    onSecondary = Color.White,
    secondaryContainer = FirstStepSecondaryContainer,
    onSecondaryContainer = FirstStepOnSecondaryContainer,
    background = FirstStepLightBackground,
    onBackground = FirstStepLightText,
    surface = FirstStepLightSurface,
    onSurface = FirstStepLightText,
    surfaceVariant = FirstStepLightSurfaceVariant,
    onSurfaceVariant = FirstStepLightTextSecondary,
    outline = FirstStepOutlineLight,
    error = StatusRejectedColor,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = FirstStepPrimaryDark,
    onPrimary = Color(0xFF061E3D),
    primaryContainer = Color(0xFF0E3A73),
    onPrimaryContainer = Color(0xFFDCEAFE),
    secondary = FirstStepSecondaryDark,
    onSecondary = Color(0xFF00241F),
    secondaryContainer = Color(0xFF004D44),
    onSecondaryContainer = Color(0xFFD4F4EE),
    background = FirstStepDarkBackground,
    onBackground = FirstStepDarkText,
    surface = FirstStepDarkSurface,
    onSurface = FirstStepDarkText,
    surfaceVariant = FirstStepDarkSurfaceVariant,
    onSurfaceVariant = FirstStepDarkTextSecondary,
    outline = FirstStepOutlineDark,
    error = Color(0xFFEF5350),
    onError = Color(0xFF280000)
)

val FirstStepShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun FirstStepTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    language: AppLanguage = AppLanguage.FR,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = firstStepTypography(language),
        shapes = FirstStepShapes,
        content = content
    )
}

fun ApplicationStatus.badgeColor(): Color = when (this) {
    ApplicationStatus.SUBMITTED -> StatusSubmittedColor
    ApplicationStatus.UNDER_REVIEW -> StatusUnderReviewColor
    ApplicationStatus.INTERVIEW_SCHEDULED -> StatusInterviewColor
    ApplicationStatus.ACCEPTED -> StatusAcceptedColor
    ApplicationStatus.REJECTED -> StatusRejectedColor
    ApplicationStatus.CANCELLED -> StatusCancelledColor
}
