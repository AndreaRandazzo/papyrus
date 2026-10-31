package io.github.andrearandazzo.papyrus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A document the user imported. The file itself lives in the app's private storage. */
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Name of the stored copy inside the app's `documents` folder. */
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Import time, in milliseconds since epoch. */
    val importedAt: Long,
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
