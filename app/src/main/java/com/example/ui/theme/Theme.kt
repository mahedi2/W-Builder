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
    primary = StudioPrimaryDark,
    onPrimary = StudioOnPrimaryDark,
    primaryContainer = StudioPrimaryContainerDark,
    onPrimaryContainer = StudioOnPrimaryContainerDark,
    secondary = StudioSecondaryDark,
    onSecondary = StudioOnSecondaryDark,
    secondaryContainer = StudioSecondaryContainerDark,
    onSecondaryContainer = StudioOnSecondaryContainerDark,
    tertiary = StudioTertiaryDark,
    onTertiary = StudioOnTertiaryDark,
    tertiaryContainer = StudioTertiaryContainerDark,
    onTertiaryContainer = StudioOnTertiaryContainerDark,
    background = StudioBackgroundDark,
    onBackground = StudioOnSurfaceDark,
    surface = StudioSurfaceDark,
    onSurface = StudioOnSurfaceDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onSurfaceVariant = StudioOnSurfaceVariantDark,
    outline = StudioOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = StudioPrimaryLight,
    onPrimary = StudioOnPrimaryLight,
    primaryContainer = StudioPrimaryContainerLight,
    onPrimaryContainer = StudioOnPrimaryContainerLight,
    secondary = StudioSecondaryLight,
    onSecondary = StudioOnSecondaryLight,
    secondaryContainer = StudioSecondaryContainerLight,
    onSecondaryContainer = StudioOnSecondaryContainerLight,
    tertiary = StudioTertiaryLight,
    onTertiary = StudioOnTertiaryLight,
    tertiaryContainer = StudioTertiaryContainerLight,
    onTertiaryContainer = StudioOnTertiaryContainerLight,
    background = StudioBackgroundLight,
    onBackground = StudioOnSurfaceLight,
    surface = StudioSurfaceLight,
    onSurface = StudioOnSurfaceLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onSurfaceVariant = StudioOnSurfaceVariantLight,
    outline = StudioOutlineLight
)

@Composable
fun WBuilderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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
