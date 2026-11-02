package io.github.andrearandazzo.papyrus.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small choices and facts that should survive closing the app. */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    var gridView: Boolean
        get() = prefs.getBoolean(KEY_GRID_VIEW, false)
        set(value) = prefs.edit { putBoolean(KEY_GRID_VIEW, value) }

    /** The Library's sort order. An unknown stored field (e.g. from a newer version) means the default. */
    var sortOrder: SortOrder
        get() {
            val field = SortField.entries.firstOrNull { it.name == prefs.getString(KEY_SORT_FIELD, null) }
                ?: return SortOrder.DEFAULT
            return SortOrder(field, prefs.getBoolean(KEY_SORT_ASCENDING, field.ascendingFirst))
        }
        set(value) = prefs.edit {
            putString(KEY_SORT_FIELD, value.field.name)
            putBoolean(KEY_SORT_ASCENDING, value.ascending)
        }

    /** When the last backup was saved (milliseconds since epoch), or null if never. */
    var lastBackupAt: Long?
        get() = prefs.getLong(KEY_LAST_BACKUP, 0L).takeIf { it > 0 }
        set(value) = prefs.edit { if (value == null) remove(KEY_LAST_BACKUP) else putLong(KEY_LAST_BACKUP, value) }

    private val _themeMode = MutableStateFlow(
        // An unknown stored value (e.g. from a newer version) falls back to following the system.
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM,
    )

    /** Observed by the activity, so changing it re-themes the app immediately. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _themeMode.value = mode
    }

    private companion object {
        const val KEY_GRID_VIEW = "grid_view"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_THEME = "theme_mode"
        const val KEY_SORT_FIELD = "sort_field"
        const val KEY_SORT_ASCENDING = "sort_ascending"
    }
}
