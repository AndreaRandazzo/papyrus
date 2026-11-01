package io.github.andrearandazzo.papyrus.data.backup

import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.StarterCategories
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The backup ZIP layout:
 * - `manifest.json`: categories and document details (see [toJson])
 * - `files/<fileName>`: each document's file, under its internal name
 *
 * Bump [VERSION] on incompatible changes; older apps then refuse newer backups instead of misreading them.
 */
object BackupFormat {
    const val VERSION = 1
    const val MANIFEST = "manifest.json"
    const val FILES_DIR = "files/"

    fun toJson(categories: List<Category>, documents: List<Document>, createdAt: Long): String =
        JSONObject()
            .put("app", "Papyrus")
            .put("format", VERSION)
            .put("createdAt", createdAt)
            .put(
                "categories",
                JSONArray(
                    categories.map {
                        JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon ?: JSONObject.NULL)
                            .put("starterKey", it.starterKey ?: JSONObject.NULL)
                    },
                ),
            )
            .put(
                "documents",
                JSONArray(
                    documents.map { document ->
                        JSONObject()
                            .put("title", document.title)
                            .put("file", document.fileName)
                            .put("mimeType", document.mimeType)
                            .put("sizeBytes", document.sizeBytes)
                            .put("importedAt", document.importedAt)
                            .put("categoryId", document.categoryId ?: JSONObject.NULL)
                            .put("note", document.note)
                            .put("expiresAt", document.expiresAt ?: JSONObject.NULL)
                    },
                ),
            )
            .toString(2)

    /** Throws [InvalidBackupException] if this isn't a backup this version can read. */
    fun parse(json: String): BackupManifest = try {
        val root = JSONObject(json)
        val format = root.getInt("format")
        if (format > VERSION) throw InvalidBackupException("Backup format $format is newer than $VERSION")

        val categories = root.getJSONArray("categories").objects().map {
            BackupCategory(
                id = it.getLong("id"),
                name = it.getString("name").trim(),
                // Missing in backups made before icons existed.
                icon = if (it.isNull("icon")) null else it.getString("icon"),
                starterKey = when {
                    // Older backups have no such field: recognize starters by their name instead.
                    !it.has("starterKey") -> StarterCategories.keyForName(it.getString("name"))
                    it.isNull("starterKey") -> null
                    else -> it.getString("starterKey")
                },
            )
        }
        val categoryIds = categories.map { it.id }.toSet()
        val documents = root.getJSONArray("documents").objects().map {
            val file = it.getString("file")
            // Only plain names we wrote ourselves; never paths.
            if (!SAFE_FILE_NAME.matches(file)) throw InvalidBackupException("Bad file name")
            BackupDocument(
                title = it.getString("title"),
                file = file,
                mimeType = it.getString("mimeType"),
                sizeBytes = it.getLong("sizeBytes"),
                importedAt = it.getLong("importedAt"),
                categoryId = it.optLongOrNull("categoryId")?.takeIf { id -> id in categoryIds },
                note = it.optString("note", ""),
                expiresAt = it.optLongOrNull("expiresAt"),
            )
        }
        if (documents.map { it.file }.toSet().size != documents.size) {
            throw InvalidBackupException("Duplicate file names")
        }
        BackupManifest(categories, documents)
    } catch (e: JSONException) {
        throw InvalidBackupException("Unreadable manifest", e)
    }

    private val SAFE_FILE_NAME = Regex("[A-Za-z0-9._-]{1,200}")

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
}

data class BackupManifest(val categories: List<BackupCategory>, val documents: List<BackupDocument>)

data class BackupCategory(val id: Long, val name: String, val icon: String? = null, val starterKey: String? = null)

data class BackupDocument(
    val title: String,
    /** Name of the file inside the backup's `files/` folder. */
    val file: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    /** Refers to [BackupCategory.id], not to a category on this phone. */
    val categoryId: Long?,
    val note: String,
    val expiresAt: Long?,
)

class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
