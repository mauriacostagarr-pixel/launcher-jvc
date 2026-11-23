package pe.launcher.jvc

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.media.AudioManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Intercepta los botones de volumen del control en cualquier app y muestra
 * la barra vertical del launcher. Solo lee esas teclas.
 */
class VolumeService : AccessibilityService() {
    private var view: VolumeOverlayView? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun ensureView(): VolumeOverlayView {
        view?.let { return it }
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val u = resources.displayMetrics.heightPixels / 720f
        val v = VolumeOverlayView(this, u)
        val lp = WindowManager.LayoutParams(
            (92 * u).toInt(),
            (460 * u).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        lp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        lp.x = (16 * u).toInt()
        wm.addView(v, lp)
        view = v
        return v
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN &&
            code != KeyEvent.KEYCODE_VOLUME_MUTE
        ) return false
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audio.isVolumeFixed) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            val dir = when (code) {
                KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
                KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
                else -> AudioManager.ADJUST_TOGGLE_MUTE
            }
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, 0)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
            val vol = if (muted) 0 else Math.round(cur * 100f / max)
            ensureView().update(vol, muted, Prefs(this).theme == "light")
        }
        return true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        view?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (e: Exception) {
            }
        }
        view = null
        return super.onUnbind(intent)
    }
}
