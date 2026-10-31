package io.github.andrearandazzo.papyrus

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.PapyrusDatabase

/** Creates the app-wide singletons once, when first needed. */
class PapyrusApp : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, PapyrusDatabase::class.java, "papyrus.db")
            .addCallback(seedDefaultCategories)
            .build()
    }

    val documentRepository by lazy { DocumentRepository(this, database.documentDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }

    /** On a fresh install, start with a few useful categories in the phone's language. */
    private val seedDefaultCategories = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            resources.getStringArray(R.array.default_categories).forEach { name ->
                db.execSQL("INSERT INTO categories (name) VALUES (?)", arrayOf(name))
            }
        }
    }
}
