package io.github.andrearandazzo.papyrus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * An extra file of a document: a contract's annexes, the receipt of a guarantee. The document's own
 * file stays in [Document]; attachments are deleted with it.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(entity = Document::class, parentColumns = ["id"], childColumns = ["documentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("documentId")],
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    /** The name shown, e.g. "Allegato B.pdf". */
    val name: String,
    /** Name of the stored copy inside the app's `documents` folder. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAt: Long,
) {
    val kind: DocumentKind get() = DocumentKind.of(mimeType)
}

/** How many attachments a document has (for the Library). */
data class AttachmentCount(val documentId: Long, val count: Int)

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE documentId = :documentId ORDER BY addedAt, id")
    fun observeFor(documentId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE documentId = :documentId ORDER BY addedAt, id")
    suspend fun getFor(documentId: Long): List<Attachment>

    @Query("SELECT * FROM attachments")
    suspend fun getAll(): List<Attachment>

    @Query("SELECT documentId, COUNT(*) AS count FROM attachments GROUP BY documentId")
    fun observeCounts(): Flow<List<AttachmentCount>>

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Delete
    suspend fun delete(attachment: Attachment)
}
