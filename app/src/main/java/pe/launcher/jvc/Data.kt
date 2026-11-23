package pe.launcher.jvc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

enum class Screen { Home, Apps }

data class AppEntry(
    val pkg: String,
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val accent: Color,
    val isLeanback: Boolean,
)

data class CardData(
    val label: String,
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val accent: Color,
    val glyph: Int,
)

data class PanelRow(val label: String, val value: String, val action: () -> Unit)

data class InputEntry(val label: String, val launch: Intent)

object AppRepository {
    fun load(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val leanIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val stdIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val lean = pm.queryIntentActivities(leanIntent, 0)
        val std = pm.queryIntentActivities(stdIntent, 0)
        val leanPkgs = lean.map { it.activityInfo.packageName }.toSet()
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (ri in lean + std) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName || !seen.add(pkg)) continue
            val label = ri.loadLabel(pm).toString()
            val bannerDrawable = runCatching {
                ri.activityInfo.loadBanner(pm) ?: ri.activityInfo.applicationInfo.loadBanner(pm)
            }.getOrNull()
            val iconDrawable = runCatching { ri.loadIcon(pm) }.getOrNull()
            val bannerBmp = bannerDrawable?.let { d -> runCatching { d.toBitmap(480, 270) }.getOrNull() }
            val iconBmp = iconDrawable?.let { d -> runCatching { d.toBitmap(160, 160) }.getOrNull() }
            val accent = dominantColor(bannerBmp ?: iconBmp)
            out.add(
                AppEntry(
                    pkg = pkg,
                    label = label,
                    banner = bannerBmp?.asImageBitmap(),
                    icon = iconBmp?.asImageBitmap(),
                    accent = accent,
                    isLeanback = pkg in leanPkgs,
                )
            )
        }
        return out
    }
}

/** Color dominante (favoreciendo los saturados) para el resplandor de cada tarjeta. */
fun dominantColor(bmp: Bitmap?): Color {
    val fallback = Color(0xFF8E8E93)
    if (bmp == null) return fallback
    val small = Bitmap.createScaledBitmap(bmp, 24, 24, true)
    val px = IntArray(24 * 24)
    small.getPixels(px, 0, 24, 0, 0, 24, 24)
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var ws = 0.0
    for (p in px) {
        val a = (p ushr 24) and 0xFF
        if (a < 40) continue
        val rr = (p shr 16) and 0xFF
        val gg = (p shr 8) and 0xFF
        val bb = p and 0xFF
        val mx = maxOf(rr, gg, bb)
        val mn = minOf(rr, gg, bb)
        val sat = if (mx == 0) 0.0 else (mx - mn).toDouble() / mx
        val w = sat * sat + 0.02
        r += rr * w
        g += gg * w
        b += bb * w
        ws += w
    }
    if (ws <= 0.0) return fallback
    var cr = (r / ws).toInt()
    var cg = (g / ws).toInt()
    var cb = (b / ws).toInt()
    val top = maxOf(cr, cg, cb)
    if (top in 1..139) {
        val k = 160f / top
        cr = minOf(255, (cr * k).toInt())
        cg = minOf(255, (cg * k).toInt())
        cb = minOf(255, (cb * k).toInt())
    }
    return Color(cr, cg, cb)
}
