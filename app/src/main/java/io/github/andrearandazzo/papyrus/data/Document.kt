package io.github.andrearandazzo.papyrus.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A document the user imported. The file itself lives in the app's private storage. */
@Entity(
    tableName = "documents",
    foreignKeys = [
        // Deleting a category keeps its documents; they just become uncategorized.
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Name of the stored copy inside the app's `documents` folder. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Import time, in milliseconds since epoch. */
    val importedAt: Long,
    val categoryId: Long? = null,
    @ColumnInfo(defaultValue = "") val note: String = "",
    /** Expiry day as UTC midnight in milliseconds (what the date picker returns), or null. */
    val expiresAt: Long? = null,
) {
    val kind: DocumentKind get() = DocumentKind.of(mimeType)
}

enum class DocumentKind {
    PDF, WORD, IMAGE, OTHER;

    companion object {
        fun of(mimeType: String): DocumentKind = when {
            mimeType == "application/pdf" -> PDF
            mimeType == "application/msword" ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> WORD
            mimeType.startsWith("image/") -> IMAGE
            else -> OTHER
        }
    }
}
