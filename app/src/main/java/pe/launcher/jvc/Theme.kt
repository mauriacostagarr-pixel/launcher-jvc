package pe.launcher.jvc

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/** Curva suave tipo Apple (frena al final), la misma del diseño. */
val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

data class Pal(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val glass: Color,
    val pill: Color,
    val focusBg: Color,
    val focusFg: Color,
    val glassStrong: Color,
    val scrim: Color,
)

val DarkPal = Pal(
    bg = Color(0xFF0B0B0D),
    fg = Color(0xFFF5F5F7),
    muted = Color(0xB8F5F5F7),
    glass = Color(0x8C2C2C32),
    pill = Color(0x24FFFFFF),
    focusBg = Color(0xFFF5F5F7),
    focusFg = Color(0xFF0B0B0D),
    glassStrong = Color(0xCC1E1E24),
    scrim = Color(0x73000000),
)

val LightPal = Pal(
    bg = Color(0xFFF5F5F7),
    fg = Color(0xFF1D1D1F),
    muted = Color(0xAD1D1D1F),
    glass = Color(0xA8FFFFFF),
    pill = Color(0x12000000),
    focusBg = Color(0xFF1D1D1F),
    focusFg = Color(0xFFF5F5F7),
    glassStrong = Color(0xD6FFFFFF),
    scrim = Color(0x80FFFFFF),
)

@Composable
fun animatedPal(light: Boolean): Pal {
    val t = if (light) LightPal else DarkPal
    val spec = tween<Color>(800)
    val bg by animateColorAsState(t.bg, spec, label = "bg")
    val fg by animateColorAsState(t.fg, spec, label = "fg")
    val muted by animateColorAsState(t.muted, spec, label = "muted")
    val glass by animateColorAsState(t.glass, spec, label = "glass")
    val pill by animateColorAsState(t.pill, spec, label = "pill")
    val focusBg by animateColorAsState(t.focusBg, spec, label = "focusBg")
    val focusFg by animateColorAsState(t.focusFg, spec, label = "focusFg")
    val glassStrong by animateColorAsState(t.glassStrong, spec, label = "glassStrong")
    val scrim by animateColorAsState(t.scrim, spec, label = "scrim")
    return Pal(bg, fg, muted, glass, pill, focusBg, focusFg, glassStrong, scrim)
}

/** Colores de las estelas: (principal, claro, acento). */
fun palette(name: String): Triple<Color, Color, Color> = when (name) {
    "atardecer" -> Triple(Color(0xFFFF375F), Color(0xFFFF9F0A), Color(0xFFBF5AF2))
    "grafito" -> Triple(Color(0xFF8E8E93), Color(0xFFD1D1D6), Color(0xFF636366))
    else -> Triple(Color(0xFF5E5CE6), Color(0xFF64D2FF), Color(0xFFBF5AF2))
}
