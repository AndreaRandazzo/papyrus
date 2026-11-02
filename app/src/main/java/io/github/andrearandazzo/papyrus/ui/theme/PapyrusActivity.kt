package io.github.andrearandazzo.papyrus.ui.theme

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.TextSize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Base of every Papyrus activity: applies Settings > Text size on top of the phone's font size, and
 * tells the app lock when the user leaves for another app on purpose.
 *
 * It's applied to the activity's configuration rather than inside Compose, so dialogs, menus and
 * bottom sheets (each drawn in a window of its own) get it too. Changing it recreates the activity,
 * which keeps screen state like any configuration change.
 */
abstract class PapyrusActivity : ComponentActivity() {
    private lateinit var appliedTextSize: TextSize

    override fun attachBaseContext(newBase: Context) {
        val preferences = (newBase.applicationContext as PapyrusApp).preferences
        appliedTextSize = preferences.textSize.value
        val configuration = Configuration(newBase.resources.configuration).apply {
            fontScale *= appliedTextSize.scale
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textSize = (application as PapyrusApp).preferences.textSize
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                textSize.first { it != appliedTextSize }
                recreate()
            }
        }
        // With the app lock on, the recent-apps screen shows a blank card instead of the documents.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLock = (application as PapyrusApp).preferences.appLock
            lifecycleScope.launch { appLock.collect { setRecentsScreenshotEnabled(!it) } }
        }
    }

    /**
     * Every other app Papyrus opens goes through here: viewers, the camera, file pickers, the
     * browser. Coming back from one shouldn't ask to unlock again straight away.
     */
    @Deprecated("Still the path every launch takes; overridden only to observe it.")
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        (application as PapyrusApp).appLock.onOpeningOtherApp()
        super.startActivityForResult(intent, requestCode, options)
    }
}
