package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JpegPdfWriterTest {

    // The writer doesn't decode images, so any bytes stand in for a JPEG.
    private val portrait = JpegPage(ByteArray(1000) { 7 }, width = 1500, height = 2000)
    private val landscape = JpegPage(ByteArray(500) { 9 }, width = 2000, height = 1500)

    private val pdf = String(JpegPdfWriter.toBytes(listOf(portrait, landscape)), Charsets.ISO_8859_1)

    @Test
    fun hasOnePagePerImageInOrder() {
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.trimEnd().endsWith("%%EOF"))
        assertTrue(pdf.contains("/Kids [3 0 R 6 0 R] /Count 2"))
    }

    @Test
    fun picksPortraitOrLandscapeA4ToSuitEachImage() {
        val boxes = Regex("/MediaBox \\[0 0 (\\d+) (\\d+)]").findAll(pdf).map { it.groupValues[1] + "x" + it.groupValues[2] }
        assertEquals(listOf("595x842", "842x595"), boxes.toList())
    }

    @Test
    fun embedsTheJpegBytesAsTheyAre() {
        assertTrue(pdf.contains("/Width 1500 /Height 2000 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length 1000"))
        assertTrue(pdf.contains("\u0007".repeat(1000)))
    }

    @Test
    fun indexPointsAtEveryObject() {
        // Each xref entry must be the byte offset where "N 0 obj" starts, or readers can't open the file.
        val xrefStart = Regex("startxref\n(\\d+)").find(pdf)!!.groupValues[1].toInt()
        val entries = Regex("(\\d{10}) 00000 n ").findAll(pdf.substring(xrefStart)).map { it.groupValues[1].toInt() }.toList()
        assertEquals(8, entries.size) // catalog, page tree, 3 objects per page
        entries.forEachIndexed { i, offset -> assertTrue(pdf.startsWith("${i + 1} 0 obj", offset)) }
    }

    @Test
    fun imageIsCenteredAndScaledToFit() {
        // 1500×2000 on 595×842: limited by width (scale 0.3967), centered vertically.
        assertTrue(pdf.contains("q 595.00 0 0 793.33 0.00 24.33 cm /Im0 Do Q"))
    }
}
