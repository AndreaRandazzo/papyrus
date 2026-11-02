package io.github.andrearandazzo.papyrus.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.andrearandazzo.papyrus.MainActivity
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Settings > Expiry reminders. Once a day (around 9:00, when Android finds it convenient) Papyrus
 * checks the expiry dates and notifies what [ReminderPlanner] says is due. Nothing leaves the phone.
 */
object ExpiryReminders {
    private const val CHANNEL = "expiry"
    const val ACTION_CHECK = "io.github.andrearandazzo.papyrus.CHECK_EXPIRY"

    /** Turns reminders on: what's already due counts as sent, then the daily check is scheduled. */
    suspend fun enable(context: Context) {
        val app = context.applicationContext as PapyrusApp
        app.preferences.sentReminders = ReminderPlanner.baseline(app.documentRepository.all(), LocalDate.now())
        app.preferences.setExpiryReminders(true)
        schedule(context)
    }

    fun disable(context: Context) {
        val app = context.applicationContext as PapyrusApp
        app.preferences.setExpiryReminders(false)
        context.getSystemService(AlarmManager::class.java).cancel(checkIntent(context))
    }

    /** The daily check; scheduling again replaces the previous alarm. Alarms don't survive a reboot. */
    fun schedule(context: Context) {
        val now = ZonedDateTime.now()
        var next = now.with(LocalTime.of(9, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        context.getSystemService(AlarmManager::class.java).setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            checkIntent(context),
        )
    }

    /** Runs the check: notifies what's due and remembers it. */
    suspend fun check(context: Context) {
        val app = context.applicationContext as PapyrusApp
        if (!app.preferences.expiryReminders.value) return
        val plan = ReminderPlanner.plan(app.documentRepository.all(), LocalDate.now(), app.preferences.sentReminders)
        if (plan.reminders.isNotEmpty() && canNotify(context)) {
            createChannel(context)
            plan.reminders.forEach { notify(context, it) }
        }
        app.preferences.sentReminders = plan.sent
    }

    /** Android 13+ asks the user before an app can show notifications. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun checkIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_CHECK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.reminders_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Suppress("MissingPermission") // Checked by canNotify().
    private fun notify(context: Context, reminder: Reminder) {
        val document = reminder.document
        val title = when {
            reminder.daysLeft > 0 -> context.resources.getQuantityString(
                R.plurals.reminder_expires_in, reminder.daysLeft.toInt(), document.title, reminder.daysLeft.toInt(),
            )
            reminder.daysLeft == 0L -> context.getString(R.string.reminder_expires_today, document.title)
            else -> context.getString(R.string.reminder_expired, document.title)
        }
        val open = PendingIntent.getActivity(
            context,
            document.id.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_DOCUMENT_ID, document.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // On the lock screen only "A document is expiring" shows, not which one.
        val public = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_public_title))
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        NotificationManagerCompat.from(context).notify(document.id.toInt(), notification)
    }
}
