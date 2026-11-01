package io.github.andrearandazzo.papyrus.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import io.github.andrearandazzo.papyrus.R

/** Languages Papyrus is translated into. [tag] null means "follow the phone". */
enum class AppLanguage(val tag: String?, @StringRes val label: Int) {
    SYSTEM(null, R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ITALIAN("it", R.string.language_italian),
}

/**
 * Per-app language, stored by Android itself (the same setting as Settings > Apps > Papyrus >
 * Language). Available on Android 13+; older phones follow the phone's language.
 */
object AppLanguageSetting {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun current(context: Context): AppLanguage {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return AppLanguage.entries.firstOrNull { it.tag == locales[0].language } ?: AppLanguage.SYSTEM
    }

    /** Android recreates the screen in the new language right away. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun set(context: Context, language: AppLanguage) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            language.tag?.let(LocaleList::forLanguageTags) ?: LocaleList.getEmptyLocaleList()
    }
}
