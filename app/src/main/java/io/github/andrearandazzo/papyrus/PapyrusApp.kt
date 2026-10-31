package io.github.andrearandazzo.papyrus

import android.app.Application
import androidx.room.Room
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.PapyrusDatabase

/** Creates the app-wide singletons once, when first needed. */
class PapyrusApp : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, PapyrusDatabase::class.java, "papyrus.db").build()
    }

    val documentRepository by lazy { DocumentRepository(this, database.documentDao()) }
}
