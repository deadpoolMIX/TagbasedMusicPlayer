package com.tagplayer.musicplayer.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.tagplayer.musicplayer.ui.settings.viewmodel.ThemeMode

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }

fun shouldUseDarkTheme(themeMode: ThemeMode, isSystemDark: Boolean): Boolean {
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }
}
