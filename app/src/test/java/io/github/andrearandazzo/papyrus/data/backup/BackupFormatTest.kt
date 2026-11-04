package io.github.andrearandazzo.papyrus.data.backup

import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupFormatTest {

    private val car = Category(id = 7, name = "Car", icon = "car")
    private val dog = Category(id = 8, name = "Dog")
    private val insurance = Document(
        id = 1,
        title = "Polizza “auto” città",
        fileName = "8295918c-ac93.pdf",
        mimeType = "application/pdf",
        sizeBytes = 193,
        importedAt = 1_700_000_000_000,
        categoryId = 7,
        note = "Rinnovo\na marzo",
        expiresAt = 1_800_000_000_000,
    )
    private val photo = insurance.copy(
        id = 2, title = "Photo", fileName = "abc.png", mimeType = "image/png",
        categoryId = null, note = "", expiresAt = null,
    )

    @Test
    fun roundTripKeepsEverything() {
        val manifest = BackupFormat.parse(BackupFormat.toJson(listOf(car, dog), listOf(insurance, photo), 0))

        assertEquals(listOf(BackupCategory(7, "Car", "car", null), BackupCategory(8, "Dog", null)), manifest.categories)
        assertEquals(
            BackupDocument(
                title = "Polizza “auto” città",
                file = "8295918c-ac93.pdf",
                mimeType = "application/pdf",
                sizeBytes = 193,
                importedAt = 1_700_000_000_000,
                categoryId = 7,
                note = "Rinnovo\na marzo",
                expiresAt = 1_800_000_000_000,
            ),
            manifest.documents[0],
        )
        assertEquals(null, manifest.documents[1].categoryId)
        assertEquals(null, manifest.documents[1].expiresAt)
    }

    @Test
    fun keepsStarterKeys() {
        val starter = Category(id = 9, name = "Salute", starterKey = "health")
        val manifest = BackupFormat.parse(BackupFormat.toJson(listOf(starter, dog), emptyList(), 0))
        assertEquals(listOf("health", null), manifest.categories.map { it.starterKey })
    }

    @Test
    fun ownCategoryNamedLikeAStarterStaysTheUsers() {
        // "Car" without a starter key in a new backup is the user's own category, not a starter.
        val manifest = BackupFormat.parse(BackupFormat.toJson(listOf(car), emptyList(), 0))
        assertEquals(null, manifest.categories.single().starterKey)
    }

    @Test
    fun recognizesStartersInBackupsMadeBeforeKeys() {
        val old = """{"format":1,"categories":[{"id":1,"name":"Health"},{"id":2,"name":"Dog"}],"documents":[]}"""
        assertEquals(listOf("health", null), BackupFormat.parse(old).categories.map { it.starterKey })
    }

    @Test
    fun readsBackupsMadeBeforeIcons() {
        val old = """{"format":1,"categories":[{"id":1,"name":"Car"}],"documents":[]}"""
        assertEquals(listOf(BackupCategory(1, "Car", null, "car")), BackupFormat.parse(old).categories)
    }

    @Test
    fun rejectsNewerFormat() {
        val json = BackupFormat.toJson(emptyList(), emptyList(), 0).replace("\"format\": 1", "\"format\": 99")
        assertThrows(InvalidBackupException::class.java) { BackupFormat.parse(json) }
    }

    @Test
    fun rejectsFileNamesThatArePaths() {
        val evil = BackupFormat.toJson(emptyList(), listOf(photo.copy(fileName = "../../databases/papyrus.db")), 0)
        assertThrows(InvalidBackupException::class.java) { BackupFormat.parse(evil) }
    }

    @Test
    fun rejectsGarbage() {
        assertThrows(InvalidBackupException::class.java) { BackupFormat.parse("not json") }
        assertThrows(InvalidBackupException::class.java) { BackupFormat.parse("{}") }
    }

    @Test
    fun unknownCategoryBecomesUncategorized() {
        val manifest = BackupFormat.parse(BackupFormat.toJson(emptyList(), listOf(insurance), 0))
        assertEquals(null, manifest.documents.single().categoryId)
    }

    @Test
    fun attachmentsRoundTripAndOldBackupsHaveNone() {
        val annex = io.github.andrearandazzo.papyrus.data.Attachment(
            id = 7, documentId = insurance.id, name = "Allegato B.pdf", fileName = "b.pdf",
            mimeType = "application/pdf", sizeBytes = 42, addedAt = 5,
        )
        val manifest = BackupFormat.parse(BackupFormat.toJson(emptyList(), listOf(insurance), 0, mapOf(insurance.id to listOf(annex))))
        assertEquals(
            listOf(BackupAttachment(name = "Allegato B.pdf", file = "b.pdf", mimeType = "application/pdf", sizeBytes = 42, addedAt = 5)),
            manifest.documents.single().attachments,
        )
        // A backup from before attachments: no field at all.
        val old = BackupFormat.toJson(emptyList(), listOf(insurance), 0).replace(Regex(",\\s*\"attachments\": \\[\\s*]"), "")
        assertTrue(!old.contains("attachments"))
        assertEquals(emptyList<BackupAttachment>(), BackupFormat.parse(old).documents.single().attachments)
    }

    @Test(expected = InvalidBackupException::class)
    fun attachmentPathsAreRejected() {
        val annex = io.github.andrearandazzo.papyrus.data.Attachment(
            documentId = insurance.id, name = "x", fileName = "../../databases/papyrus.db",
            mimeType = "application/pdf", sizeBytes = 1, addedAt = 0,
        )
        BackupFormat.parse(BackupFormat.toJson(emptyList(), listOf(insurance), 0, mapOf(insurance.id to listOf(annex))))
    }
}
