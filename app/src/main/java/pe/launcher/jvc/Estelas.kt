package pe.launcher.jvc

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer

private fun gradA(a: Color, b: Color, c: Color, w: Float): Brush = Brush.horizontalGradient(
    0f to a.copy(alpha = 0f),
    0.35f to a.copy(alpha = 0.95f),
    0.7f to c.copy(alpha = 0.95f),
    1f to b.copy(alpha = 0f),
    startX = 0f,
    endX = w,
)

private fun gradB(a: Color, b: Color, c: Color, w: Float): Brush = Brush.horizontalGradient(
    0f to b.copy(alpha = 0f),
    0.4f to b.copy(alpha = 0.9f),
    0.75f to a.copy(alpha = 0.9f),
    1f to c.copy(alpha = 0f),
    startX = 0f,
    endX = w,
)

/** Trazo ancho con bordes suaves (simula el desenfoque apilando trazos cada vez más finos). */
private fun DrawScope.softStroke(path: Path, brush: Brush, width: Float, alpha: Float) {
    val steps = 5
    for (i in 0 until steps) {
        val f = 1f - i.toFloat() / steps
        drawPath(path, brush, alpha = alpha * 0.38f, style = Stroke(width * f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.crisp(path: Path, brush: Brush, width: Float, alpha: Float) {
    drawPath(path, brush, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round))
}

@Composable
fun Estelas(
    bg: Color,
    colors: Triple<Color, Color, Color>,
    motion: Boolean,
    shift: Float,
    fx: Float,
) {
    val inf = rememberInfiniteTransition(label = "estelas")
    val dA by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dA",
    )
    val dB by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dB",
    )
    val dC by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dC",
    )
    val s by animateFloatAsState(targetValue = shift, animationSpec = tween(1400, easing = Ease), label = "shift")
    val f by animateFloatAsState(targetValue = fx, animationSpec = tween(1400, easing = Ease), label = "fx")
    val m = if (motion) 1f else 0f
    val ca = colors.first
    val cb = colors.second
    val cc = colors.third

    Box(Modifier.fillMaxSize().background(bg)) {
        // Resplandor superior derecho
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val k = size.width / 1280f
                translationX = ((-90f * s + 6f * f) + (-110f * dC * m)) * k
                translationY = ((70f * s) + (36f * dC * m)) * k
                val sc = 1f + 0.05f * dC * m
                scaleX = sc
                scaleY = sc
                rotationZ = 1.6f * dC * m
            }
        ) {
            val k = size.width / 1280f
            val p = Path().apply {
                moveTo(640f * k, -100f * k)
                cubicTo(860f * k, 110f * k, 1060f * k, 40f * k, 1380f * k, 250f * k)
            }
            softStroke(p, gradB(ca, cb, cc, size.width), 130f * k, 0.34f)
        }

        // Cinta principal
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val k = size.width / 1280f
                translationX = ((-150f * s + 14f * f) + (-110f * dA * m)) * k
                translationY = ((-70f * s) + (36f * dA * m)) * k
                val sc = 1f + 0.05f * dA * m
                scaleX = sc
                scaleY = sc
                rotationZ = -4f * s + 1.6f * dA * m
            }
        ) {
            val k = size.width / 1280f
            val g = gradA(ca, cb, cc, size.width)
            val soft = Path().apply {
                moveTo(-120f * k, 520f * k)
                cubicTo(250f * k, 370f * k, 520f * k, 710f * k, 860f * k, 470f * k)
                cubicTo(1200f * k, 230f * k, 1240f * k, 290f * k, 1420f * k, 380f * k)
            }
            softStroke(soft, g, 96f * k, 0.58f)
            val thin1 = Path().apply {
                moveTo(-100f * k, 560f * k)
                cubicTo(270f * k, 420f * k, 540f * k, 730f * k, 880f * k, 500f * k)
                cubicTo(1220f * k, 270f * k, 1240f * k, 340f * k, 1400f * k, 420f * k)
            }
            crisp(thin1, g, 2.5f * k, 0.75f)
            val thin2 = Path().apply {
                moveTo(-100f * k, 580f * k)
                cubicTo(270f * k, 440f * k, 540f * k, 750f * k, 880f * k, 520f * k)
                cubicTo(1220f * k, 290f * k, 1240f * k, 360f * k, 1400f * k, 440f * k)
            }
            crisp(thin2, g, 1.5f * k, 0.5f)
        }

        // Cinta inferior
        Canvas(
            Modifier.fillMaxSize().graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val k = size.width / 1280f
                translationX = ((110f * s - 22f * f) + (90f * dB * m)) * k
                translationY = ((50f * s) + (-44f * dB * m)) * k
                val sc = 1f + 0.04f * dB * m
                scaleX = sc
                scaleY = sc
                rotationZ = 3f * s - 1.8f * dB * m
            }
        ) {
            val k = size.width / 1280f
            val g = gradB(ca, cb, cc, size.width)
            val soft = Path().apply {
                moveTo(-120f * k, 650f * k)
                cubicTo(290f * k, 560f * k, 560f * k, 780f * k, 900f * k, 610f * k)
                cubicTo(1240f * k, 440f * k, 1260f * k, 520f * k, 1420f * k, 610f * k)
            }
            softStroke(soft, g, 64f * k, 0.52f)
            val thin = Path().apply {
                moveTo(-100f * k, 676f * k)
                cubicTo(300f * k, 590f * k, 570f * k, 800f * k, 910f * k, 636f * k)
                cubicTo(1250f * k, 472f * k, 1260f * k, 546f * k, 1400f * k, 636f * k)
            }
            crisp(thin, g, 2f * k, 0.65f)
        }
    }
}
