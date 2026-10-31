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

    /** Readable-named copies handed to other apps; see [exportCopy]. Must match res/xml/file_paths.xml. */
    private val sharedDir = File(context.cacheDir, "shared")

    val documents: Flow<List<Document>> = dao.observeAll()

    fun observe(id: Long): Flow<Document?> = dao.observe(id)

    suspend fun update(document: Document) = dao.update(document)

    /** The stored file. Treat it as read-only. */
    fun fileOf(document: Document): File = File(storageDir, document.fileName)

    /** A new, uniquely named file in storage. Unique names mean two "scan.pdf" never clash. */
    fun newStorageFile(extension: String): File =
        File(storageDir, UUID.randomUUID().toString() + if (extension.isEmpty()) "" else ".$extension")

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

        val target = newStorageFile(extension)
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

    /**
     * Copies the document to a cache file named after its title (e.g. "Car insurance.pdf"),
     * so apps it is opened or shared with show a readable name instead of the internal one.
     */
    suspend fun exportCopy(document: Document): File = withContext(Dispatchers.IO) {
        val dir = File(sharedDir, document.id.toString())
        dir.deleteRecursively()
        dir.mkdirs()
        val extension = document.fileName.substringAfterLast('.', missingDelimiterValue = "")
        val name = safeFileName(document.title) + if (extension.isEmpty()) "" else ".$extension"
        fileOf(document).copyTo(File(dir, name))
    }

    /** Removes the record first, then the files, so a failure never leaves a record without its file. */
    suspend fun delete(document: Document) = withContext(Dispatchers.IO) {
        dao.delete(document)
        fileOf(document).delete()
        File(sharedDir, document.id.toString()).deleteRecursively()
    }

    private fun safeFileName(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().take(100).ifEmpty { "document" }
}
