package io.github.andrearandazzo.papyrus.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-defined group of documents, e.g. "Car" or "Health". Names are unique, ignoring case. */
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    /**
     * Key of the icon the user picked (see CategoryIcon), or null to choose one from the name.
     * A stable text key, not a resource id, so it survives app updates and backups.
     */
    val icon: String? = null,
    /**
     * Set on a starter category the user hasn't renamed (see StarterCategories): its [name] then
     * follows the app's language. Cleared when the user renames it.
     */
    val starterKey: String? = null,
)
