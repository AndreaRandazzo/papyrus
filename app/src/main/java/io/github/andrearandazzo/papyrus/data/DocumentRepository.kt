package io.github.andrearandazzo.papyrus.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Single owner of documents: keeps the stored files and the database rows in sync.
 */
class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
) {
    private val storageDir = File(context.filesDir, "documents").apply { mkdirs() }

    val documents: Flow<List<Document>> = dao.observeAll()

    fun observe(id: Long): Flow<Document?> = dao.observe(id)

    suspend fun update(document: Document) = dao.update(document)

    /**
     * Copies the file behind [uri] into private storage and records it in [categoryId].
     * The copy means the document survives even if the original is deleted.
     */
    suspend fun import(uri: Uri, categoryId: Long?): Document = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: "Document"
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .ifEmpty { MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty() }

        val target = File(storageDir, UUID.randomUUID().toString() + if (extension.isEmpty()) "" else ".$extension")
        try {
            val size = resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Cannot open $uri")

            val document = Document(
                title = displayName.substringBeforeLast('.').ifBlank { displayName },
                fileName = target.name,
                mimeType = mimeType,
                sizeBytes = size,
                importedAt = System.currentTimeMillis(),
                categoryId = categoryId,
            )
            document.copy(id = dao.insert(document))
        } catch (e: Throwable) {
            // Never leave an orphan file without its database row.
            target.delete()
            throw e
        }
    }
}
