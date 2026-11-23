package pe.launcher.jvc

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var theme: String
        get() = sp.getString("theme", "dark") ?: "dark"
        set(v) = sp.edit().putString("theme", v).apply()

    var palette: String
        get() = sp.getString("palette", "aurora") ?: "aurora"
        set(v) = sp.edit().putString("palette", v).apply()

    var motion: Boolean
        get() = sp.getBoolean("motion", true)
        set(v) = sp.edit().putBoolean("motion", v).apply()

    var customVolume: Boolean
        get() = sp.getBoolean("customVolume", true)
        set(v) = sp.edit().putBoolean("customVolume", v).apply()

    /** Orden de las apps del inicio. null = primera vez. */
    var home: List<String>?
        get() = if (sp.contains("home")) {
            (sp.getString("home", "") ?: "").split("|").filter { it.isNotEmpty() }
        } else {
            null
        }
        set(v) {
            if (v == null) sp.edit().remove("home").apply()
            else sp.edit().putString("home", v.joinToString("|")).apply()
        }

    /** Apps que ya se conocían, para detectar las recién instaladas. */
    var seen: Set<String>
        get() = HashSet(sp.getStringSet("seen", emptySet()) ?: emptySet())
        set(v) = sp.edit().putStringSet("seen", HashSet(v)).apply()
}
