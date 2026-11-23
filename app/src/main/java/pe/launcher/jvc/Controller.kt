package pe.launcher.jvc

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.tv.TvContract
import android.media.tv.TvInputManager
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado y lógica del launcher. Los botones del control remoto llegan aquí
 * desde MainActivity.dispatchKeyEvent.
 */
class LauncherController(private val ctx: Context) {
    private val prefs = Prefs(ctx)
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var screen by mutableStateOf(Screen.Home)
    var apps by mutableStateOf<List<AppEntry>>(emptyList())
    var homeIds by mutableStateOf<List<String>>(emptyList())
    var focus by mutableStateOf(0)
    var editing by mutableStateOf(false)
    var gridFocus by mutableStateOf(0)

    var panelOpen by mutableStateOf(false)
    var panelPage by mutableStateOf(0)
    var panelFocus by mutableStateOf(0)

    var theme by mutableStateOf(prefs.theme)
    var palette by mutableStateOf(prefs.palette)
    var motion by mutableStateOf(prefs.motion)
    var customVolume by mutableStateOf(prefs.customVolume)

    var volume by mutableStateOf(0)
    var volVisible by mutableStateOf(false)
    var volTick by mutableStateOf(0)

    var inputs by mutableStateOf<List<InputEntry>>(emptyList())
    var outputsLabel by mutableStateOf("")

    private val pressed = HashSet<Int>()
    private var longFired = false
    private var lastNav = 0L

    init {
        syncVolume(false)
        refreshOutputs()
    }

    // ---------- Apps ----------

    val homeApps: List<AppEntry>
        get() {
            val m = apps.associateBy { it.pkg }
            return homeIds.mapNotNull { m[it] }
        }

    val cardCount: Int
        get() = homeApps.size + 2

    fun setApps(list: List<AppEntry>) {
        val sorted = list.sortedBy { it.label.lowercase() }
        apps = sorted
        val byPkg = sorted.associateBy { it.pkg }
        val stored = prefs.home
        val seenOld = prefs.seen
        val home = ArrayList<String>()
        if (stored == null) {
            for (k in KNOWN) if (byPkg.containsKey(k)) home.add(k)
            for (a in sorted) if (a.isLeanback && a.pkg !in home) home.add(a.pkg)
        } else {
            for (p in stored) if (byPkg.containsKey(p)) home.add(p)
            for (a in sorted) if (a.isLeanback && a.pkg !in seenOld && a.pkg !in home) home.add(a.pkg)
        }
        prefs.home = home
        prefs.seen = seenOld + sorted.map { it.pkg }
        homeIds = home
        focus = focus.coerceIn(0, cardCount - 1)
        gridFocus = gridFocus.coerceIn(0, maxOf(0, sorted.size - 1))
    }

    fun launch(pkg: String) {
        val pm = ctx.packageManager
        val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (i == null) {
            toast("No se pudo abrir la app")
            return
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            ctx.startActivity(i)
        } catch (e: Exception) {
            toast("No se pudo abrir la app")
        }
    }

    private fun togglePin(pkg: String) {
        val l = homeIds.toMutableList()
        if (l.remove(pkg)) {
            toast("Quitada del inicio")
        } else {
            l.add(pkg)
            toast("Añadida al inicio")
        }
        homeIds = l
        prefs.home = l
    }

    private fun removeFocused() {
        val a = homeApps.getOrNull(focus) ?: return
        val l = homeIds.toMutableList()
        l.remove(a.pkg)
        homeIds = l
        prefs.home = l
        focus = focus.coerceIn(0, cardCount - 1)
    }

    private fun moveFocused(dir: Int) {
        val target = focus + dir
        if (focus >= homeIds.size || target < 0 || target >= homeIds.size) return
        val l = homeIds.toMutableList()
        val tmp = l[focus]
        l[focus] = l[target]
        l[target] = tmp
        homeIds = l
        prefs.home = l
        focus = target
    }

    private fun activateCard() {
        val h = homeApps
        when {
            focus < h.size -> launch(h[focus].pkg)
            focus == h.size -> {
                screen = Screen.Apps
                gridFocus = 0
                editing = false
            }
            else -> openPanel()
        }
    }

    fun goHomeFromSystem() {
        screen = Screen.Home
        panelOpen = false
        editing = false
        focus = 0
    }

    // ---------- Ajustes ----------

    fun openPanel() {
        panelOpen = true
        panelPage = 0
        panelFocus = 0
    }

    private fun openPage(n: Int) {
        panelPage = n
        panelFocus = 0
        if (n == 1) refreshOutputs()
        if (n == 2) loadInputs()
    }

    private fun toggleTheme() {
        theme = if (theme == "light") "dark" else "light"
        prefs.theme = theme
    }

    private fun cyclePalette() {
        val order = listOf("aurora", "atardecer", "grafito")
        palette = order[(order.indexOf(palette) + 1) % order.size]
        prefs.palette = palette
    }

    fun rows(pg: Int): List<PanelRow> = when (pg) {
        0 -> listOf(
            PanelRow("Tema", if (theme == "light") "Claro" else "Oscuro") { toggleTheme() },
            PanelRow("Estelas", when (palette) {
                "atardecer" -> "Atardecer"
                "grafito" -> "Grafito"
                else -> "Aurora"
            }) { cyclePalette() },
            PanelRow("Movimiento del fondo", if (motion) "Activado" else "Desactivado") {
                motion = !motion
                prefs.motion = motion
            },
            PanelRow("Barra de volumen propia", if (customVolume) "Activada" else "Desactivada") {
                customVolume = !customVolume
                prefs.customVolume = customVolume
            },
            PanelRow("Sonido", "›") { openPage(1) },
            PanelRow("Entradas", "›") { openPage(2) },
            PanelRow("Editar inicio", "") {
                panelOpen = false
                screen = Screen.Home
                editing = true
            },
        )
        1 -> listOf(
            PanelRow("Abrir ajustes de sonido", "›") { openSoundSettings() },
            PanelRow("Abrir Bluetooth", "›") { openBluetoothSettings() },
        )
        else -> if (inputs.isEmpty()) {
            listOf(PanelRow("Abrir ajustes de la TV", "›") { openTvSettings() })
        } else {
            inputs.map { e -> PanelRow(e.label, "") { launchInput(e) } }
        }
    }

    private fun tryStart(i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        true
    } catch (e: Exception) {
        false
    }

    private fun openSoundSettings() {
        panelOpen = false
        if (!tryStart(Intent(Settings.ACTION_SOUND_SETTINGS)) && !tryStart(Intent(Settings.ACTION_SETTINGS))) {
            toast("No se pudo abrir los ajustes")
        }
    }

    private fun openBluetoothSettings() {
        panelOpen = false
        if (!tryStart(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) && !tryStart(Intent(Settings.ACTION_SETTINGS))) {
            toast("No se pudo abrir Bluetooth")
        }
    }

    private fun openTvSettings() {
        panelOpen = false
        if (!tryStart(Intent(Settings.ACTION_SETTINGS))) toast("No se pudo abrir los ajustes")
    }

    private fun launchInput(e: InputEntry) {
        panelOpen = false
        if (!tryStart(e.launch)) toast("No se pudo abrir la entrada")
    }

    private fun loadInputs() {
        val list = ArrayList<InputEntry>()
        try {
            val tm = ctx.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            if (tm != null) {
                for (info in tm.tvInputList) {
                    if (!info.isPassthroughInput) continue
                    val label = info.loadCustomLabel(ctx)?.toString()?.takeIf { it.isNotBlank() }
                        ?: info.loadLabel(ctx).toString()
                    val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.id))
                    list.add(InputEntry(label, intent))
                }
            }
        } catch (e: Exception) {
            // Sin acceso a las entradas: se mostrará el atajo a los ajustes de la TV.
        }
        val liveTv = ctx.packageManager.getLeanbackLaunchIntentForPackage("com.google.android.tv")
        if (liveTv != null) list.add(0, InputEntry("TV en vivo", liveTv))
        inputs = list
    }

    // ---------- Sonido y volumen ----------

    fun refreshOutputs() {
        val names = LinkedHashSet<String>()
        try {
            for (d in audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                when (d.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> names.add("Altavoces de la TV")
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        val n = d.productName?.toString()?.trim().orEmpty()
                        names.add(if (n.isEmpty()) "Bluetooth" else "Bluetooth ($n)")
                    }
                    AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> names.add("HDMI / ARC")
                    AudioDeviceInfo.TYPE_LINE_DIGITAL -> names.add("Salida óptica / digital")
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> names.add("Auriculares con cable")
                    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> names.add("USB")
                    else -> {}
                }
            }
        } catch (e: Exception) {
            // ignorar
        }
        outputsLabel = if (names.isEmpty()) "No se detectaron salidas" else names.joinToString("  ·  ")
    }

    fun syncVolume(show: Boolean) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val muted = audio.isStreamMute(AudioManager.STREAM_MUSIC)
        volume = if (muted) 0 else Math.round(cur * 100f / max)
        if (show) {
            volVisible = true
            volTick += 1
        }
    }

    private fun handleVolume(code: Int): Boolean {
        // Con volumen fijo (p. ej. salida óptica) o si el usuario lo desactiva, lo maneja la TV.
        if (!customVolume || audio.isVolumeFixed) return false
        val dir = when (code) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            else -> AudioManager.ADJUST_TOGGLE_MUTE
        }
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, 0)
        syncVolume(true)
        pressed.add(code)
        return true
    }

    // ---------- Teclas del control remoto ----------

    private fun isCenter(code: Int): Boolean =
        code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
            code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

    fun onKeyDown(code: Int, repeat: Int): Boolean {
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN ||
            code == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            return handleVolume(code)
        }
        if (isCenter(code)) {
            if (repeat == 0) {
                longFired = false
                pressed.add(code)
            } else if (!longFired) {
                longFired = true
                onLongPress()
            }
            return true
        }
        val now = SystemClock.uptimeMillis()
        if (repeat > 0 && now - lastNav < 160) return true
        val handled = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> { nav(-1, 0); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { nav(1, 0); true }
            KeyEvent.KEYCODE_DPAD_UP -> { nav(0, -1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { nav(0, 1); true }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> { if (repeat == 0) back(); true }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                if (repeat == 0) {
                    if (panelOpen) panelOpen = false else openPanel()
                }
                true
            }
            else -> false
        }
        if (handled) {
            lastNav = now
            pressed.add(code)
        }
        return handled
    }

    fun onKeyUp(code: Int): Boolean {
        if (!pressed.remove(code)) return false
        if (isCenter(code)) {
            if (!longFired) onShortPress()
            longFired = false
        }
        return true
    }

    private fun nav(dx: Int, dy: Int) {
        when {
            panelOpen -> {
                if (dy != 0) {
                    val n = rows(panelPage).size
                    panelFocus = (panelFocus + dy).coerceIn(0, maxOf(0, n - 1))
                } else if (dx < 0 && panelPage != 0) {
                    panelPage = 0
                    panelFocus = 0
                }
            }
            screen == Screen.Apps -> {
                val n = apps.size
                if (n > 0) {
                    val g = gridFocus + dx + dy * 6
                    if (g in 0 until n) {
                        gridFocus = g
                    } else if (dy > 0 && gridFocus / 6 < (n - 1) / 6) {
                        gridFocus = n - 1
                    }
                }
            }
            else -> {
                if (dx != 0) {
                    if (editing) moveFocused(dx) else focus = (focus + dx).coerceIn(0, cardCount - 1)
                }
            }
        }
    }

    private fun back() {
        when {
            panelOpen -> if (panelPage != 0) {
                panelPage = 0
                panelFocus = 0
            } else {
                panelOpen = false
            }
            screen == Screen.Apps -> screen = Screen.Home
            editing -> editing = false
            else -> {}
        }
    }

    private fun onShortPress() {
        when {
            panelOpen -> rows(panelPage).getOrNull(panelFocus)?.action?.invoke()
            screen == Screen.Apps -> apps.getOrNull(gridFocus)?.let { launch(it.pkg) }
            editing -> if (focus < homeApps.size) removeFocused()
            else -> activateCard()
        }
    }

    private fun onLongPress() {
        when {
            panelOpen -> {}
            screen == Screen.Apps -> apps.getOrNull(gridFocus)?.let { togglePin(it.pkg) }
            else -> if (focus < homeApps.size) editing = !editing
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val KNOWN = listOf(
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.spotify.tv.android",
            "com.google.android.youtube.tvmusic",
        )
    }
}
