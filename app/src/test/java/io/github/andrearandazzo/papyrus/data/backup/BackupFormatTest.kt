package io.github.andrearandazzo.papyrus.data.backup

import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import org.junit.Assert.assertEquals
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

        assertEquals(listOf(BackupCategory(7, "Car", "car"), BackupCategory(8, "Dog", null)), manifest.categories)
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
    fun readsBackupsMadeBeforeIcons() {
        val old = """{"format":1,"categories":[{"id":1,"name":"Car"}],"documents":[]}"""
        assertEquals(listOf(BackupCategory(1, "Car", null)), BackupFormat.parse(old).categories)
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
}
