package io.github.andrearandazzo.papyrus.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Document::class], version = 1)
abstract class PapyrusDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
