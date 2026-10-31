package io.github.andrearandazzo.papyrus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    /** Emits the full list again every time the table changes. */
    @Query("SELECT * FROM documents ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observe(id: Long): Flow<Document?>

    @Insert
    suspend fun insert(document: Document): Long

    @Update
    suspend fun update(document: Document)
}
