package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MomoPrimaryDark,
    onPrimary = MomoOnPrimaryDark,
    primaryContainer = MomoPrimaryContainerDark,
    onPrimaryContainer = MomoOnPrimaryContainerDark,
    secondary = MomoSecondaryDark,
    onSecondary = MomoOnSecondaryDark,
    secondaryContainer = MomoSecondaryContainerDark,
    onSecondaryContainer = MomoOnSecondaryContainerDark,
    tertiary = MomoTertiaryDark,
    onTertiary = MomoOnTertiaryDark,
    tertiaryContainer = MomoTertiaryContainerDark,
    onTertiaryContainer = MomoOnTertiaryContainerDark,
    background = MomoBackgroundDark,
    onBackground = MomoOnBackgroundDark,
    surface = MomoSurfaceDark,
    onSurface = MomoOnSurfaceDark,
    surfaceVariant = MomoSurfaceVariantDark,
    onSurfaceVariant = MomoOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = MomoPrimaryLight,
    onPrimary = MomoOnPrimaryLight,
    primaryContainer = MomoPrimaryContainerLight,
    onPrimaryContainer = MomoOnPrimaryContainerLight,
    secondary = MomoSecondaryLight,
    onSecondary = MomoOnSecondaryLight,
    secondaryContainer = MomoSecondaryContainerLight,
    onSecondaryContainer = MomoOnSecondaryContainerLight,
    tertiary = MomoTertiaryLight,
    onTertiary = MomoOnTertiaryLight,
    tertiaryContainer = MomoTertiaryContainerLight,
    onTertiaryContainer = MomoOnTertiaryContainerLight,
    background = MomoBackgroundLight,
    onBackground = MomoOnBackgroundLight,
    surface = MomoSurfaceLight,
    onSurface = MomoOnSurfaceLight,
    surfaceVariant = MomoSurfaceVariantLight,
    onSurfaceVariant = MomoOnSurfaceVariantLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded Momo palette by default for distinctive character
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
