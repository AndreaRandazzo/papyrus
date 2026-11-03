package io.github.andrearandazzo.papyrus.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Bump [version] whenever an entity changes, and add a migration so users keep their data.
 * Schemas of every version are kept in `app/schemas/`.
 */
@Database(
    entities = [Document::class, Category::class, DocumentText::class],
    version = 5,
    autoMigrations = [
        // v2: categories table; category, note and expiry on documents.
        AutoMigration(from = 1, to = 2),
        // v3: optional icon on categories.
        AutoMigration(from = 2, to = 3),
        // v4: starter key on categories, so untouched starters follow the app's language.
        AutoMigration(from = 3, to = 4, spec = PapyrusDatabase.MarkStarterCategories::class),
        // v5: the text inside documents, for searching it. Existing documents get read afterwards.
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class PapyrusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun documentTextDao(): DocumentTextDao

    /**
     * Older versions stored starter categories as plain names. Marks the ones still named exactly
     * like a starter, in any shipped language, so they follow the language from now on. Changes
     * nothing else; running it again is harmless.
     */
    class MarkStarterCategories : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            for ((key, name) in StarterCategories.knownNames) {
                db.execSQL(
                    // At most one row per key: the unique-name index already prevents duplicates of
                    // one name, and the subquery skips a key another translation has already taken.
                    "UPDATE categories SET starterKey = ? WHERE starterKey IS NULL AND name = ? COLLATE NOCASE " +
                        "AND NOT EXISTS (SELECT 1 FROM categories WHERE starterKey = ?)",
                    arrayOf(key, name, key),
                )
            }
        }
    }
}
