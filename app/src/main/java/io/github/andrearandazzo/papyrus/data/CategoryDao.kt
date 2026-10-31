package io.github.andrearandazzo.papyrus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    fun observeAll(): Flow<List<Category>>

    /** Throws SQLiteConstraintException if the name already exists. */
    @Insert
    suspend fun insert(category: Category): Long

    /** Throws SQLiteConstraintException if the new name already exists. */
    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
