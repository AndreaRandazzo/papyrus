package io.github.andrearandazzo.papyrus

import android.app.Application
import android.content.res.Resources
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.PapyrusDatabase
import io.github.andrearandazzo.papyrus.data.StarterCategories
import io.github.andrearandazzo.papyrus.data.ThumbnailLoader
import io.github.andrearandazzo.papyrus.data.UserPreferences
import io.github.andrearandazzo.papyrus.data.backup.BackupManager
import io.github.andrearandazzo.papyrus.lock.AppLock
import kotlinx.coroutines.launch

/** Creates the app-wide singletons once, when first needed. */
class PapyrusApp : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, PapyrusDatabase::class.java, "papyrus.db")
            .addCallback(seedStarterCategories)
            .build()
    }

    val documentRepository by lazy { DocumentRepository(this, database.documentDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val thumbnails by lazy { ThumbnailLoader(documentRepository) }
    val backupManager by lazy { BackupManager(this, database, documentRepository) }
    val preferences by lazy { UserPreferences(this) }
    lateinit var appLock: AppLock
        private set

    override fun onCreate() {
        super.onCreate()
        appLock = AppLock(preferences)
        registerActivityLifecycleCallbacks(appLock)
    }

    /** On a fresh install, start with a few useful categories in the app's language. */
    private val seedStarterCategories = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val names = starterNames(resources)
            StarterCategories.KEYS.forEach { key ->
                db.execSQL("INSERT INTO categories (name, starterKey) VALUES (?, ?)", arrayOf(names.getValue(key), key))
            }
        }
    }
}

/** Starter category names in the language of [resources]. */
fun starterNames(resources: Resources): Map<String, String> = mapOf(
    "identity" to resources.getString(R.string.starter_identity),
    "health" to resources.getString(R.string.starter_health),
    "home" to resources.getString(R.string.starter_home),
    "car" to resources.getString(R.string.starter_car),
    "work" to resources.getString(R.string.starter_work),
    "finance" to resources.getString(R.string.starter_finance),
)

/**
 * Renames untouched starter categories to this screen's language. Called whenever a screen opens,
 * which includes right after the language changes (Android recreates the screen).
 */
fun ComponentActivity.syncStarterCategories() {
    val names = starterNames(resources)
    lifecycleScope.launch { (application as PapyrusApp).categoryRepository.syncStarterNames(names) }
}
