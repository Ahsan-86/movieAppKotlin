package com.ahsan.movieapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = AmberAccent,
    onPrimary = Charcoal900,
    secondary = FavoriteRed,
    onSecondary = OnCharcoalPrimary,
    background = Charcoal900,
    onBackground = OnCharcoalPrimary,
    surface = Charcoal800,
    onSurface = OnCharcoalPrimary,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = OnCharcoalSecondary,
    error = ErrorRed,
    outline = Charcoal600
)

private val LightColors = lightColorScheme(
    primary = AmberAccentDark,
    onPrimary = Cream100,
    secondary = FavoriteRed,
    onSecondary = Cream100,
    background = Cream100,
    onBackground = OnCreamPrimary,
    surface = Cream200,
    onSurface = OnCreamPrimary,
    surfaceVariant = Cream300,
    onSurfaceVariant = OnCreamSecondary,
    error = ErrorRed,
    outline = Cream300
)

/**
 * App-wide theme. Defaults to a cinematic dark palette regardless of system theme (movie posters
 * and backdrops read best on dark), but respects the system's light/dark choice, and layers
 * Material You dynamic color on top on Android 12+ if the person has that enabled.
 */
@Composable
fun MovieAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MovieTypography,
        content = content
    )
}
