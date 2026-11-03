package io.github.andrearandazzo.papyrus.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The text inside a document: read from the PDF itself, or recognized on the phone (OCR) from photos
 * and scans. No row yet means it hasn't been read; an empty [text] means there was nothing to read.
 * Kept apart from [Document] so saving a document can never overwrite it, and deleted with it.
 */
@Entity(
    tableName = "document_text",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DocumentText(
    @PrimaryKey val documentId: Long,
    val text: String,
)

@Dao
interface DocumentTextDao {
    @Query("SELECT * FROM document_text")
    fun observeAll(): Flow<List<DocumentText>>

    @Query("SELECT * FROM document_text WHERE documentId = :id")
    fun observe(id: Long): Flow<DocumentText?>

    /** Documents whose text hasn't been read yet, oldest first. */
    @Query("SELECT * FROM documents WHERE id NOT IN (SELECT documentId FROM document_text) ORDER BY importedAt")
    fun observeUnread(): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(text: DocumentText)
}
