package com.egepaksoy.interner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Kartlar `surfaceContainerLow` rengini kullanır: açık temada beyaz, koyu temada zeminden bir ton açık.
private val Light = lightColorScheme(
    primary = Color(0xFF0B4F9E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FB),
    onPrimaryContainer = Color(0xFF062A57),
    secondary = Color(0xFF55607A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3E8F2),
    onSecondaryContainer = Color(0xFF1B2333),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE7C2),
    onTertiaryContainer = Color(0xFF4A2B00),
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF151A22),
    surface = Color(0xFFF2F4F8),
    onSurface = Color(0xFF151A22),
    surfaceVariant = Color(0xFFE3E7EE),
    onSurfaceVariant = Color(0xFF596273),
    outline = Color(0xFF8891A1),
    outlineVariant = Color(0xFFD5DAE3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF7F8FB),
    surfaceContainerHigh = Color(0xFFE8EBF1),
    surfaceContainerHighest = Color(0xFFE1E5EC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CC4FF),
    onPrimary = Color(0xFF002F63),
    primaryContainer = Color(0xFF123A6B),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFFBCC6DC),
    onSecondary = Color(0xFF263042),
    secondaryContainer = Color(0xFF2B3342),
    onSecondaryContainer = Color(0xFFD9E2F5),
    tertiary = Color(0xFFFFC069),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF5C3A05),
    onTertiaryContainer = Color(0xFFFFE2B8),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE4E7EE),
    surface = Color(0xFF0E1116),
    onSurface = Color(0xFFE4E7EE),
    surfaceVariant = Color(0xFF2A303A),
    onSurfaceVariant = Color(0xFFA9B1C0),
    outline = Color(0xFF7D8696),
    outlineVariant = Color(0xFF2E343E),
    surfaceContainerLowest = Color(0xFF0A0C10),
    surfaceContainerLow = Color(0xFF181C23),
    surfaceContainer = Color(0xFF14181E),
    surfaceContainerHigh = Color(0xFF232830),
    surfaceContainerHighest = Color(0xFF2D333C),
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Çalışma alanı → (açık temadaki renk, koyu temadaki renk). */
private val AreaColors = mapOf(
    "Hat Bakım" to (Color(0xFF1565C0) to Color(0xFF90CAF9)),
    "Hangar / Üs Bakım" to (Color(0xFF00796B) to Color(0xFF80CBC4)),
    "C-Check" to (Color(0xFF6A3FB5) to Color(0xFFCDB5F5)),
    "Kabin" to (Color(0xFFC2185B) to Color(0xFFF8A9C5)),
    "Yapısal" to (Color(0xFFD84315) to Color(0xFFFFAB91)),
    "Takımhane" to (Color(0xFF8D6E00) to Color(0xFFE6C95C)),
    "Eğitim" to (Color(0xFF2E7D32) to Color(0xFFA5D6A7)),
)
private val OtherArea = Color(0xFF546E7A) to Color(0xFFB0BEC5)

/** Bir çalışma alanının vurgu rengi; listede olmayan (eski ya da boş) alanlar gri olur. */
@Composable
fun areaColor(area: String): Color {
    val pair = AreaColors[area] ?: OtherArea
    return if (isSystemInDarkTheme()) pair.second else pair.first
}

@Composable
fun InternerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
