package io.github.andrearandazzo.papyrus.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.andrearandazzo.papyrus.PapyrusApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The daily alarm runs the expiry check; after a reboot, the alarm is set again. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PapyrusApp
        if (!app.preferences.expiryReminders.value) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ExpiryReminders.schedule(context)
            ExpiryReminders.ACTION_CHECK -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ExpiryReminders.check(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
