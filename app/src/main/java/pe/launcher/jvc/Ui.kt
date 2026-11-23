package pe.launcher.jvc

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun greeting(h: Int): String = when {
    h < 6 -> "Buenas noches"
    h < 12 -> "Buenos días"
    h < 19 -> "Buenas tardes"
    else -> "Buenas noches"
}

private val DAYS = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

@Composable
private fun T(
    text: String,
    size: Float,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, textAlign = align),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun LauncherRoot(c: LauncherController) {
    val pal = animatedPal(c.theme == "light")
    val isHome = c.screen == Screen.Home
    val shift = if (isHome) (if (c.panelOpen) 0.45f else 0f) else 1f
    val fx = if (isHome) (c.focus - 2).coerceIn(-3, 3).toFloat() else 0f

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val k = maxWidth.value / 1280f
        Estelas(pal.bg, palette(c.palette), c.motion, shift, fx)

        val homeA by animateFloatAsState(if (isHome) 1f else 0f, tween(if (isHome) 700 else 450, delayMillis = if (isHome) 350 else 0), label = "homeA")
        val appsA by animateFloatAsState(if (!isHome) 1f else 0f, tween(if (!isHome) 700 else 400, delayMillis = if (!isHome) 350 else 0), label = "appsA")

        if (homeA > 0.01f) HomeLayer(c, pal, k, homeA)
        if (appsA > 0.01f) AppsLayer(c, pal, k, appsA)

        // Velo + panel de ajustes
        val panelA by animateFloatAsState(if (c.panelOpen) 1f else 0f, tween(600, easing = Ease), label = "panelA")
        if (panelA > 0.01f) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = panelA }.background(pal.scrim))
            PanelLayer(c, pal, k, panelA)
        }

        VolumeBar(c, pal, k)
    }
}

@Composable
private fun HomeLayer(c: LauncherController, pal: Pal, k: Float, alpha: Float) {
    var now by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Calendar.getInstance()
            delay(15000)
        }
    }
    val h = now.get(Calendar.HOUR_OF_DAY)
    val m = now.get(Calendar.MINUTE)
    val time = String.format("%02d:%02d", h, m)
    val date = "${DAYS[now.get(Calendar.DAY_OF_WEEK) - 1].replaceFirstChar { it.uppercase() }}, ${now.get(Calendar.DAY_OF_MONTH)} de ${MONTHS[now.get(Calendar.MONTH)]}"

    val home = c.homeApps
    val n = c.cardCount
    val title = when {
        c.focus < home.size -> home[c.focus].label
        c.focus == home.size -> "Todas las apps"
        else -> "Ajustes"
    }

    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        // Saludo
        Column(Modifier.padding(start = (72 * k).dp, top = (52 * k).dp)) {
            T(greeting(h), 30f * k, pal.fg, FontWeight.SemiBold)
            T(if (c.editing) "Modo edición · OK quita · ◄ ► mueve · Atrás termina" else "Mauricio", 20f * k, pal.muted)
        }
        // Reloj
        Column(
            Modifier.align(Alignment.TopEnd).padding(end = (72 * k).dp, top = (44 * k).dp),
            horizontalAlignment = Alignment.End,
        ) {
            T(time, 54f * k, pal.fg, FontWeight.Light)
            T(date, 20f * k, pal.muted)
        }
        // Nombre grande
        T(
            title,
            78f * k,
            pal.fg,
            FontWeight.Bold,
            Modifier.align(Alignment.CenterStart).padding(start = (72 * k).dp, bottom = (110 * k).dp),
        )

        // Fila de tarjetas
        val density = LocalDensity.current
        val cardW = 288 * k
        val gap = 32 * k
        val step = cardW + gap
        val viewW = 1280 * k - 2 * 72 * k
        val total = n * step - gap
        val target = min(max(0f, (c.focus - 1) * step), max(0f, total - viewW))
        val scrollDp by animateFloatAsState(target, tween(800, easing = Ease), label = "scroll")

        Box(
            Modifier.align(Alignment.BottomStart).padding(start = (72 * k).dp, bottom = (96 * k).dp)
                .fillMaxWidth().height((260 * k).dp)
        ) {
            Row(
                Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                    .offset { IntOffset(-(scrollDp * density.density).roundToInt(), 0) }
                    .padding(top = (40 * k).dp),
                horizontalArrangement = Arrangement.spacedBy(gap.dp),
            ) {
                for (i in 0 until n) {
                    val focused = i == c.focus
                    when {
                        i < home.size -> {
                            val a = home[i]
                            Card(k, pal, focused, c.editing, a.accent, a.banner, a.icon, a.label.take(1).uppercase(), if (a.banner == null) a.label else null, i)
                        }
                        i == home.size -> Card(k, pal, focused, false, Color(0xFF8E8E93), null, null, "▦", "Todas las apps", i)
                        else -> Card(k, pal, focused, false, Color(0xFFBF5AF2), null, null, "⚙", "Ajustes", i)
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(
    k: Float,
    pal: Pal,
    focused: Boolean,
    editing: Boolean,
    accent: Color,
    banner: androidx.compose.ui.graphics.ImageBitmap?,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    glyph: String,
    label: String?,
    index: Int,
) {
    val lift by animateFloatAsState(if (focused) 1f else 0f, tween(550, easing = Ease), label = "lift$index")
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(650, delayMillis = if (focused) 120 else 0, easing = Ease), label = "glow$index")
    val w = (288 * k).dp
    val hgt = (144 * k).dp
    Box(Modifier.width(w).height(hgt + (60 * k).dp)) {
        // Reflejo / resplandor del color de la app: solo en la seleccionada
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = w * 0.06f, y = hgt - (6 * k).dp)
                .width(w * 0.88f).height((54 * k).dp)
                .graphicsLayer { alpha = glow * 0.85f; scaleX = 1f + 0.08f * lift }
                .background(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent.copy(alpha = 0.25f), Color.Transparent)),
                    RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
                )
        )
        Box(
            Modifier.size(w, hgt)
                .graphicsLayer {
                    translationY = -14f * k * density * lift
                    scaleX = 1f + 0.1f * lift
                    scaleY = 1f + 0.1f * lift
                    if (editing && focused) rotationZ = 0.8f
                }
                .clip(RoundedCornerShape((22 * k).dp))
                .background(if (banner != null) Color(0xFF1C1C1E) else pal.glass)
                .drawBehind {
                    if (focused) drawRect(Color.White.copy(alpha = 0.06f))
                },
            contentAlignment = Alignment.Center,
        ) {
            if (banner != null) {
                Image(banner, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (icon != null) {
                        Image(icon, null, Modifier.size((64 * k).dp))
                    } else {
                        T(glyph, 56f * k, pal.fg, FontWeight.Bold, align = TextAlign.Center)
                    }
                    if (label != null) T(label, 22f * k, pal.fg, FontWeight.Medium, Modifier.padding(top = (6 * k).dp, start = 12.dp, end = 12.dp), TextAlign.Center)
                }
            }
            if (editing && focused) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding((10 * k).dp).size((30 * k).dp)
                        .background(Color(0xFFFF3B30), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) { T("−", 22f * k, Color.White, FontWeight.Bold, align = TextAlign.Center) }
            }
        }
    }
}

@Composable
private fun AppsLayer(c: LauncherController, pal: Pal, k: Float, alpha: Float) {
    val state = rememberLazyGridState()
    LaunchedEffect(c.gridFocus) {
        val row = c.gridFocus / 6
        state.animateScrollToItem(max(0, row - 1) * 6)
    }
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        Column(Modifier.padding(start = (72 * k).dp, top = (52 * k).dp)) {
            T("Todas las apps", 40f * k, pal.fg, FontWeight.Bold)
            T("OK abre · mantén OK para añadir o quitar del inicio · Atrás vuelve", 18f * k, pal.muted)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = state,
            modifier = Modifier.fillMaxSize().padding(start = (72 * k).dp, end = (72 * k).dp, top = (150 * k).dp, bottom = (30 * k).dp),
            horizontalArrangement = Arrangement.spacedBy((24 * k).dp),
            verticalArrangement = Arrangement.spacedBy((24 * k).dp),
        ) {
            itemsIndexed(c.apps) { i, a ->
                val f = i == c.gridFocus
                val s by animateFloatAsState(if (f) 1f else 0f, tween(450, easing = Ease), label = "g$i")
                val pinned = a.pkg in c.homeIds
                Column(
                    Modifier.graphicsLayer { scaleX = 1f + 0.08f * s; scaleY = 1f + 0.08f * s }
                        .clip(RoundedCornerShape((20 * k).dp))
                        .background(if (f) pal.focusBg else pal.glass)
                        .padding((14 * k).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size((84 * k).dp), contentAlignment = Alignment.Center) {
                        val img = a.icon ?: a.banner
                        if (img != null) Image(img, null, Modifier.fillMaxSize())
                    }
                    T(a.label, 18f * k, if (f) pal.focusFg else pal.fg, FontWeight.Medium, Modifier.padding(top = (8 * k).dp), TextAlign.Center)
                    T(if (pinned) "En inicio" else " ", 14f * k, if (f) pal.focusFg else pal.muted, align = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun PanelLayer(c: LauncherController, pal: Pal, k: Float, alpha: Float) {
    val rows = c.rows(c.panelPage)
    val title = when (c.panelPage) {
        0 -> "Ajustes"
        1 -> "Sonido"
        else -> "Entradas"
    }
    val slide = (1f - alpha) * 60f * k
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = (24 * k).dp)
                .width((460 * k).dp)
                .graphicsLayer { translationX = slide * density; this.alpha = alpha }
                .clip(RoundedCornerShape((32 * k).dp))
                .background(pal.glassStrong)
                .padding((28 * k).dp),
            verticalArrangement = Arrangement.spacedBy((10 * k).dp),
        ) {
            T(title, 36f * k, pal.fg, FontWeight.Bold)
            if (c.panelPage == 1) {
                T("Salidas detectadas:", 16f * k, pal.muted)
                BasicText(
                    c.outputsLabel,
                    style = TextStyle(color = pal.fg, fontSize = (18f * k).sp),
                    modifier = Modifier.padding(bottom = (6 * k).dp),
                )
                T("Para cambiar entre altavoces y barra de sonido usa los atajos de abajo.", 15f * k, pal.muted)
            }
            rows.forEachIndexed { i, r ->
                val f = i == c.panelFocus
                val s by animateFloatAsState(if (f) 1f else 0f, tween(400, easing = Ease), label = "p$i")
                Row(
                    Modifier.fillMaxWidth()
                        .graphicsLayer { scaleX = 1f + 0.02f * s; scaleY = 1f + 0.02f * s }
                        .clip(RoundedCornerShape((16 * k).dp))
                        .background(if (f) pal.focusBg else pal.pill)
                        .padding(horizontal = (20 * k).dp, vertical = (16 * k).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    T(r.label, 22f * k, if (f) pal.focusFg else pal.fg, FontWeight.Medium, Modifier.weight(1f))
                    T(r.value, 20f * k, if (f) pal.focusFg else pal.muted, align = TextAlign.End)
                }
            }
            T(if (c.panelPage == 0) "▲ ▼ elegir · OK cambiar · Atrás cierra" else "Atrás vuelve", 15f * k, pal.muted)
        }
    }
}

@Composable
private fun VolumeBar(c: LauncherController, pal: Pal, k: Float) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(c.volTick) {
        if (c.volTick > 0) {
            visible = true
            delay(2200)
            visible = false
        }
    }
    val a by animateFloatAsState(if (visible) 1f else 0f, tween(450, easing = Ease), label = "volA")
    val level by animateFloatAsState(c.volume / 100f, tween(350, easing = Ease), label = "volL")
    if (a < 0.01f) return
    val barH = (330 * k).dp
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.CenterStart).padding(start = (22 * k).dp)
                .graphicsLayer { alpha = a; translationX = -(1f - a) * 40f * density * k }
                .clip(RoundedCornerShape((30 * k).dp))
                .background(pal.glassStrong)
                .padding(horizontal = (22 * k).dp, vertical = (20 * k).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((12 * k).dp),
        ) {
            T(c.volume.toString(), 34f * k, pal.fg, FontWeight.Bold, align = TextAlign.Center)
            Box(
                Modifier.width((8 * k).dp).height(barH)
                    .clip(RoundedCornerShape(50))
                    .background(pal.pill),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(level.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(50))
                        .background(pal.fg)
                )
            }
            T(if (c.volume == 0) "🔇" else "🔊", 22f * k, pal.fg, align = TextAlign.Center)
        }
    }
}
