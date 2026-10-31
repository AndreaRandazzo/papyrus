package io.github.andrearandazzo.papyrus.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.PapyrusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreResult(val restored: Int, val alreadyPresent: Int)

/** Saves the whole library to one ZIP file, and merges such a file back in. */
class BackupManager(
    private val context: Context,
    private val database: PapyrusDatabase,
    private val documents: DocumentRepository,
) {
    private val documentDao = database.documentDao()
    private val categoryDao = database.categoryDao()

    /** Writes every document and category to [target]. Returns how many documents were saved. */
    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val categories = categoryDao.getAll()
        val allDocuments = documentDao.getAll()
        val output = context.contentResolver.openOutputStream(target, "wt")
            ?: throw IOException("Cannot write $target")
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupFormat.MANIFEST))
            zip.write(BackupFormat.toJson(categories, allDocuments, System.currentTimeMillis()).toByteArray())
            zip.closeEntry()
            for (document in allDocuments) {
                zip.putNextEntry(ZipEntry(BackupFormat.FILES_DIR + document.fileName))
                documents.fileOf(document).inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        allDocuments.size
    }

    /**
     * Adds the backup's documents to the library. Documents already present (same title, size and
     * import time) are skipped, so restoring twice is harmless. All or nothing: on any error the
     * library is left exactly as it was. Throws [InvalidBackupException] for files that aren't backups.
     */
    suspend fun restore(source: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val manifest = readManifest(source)

        val existingKeys = documentDao.getAll().map { it.duplicateKey() }.toSet()
        val toRestore = manifest.documents.filter { it.duplicateKey() !in existingKeys }
        val wanted = toRestore.associateBy { it.file }

        // 1. Copy the files into storage under new names.
        val copied = mutableMapOf<String, File>()
        try {
            openZip(source) { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val name = entry.name.removePrefix(BackupFormat.FILES_DIR)
                    val document = wanted[name]
                    if (entry.name.startsWith(BackupFormat.FILES_DIR) && document != null && name !in copied) {
                        val target = documents.newStorageFile(name.substringAfterLast('.', ""))
                        copied[name] = target
                        val size = target.outputStream().use { zip.copyTo(it) }
                        if (size != document.sizeBytes) throw InvalidBackupException("Size mismatch for $name")
                    }
                }
            }
            val missing = wanted.keys - copied.keys
            if (missing.isNotEmpty()) throw InvalidBackupException("Missing ${missing.size} files")

            // 2. Record everything in one transaction.
            database.withTransaction {
                val categoryIds = restoreCategories(manifest.categories)
                for (document in toRestore) {
                    documentDao.insert(
                        Document(
                            title = document.title,
                            fileName = copied.getValue(document.file).name,
                            mimeType = document.mimeType,
                            sizeBytes = document.sizeBytes,
                            importedAt = document.importedAt,
                            categoryId = document.categoryId?.let(categoryIds::get),
                            note = document.note,
                            expiresAt = document.expiresAt,
                        ),
                    )
                }
            }
        } catch (e: Throwable) {
            copied.values.forEach { it.delete() }
            throw e
        }
        RestoreResult(restored = toRestore.size, alreadyPresent = manifest.documents.size - toRestore.size)
    }

    /** Maps each backup category id to a category here, reusing ones with the same name. */
    private suspend fun restoreCategories(backup: List<BackupCategory>): Map<Long, Long> {
        val byName = categoryDao.getAll().associate { it.name.lowercase() to it.id }.toMutableMap()
        return backup.filter { it.name.isNotEmpty() }.associate { category ->
            val id = byName.getOrPut(category.name.lowercase()) { categoryDao.insert(Category(name = category.name)) }
            category.id to id
        }
    }

    private fun readManifest(source: Uri): BackupManifest {
        var json: String? = null
        openZip(source) { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == BackupFormat.MANIFEST }
                ?.let { json = zip.readBytes().decodeToString() }
        }
        return BackupFormat.parse(json ?: throw InvalidBackupException("No manifest"))
    }

    private fun openZip(source: Uri, block: (ZipInputStream) -> Unit) {
        val input: InputStream = context.contentResolver.openInputStream(source)
            ?: throw IOException("Cannot read $source")
        ZipInputStream(input.buffered()).use(block)
    }

    private fun Document.duplicateKey() = Triple(title, sizeBytes, importedAt)
    private fun BackupDocument.duplicateKey() = Triple(title, sizeBytes, importedAt)
}
