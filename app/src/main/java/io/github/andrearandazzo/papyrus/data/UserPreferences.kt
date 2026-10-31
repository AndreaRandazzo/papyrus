package io.github.andrearandazzo.papyrus.data

import android.content.Context
import androidx.core.content.edit

/** Small display choices that should survive closing the app. */
class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    var gridView: Boolean
        get() = prefs.getBoolean(KEY_GRID_VIEW, false)
        set(value) = prefs.edit { putBoolean(KEY_GRID_VIEW, value) }

    private companion object {
        const val KEY_GRID_VIEW = "grid_view"
    }
}
