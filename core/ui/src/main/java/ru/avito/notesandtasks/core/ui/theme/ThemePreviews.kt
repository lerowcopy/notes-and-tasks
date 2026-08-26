package ru.avito.notesandtasks.core.ui.theme

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ru.avito.notesandtasks.core.ui.components.SearchTopBar

@Preview(name = "Blue light")
@Composable
private fun BlueLightPreview() = ThemePreview(
    darkTheme = false,
    accentColor = AccentColor.BLUE,
)

@Preview(name = "Blue dark")
@Composable
private fun BlueDarkPreview() = ThemePreview(
    darkTheme = true,
    accentColor = AccentColor.BLUE,
)

@Preview(name = "Violet light")
@Composable
private fun VioletLightPreview() = ThemePreview(
    darkTheme = false,
    accentColor = AccentColor.VIOLET,
)

@Preview(name = "Violet dark")
@Composable
private fun VioletDarkPreview() = ThemePreview(
    darkTheme = true,
    accentColor = AccentColor.VIOLET,
)

@Preview(name = "Green light")
@Composable
private fun GreenLightPreview() = ThemePreview(
    darkTheme = false,
    accentColor = AccentColor.GREEN,
)

@Preview(name = "Green dark")
@Composable
private fun GreenDarkPreview() = ThemePreview(
    darkTheme = true,
    accentColor = AccentColor.GREEN,
)

@Preview(name = "Orange light")
@Composable
private fun OrangeLightPreview() = ThemePreview(
    darkTheme = false,
    accentColor = AccentColor.ORANGE,
)

@Preview(name = "Orange dark")
@Composable
private fun OrangeDarkPreview() = ThemePreview(
    darkTheme = true,
    accentColor = AccentColor.ORANGE,
)

@Preview(name = "Red light")
@Composable
private fun RedLightPreview() = ThemePreview(
    darkTheme = false,
    accentColor = AccentColor.RED,
)

@Preview(name = "Red dark")
@Composable
private fun RedDarkPreview() = ThemePreview(
    darkTheme = true,
    accentColor = AccentColor.RED,
)

@Composable
private fun ThemePreview(
    darkTheme: Boolean,
    accentColor: AccentColor,
) {
    NotesAndTasksTheme(
        darkTheme = darkTheme,
        accentColor = accentColor,
    ) {
        Surface {
            SearchTopBar(
                query = "",
                onQueryChange = {},
                onSearch = {},
            )
        }
    }
}
