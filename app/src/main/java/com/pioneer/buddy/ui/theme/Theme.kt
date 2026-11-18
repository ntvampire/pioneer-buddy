package com.pioneer.buddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0D0F12)
val CardBackground = Color(0xFF16191E)
val CardBorder = Color(0xFF262B33)

val PioneerCyan = Color(0xFF00E5FF)
val PioneerAmber = Color(0xFFFF9100)
val PioneerRed = Color(0xFFFF1744)

val TextPrimary = Color(0xFFF0F4F8)
val TextSecondary = Color(0xFF8F9CAE)

private val DarkColors = darkColorScheme(
    primary = PioneerCyan,
    secondary = PioneerAmber,
    background = DarkBackground,
    surface = CardBackground,
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PioneerBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
