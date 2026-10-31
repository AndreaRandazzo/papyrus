package io.github.andrearandazzo.papyrus.data

import android.content.Context
import androidx.core.content.edit

/** Small choices and facts that should survive closing the app. */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    var gridView: Boolean
        get() = prefs.getBoolean(KEY_GRID_VIEW, false)
        set(value) = prefs.edit { putBoolean(KEY_GRID_VIEW, value) }

    /** When the last backup was saved (milliseconds since epoch), or null if never. */
    var lastBackupAt: Long?
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY_LAST_BACKUP) else putLong(KEY_LAST_BACKUP, value) }

    private companion object {
        const val KEY_GRID_VIEW = "grid_view"
        const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
