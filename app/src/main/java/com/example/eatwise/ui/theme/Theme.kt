package com.example.eatwise.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Orange80,
    secondary = OrangeGrey80,
    tertiary = Peach80,
)
private val LightColorScheme = lightColorScheme(
    primary = CustomOrange,           // **Ustawiamy Orange40 jako primary dla jasnego motywu**
    secondary = Orange50,      // Możesz dostosować secondary i tertiary do palety pomarańczowej
    tertiary = Orange45,

    background = White,            // **Ustawiamy białe tło dla jasnego motywu**
    surface = White,               // **Ustawiamy białe powierzchnie dla jasnego motywu**
    onPrimary = Black,           // Tekst/ikony na 'primary' kolorze (pomarańczowym) powinny być białe dla kontrastu
    onSecondary = White,
    onTertiary = Orange80,
    onBackground = Black,         // **Tekst/ikony na białym tle powinny być czarne lub ciemnoszare**
    onSurface = Black,            // **Tekst/ikony na białych powierzchniach powinny być czarne lub ciemnoszare**
    onSurfaceVariant = DarkGray,   // Opcjonalnie: Ciemnoszary dla wariantów powierzchni (np. słabszy tekst)
)

@Composable
fun EatWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )

}