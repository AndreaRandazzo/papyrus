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

/** The backup is password-protected and no password was given. */
class PasswordRequiredException : Exception("Password required")

/**
 * Saves the whole library to one ZIP file, and merges such a file back in. With a password the ZIP
 * is encrypted (see [BackupCrypto]); without one it stays a plain ZIP, readable on a computer.
 */
class BackupManager(
    private val context: Context,
    private val database: PapyrusDatabase,
    private val documents: DocumentRepository,
) {
    private val documentDao = database.documentDao()
    private val categoryDao = database.categoryDao()

    /**
     * Writes every document and category to [target], encrypted with [password] if given (it's
     * wiped afterwards). Returns how many documents were saved.
     */
    suspend fun export(target: Uri, password: CharArray? = null): Int = withContext(Dispatchers.IO) {
        val categories = categoryDao.getAll()
        val allDocuments = documentDao.getAll()
        val output = context.contentResolver.openOutputStream(target, "wt")
            ?: throw IOException("Cannot write $target")
        val file = output.buffered()
        val sink = try {
            if (password == null) file else BackupCrypto.encrypting(file, BackupCrypto.newKey(password))
        } catch (e: Throwable) {
            file.close()
            throw e
        } finally {
            password?.fill(' ')
        }
        ZipOutputStream(sink).use { zip ->
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
    suspend fun restore(source: Uri, password: CharArray? = null): RestoreResult = withContext(Dispatchers.IO) {
        // Derived once: the file is read twice (manifest, then files), and deriving takes a moment.
        val key = try {
            if (!isEncrypted(source)) {
                null
            } else {
                val header = open(source).use(BackupCrypto::readHeader)
                BackupCrypto.keyFor(password ?: throw PasswordRequiredException(), header)
            }
        } finally {
            password?.fill(' ')
        }
        val manifest = readManifest(source, key)

        val existingKeys = documentDao.getAll().map { it.duplicateKey() }.toSet()
        val toRestore = manifest.documents.filter { it.duplicateKey() !in existingKeys }
        val wanted = toRestore.associateBy { it.file }

        // 1. Copy the files into storage under new names.
        val copied = mutableMapOf<String, File>()
        try {
            openZip(source, key) { zip ->
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

    /**
     * Maps each backup category id to a category here: the same starter (so a backup's "Health"
     * lands in an Italian phone's "Salute"), else one with the same name, else a new one.
     */
    private suspend fun restoreCategories(backup: List<BackupCategory>): Map<Long, Long> {
        val existing = categoryDao.getAll()
        val byStarter = existing.filter { it.starterKey != null }.associate { it.starterKey!! to it.id }.toMutableMap()
        val byName = existing.associate { it.name.lowercase() to it.id }.toMutableMap()
        return backup.filter { it.name.isNotEmpty() }.associate { category ->
            val key = category.starterKey
            val id = key?.let(byStarter::get) ?: byName.getOrPut(category.name.lowercase()) {
                // A new starter keeps its key; the next screen to open renames it to the app's language.
                val starterKey = key?.takeIf { it !in byStarter }
                categoryDao.insert(Category(name = category.name, icon = category.icon, starterKey = starterKey))
                    .also { id -> starterKey?.let { byStarter[it] = id } }
            }
            category.id to id
        }
    }

    /** True if [source] is a password-protected backup. */
    suspend fun isEncrypted(source: Uri): Boolean = withContext(Dispatchers.IO) {
        val start = ByteArray(BackupCrypto.HEADER_SIZE)
        val read = open(source).use { it.readNBytesCompat(start) }
        BackupCrypto.isEncrypted(start.copyOf(read))
    }

    private fun readManifest(source: Uri, key: BackupCrypto.Key?): BackupManifest {
        var json: String? = null
        openZip(source, key) { zip ->
            generateSequence { zip.nextEntry }.firstOrNull { it.name == BackupFormat.MANIFEST }
                ?.let { json = zip.readBytes().decodeToString() }
        }
        return BackupFormat.parse(json ?: throw InvalidBackupException("No manifest"))
    }

    private fun open(source: Uri): InputStream =
        context.contentResolver.openInputStream(source)?.buffered() ?: throw IOException("Cannot read $source")

    private fun openZip(source: Uri, key: BackupCrypto.Key?, block: (ZipInputStream) -> Unit) {
        val input = open(source)
        val plain = if (key == null) {
            input
        } else {
            // The header was already checked when the key was made; skip it.
            BackupCrypto.readHeader(input)
            BackupCrypto.decrypting(input, key).buffered()
        }
        ZipInputStream(plain).use(block)
    }

    /** Reads up to [buffer].size bytes; fewer only at the end of the file. */
    private fun InputStream.readNBytesCompat(buffer: ByteArray): Int {
        var n = 0
        while (n < buffer.size) {
            val read = read(buffer, n, buffer.size - n)
            if (read < 0) break
            n += read
        }
        return n
    }

    private fun Document.duplicateKey() = Triple(title, sizeBytes, importedAt)
    private fun BackupDocument.duplicateKey() = Triple(title, sizeBytes, importedAt)
}
