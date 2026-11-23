package pe.launcher.jvc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var controller: LauncherController

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = LauncherController(this)
        setContent { LauncherRoot(controller) }
        reloadApps()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            // ya estaba desregistrado
        }
    }

    override fun onResume() {
        super.onResume()
        controller.syncVolume(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Se pulsó el botón Home: volver al inicio.
        controller.goHomeFromSystem()
    }

    private fun reloadApps() {
        Thread {
            val list = AppRepository.load(this)
            runOnUiThread { controller.setApps(list) }
        }.start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = when (event.action) {
            KeyEvent.ACTION_DOWN -> controller.onKeyDown(event.keyCode, event.repeatCount)
            KeyEvent.ACTION_UP -> controller.onKeyUp(event.keyCode)
            else -> false
        }
        return handled || super.dispatchKeyEvent(event)
    }
}
