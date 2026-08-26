package ru.avito.notesandtasks.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.avito.notesandtasks.core.common.settings.AccentColor

/**
 * Каждая ветка возвращает полный [ColorScheme] через фабрики Material 3. Меняются только
 * акцентные tonal-роли, а остальные роли остаются согласованными системными значениями Material.
 */
fun AccentColor.colorScheme(isDarkTheme: Boolean): ColorScheme = when (this) {
    AccentColor.BLUE -> createColorScheme(
        isDarkTheme = isDarkTheme,
        lightPrimary = Color(0xFF245FA6),
        lightPrimaryContainer = Color(0xFFD4E3FF),
        darkPrimary = Color(0xFFA9C8FF),
        darkPrimaryContainer = Color(0xFF004A88),
    )

    AccentColor.VIOLET -> createColorScheme(
        isDarkTheme = isDarkTheme,
        lightPrimary = Color(0xFF6F4FA2),
        lightPrimaryContainer = Color(0xFFEBDDFF),
        darkPrimary = Color(0xFFD7B8FF),
        darkPrimaryContainer = Color(0xFF573584),
    )

    AccentColor.GREEN -> createColorScheme(
        isDarkTheme = isDarkTheme,
        lightPrimary = Color(0xFF286A43),
        lightPrimaryContainer = Color(0xFFAAEFBA),
        darkPrimary = Color(0xFF8CD39D),
        darkPrimaryContainer = Color(0xFF0B512D),
    )

    AccentColor.ORANGE -> createColorScheme(
        isDarkTheme = isDarkTheme,
        lightPrimary = Color(0xFF965000),
        lightPrimaryContainer = Color(0xFFFFDCC1),
        darkPrimary = Color(0xFFFFB77D),
        darkPrimaryContainer = Color(0xFF743C00),
    )

    AccentColor.RED -> createColorScheme(
        isDarkTheme = isDarkTheme,
        lightPrimary = Color(0xFFAD2D3B),
        lightPrimaryContainer = Color(0xFFFFD9DC),
        darkPrimary = Color(0xFFFFB3BA),
        darkPrimaryContainer = Color(0xFF8B1127),
    )
}

@Composable
fun NotesAndTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = accentColor.colorScheme(isDarkTheme = darkTheme),
        content = content,
    )
}

object Spacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 24.dp
    val huge: Dp = 32.dp
}

private fun createColorScheme(
    isDarkTheme: Boolean,
    lightPrimary: Color,
    lightPrimaryContainer: Color,
    darkPrimary: Color,
    darkPrimaryContainer: Color,
): ColorScheme = if (isDarkTheme) {
    darkColorScheme(
        primary = darkPrimary,
        onPrimary = Color(0xFF00315B),
        primaryContainer = darkPrimaryContainer,
        onPrimaryContainer = Color(0xFFD4E3FF),
        secondary = darkPrimary,
        tertiary = darkPrimary,
    )
} else {
    lightColorScheme(
        primary = lightPrimary,
        onPrimary = Color.White,
        primaryContainer = lightPrimaryContainer,
        onPrimaryContainer = Color(0xFF001C3A),
        secondary = lightPrimary,
        tertiary = lightPrimary,
    )
}
