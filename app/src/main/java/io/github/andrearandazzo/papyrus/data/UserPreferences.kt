package io.github.andrearandazzo.papyrus.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The app's colors, chosen in Settings. */
enum class AppPalette { TEAL, SAGE, SUN }

/** Text size on top of the phone's own font size, chosen in Settings. */
enum class TextSize(val scale: Float) { SMALL(0.9f), NORMAL(1f), LARGE(1.15f), EXTRA_LARGE(1.3f) }

/** How the Library shows documents. */
enum class LibraryView { LIST, GRID, FOLDERS }

/** Small choices and facts that should survive closing the app. */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    var libraryView: LibraryView
        get() = LibraryView.entries.firstOrNull { it.name == prefs.getString(KEY_LIBRARY_VIEW, null) }
            // Before Folders, versions up to 1.1 stored only "grid or not".
            ?: if (prefs.getBoolean(KEY_GRID_VIEW, false)) LibraryView.GRID else LibraryView.LIST
        set(value) = prefs.edit { putString(KEY_LIBRARY_VIEW, value.name) }

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

    // Look settings are observed by every activity, so changing one restyles the app immediately.
    private val _themeMode = MutableStateFlow(stored(KEY_THEME, ThemeMode.SYSTEM))
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = store(KEY_THEME, mode, _themeMode)

    private val _palette = MutableStateFlow(stored(KEY_PALETTE, AppPalette.TEAL))
    val palette: StateFlow<AppPalette> = _palette.asStateFlow()

    fun setPalette(palette: AppPalette) = store(KEY_PALETTE, palette, _palette)

    private val _textSize = MutableStateFlow(stored(KEY_TEXT_SIZE, TextSize.NORMAL))
    val textSize: StateFlow<TextSize> = _textSize.asStateFlow()

    fun setTextSize(size: TextSize) = store(KEY_TEXT_SIZE, size, _textSize)

    /** An unknown stored value (e.g. from a newer version) falls back to [default]. */
    private inline fun <reified T : Enum<T>> stored(key: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == prefs.getString(key, null) } ?: default

    private fun <T : Enum<T>> store(key: String, value: T, flow: MutableStateFlow<T>) {
        prefs.edit { putString(key, value.name) }
        flow.value = value
    }

    private companion object {
        const val KEY_GRID_VIEW = "grid_view"
        const val KEY_LIBRARY_VIEW = "library_view"
        const val KEY_LAST_BACKUP = "last_backup_at"
        const val KEY_THEME = "theme_mode"
        const val KEY_PALETTE = "palette"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_SORT_FIELD = "sort_field"
        const val KEY_SORT_ASCENDING = "sort_ascending"
    }
}
