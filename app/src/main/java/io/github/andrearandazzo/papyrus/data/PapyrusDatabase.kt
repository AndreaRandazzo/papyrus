package io.github.andrearandazzo.papyrus.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Bump [version] whenever an entity changes, and add a migration so users keep their data.
 * Schemas of every version are kept in `app/schemas/`.
 */
@Database(
    entities = [Document::class, Category::class],
    version = 2,
    autoMigrations = [
        // v2: categories table; category, note and expiry on documents.
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class PapyrusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun categoryDao(): CategoryDao
}
