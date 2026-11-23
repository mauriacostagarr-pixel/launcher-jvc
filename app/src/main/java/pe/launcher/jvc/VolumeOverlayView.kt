package pe.launcher.jvc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import android.view.animation.PathInterpolator

/** Barra de volumen vertical (0–100) dibujada a mano, ligera para TVs modestas. */
class VolumeOverlayView(ctx: Context, private val u: Float) : View(ctx) {
    private var light = false
    private var level = 0f
    private var number = 0
    private var muted = false
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val ease = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    private var levelAnim: ValueAnimator? = null
    private var showAnim: ValueAnimator? = null
    private var shown = 0f
    private val hideTask = Runnable { animateShown(0f) }

    init {
        visibility = GONE
        alpha = 0f
    }

    fun update(vol: Int, isMuted: Boolean, isLight: Boolean) {
        light = isLight
        muted = isMuted
        number = vol
        val target = vol / 100f
        levelAnim?.cancel()
        levelAnim = ValueAnimator.ofFloat(level, target).apply {
            duration = 350
            interpolator = ease
            addUpdateListener { level = it.animatedValue as Float; invalidate() }
            start()
        }
        removeCallbacks(hideTask)
        animateShown(1f)
        postDelayed(hideTask, 2200)
        invalidate()
    }

    private fun animateShown(to: Float) {
        showAnim?.cancel()
        if (to > 0f) visibility = VISIBLE
        showAnim = ValueAnimator.ofFloat(shown, to).apply {
            duration = 450
            interpolator = ease
            addUpdateListener {
                shown = it.animatedValue as Float
                alpha = shown
                translationX = -(1f - shown) * 40f * u
                if (shown <= 0.001f && to == 0f) visibility = GONE
            }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val fg = if (light) 0xFF1D1D1F.toInt() else 0xFFF5F5F7.toInt()
        val glass = if (light) 0xD6FFFFFF.toInt() else 0xCC1E1E24.toInt()
        val pill = if (light) 0x1F000000 else 0x33FFFFFF

        p.style = Paint.Style.FILL
        p.color = glass
        rect.set(0f, 0f, w, h)
        c.drawRoundRect(rect, 30f * u, 30f * u, p)

        p.color = fg
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 34f * u
        c.drawText(number.toString(), w / 2f, 62f * u, p)

        val top = 88f * u
        val bottom = h - 64f * u
        val tw = 8f * u
        p.color = pill
        rect.set(w / 2f - tw / 2f, top, w / 2f + tw / 2f, bottom)
        c.drawRoundRect(rect, tw / 2f, tw / 2f, p)

        p.color = fg
        val fillTop = bottom - (bottom - top) * level.coerceIn(0f, 1f)
        rect.set(w / 2f - tw / 2f, fillTop, w / 2f + tw / 2f, bottom)
        c.drawRoundRect(rect, tw / 2f, tw / 2f, p)

        p.typeface = Typeface.DEFAULT
        p.textSize = 15f * u
        c.drawText(if (muted || number == 0) "SILENCIO" else "VOL", w / 2f, h - 26f * u, p)
    }
}
