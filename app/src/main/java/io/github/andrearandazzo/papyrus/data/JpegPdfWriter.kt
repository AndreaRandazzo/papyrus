package io.github.andrearandazzo.papyrus.data

import java.io.ByteArrayOutputStream
import java.io.OutputStream

/** A page image already encoded as JPEG, [width] × [height] pixels. */
class JpegPage(val jpeg: ByteArray, val width: Int, val height: Int)

/**
 * Writes a PDF with one A4 page per JPEG image (portrait or landscape to suit it), each image
 * centered and as large as fits. The JPEG bytes are embedded as they are (DCTDecode), so a page
 * costs about as much as its photo; Android's PdfDocument stores images uncompressed instead.
 *
 * Plain Kotlin with no Android types, so it can be unit tested on the computer.
 */
object JpegPdfWriter {
    private const val A4_SHORT = 595
    private const val A4_LONG = 842

    fun write(pages: List<JpegPage>, out: OutputStream) {
        require(pages.isNotEmpty()) { "A PDF needs at least one page" }
        val pdf = CountingOutput(out)
        val offsets = mutableListOf<Int>()

        fun obj(number: Int, body: () -> Unit) {
            check(offsets.size == number - 1) { "Objects must be written in order" }
            offsets += pdf.count
            pdf.text("$number 0 obj\n")
            body()
            pdf.text("\nendobj\n")
        }

        // Object numbers: 1 catalog, 2 page tree, then three per page: page, image, contents.
        fun pageObj(i: Int) = 3 + i * 3

        pdf.text("%PDF-1.4\n")
        pdf.bytes(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))
        obj(1) { pdf.text("<< /Type /Catalog /Pages 2 0 R >>") }
        obj(2) {
            val kids = pages.indices.joinToString(" ") { "${pageObj(it)} 0 R" }
            pdf.text("<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>")
        }
        pages.forEachIndexed { i, page ->
            val landscape = page.width > page.height
            val pageWidth = if (landscape) A4_LONG else A4_SHORT
            val pageHeight = if (landscape) A4_SHORT else A4_LONG
            val scale = minOf(pageWidth.toDouble() / page.width, pageHeight.toDouble() / page.height)
            val w = page.width * scale
            val h = page.height * scale
            val x = (pageWidth - w) / 2
            val y = (pageHeight - h) / 2
            val content = "q ${n(w)} 0 0 ${n(h)} ${n(x)} ${n(y)} cm /Im0 Do Q".toByteArray(Charsets.US_ASCII)

            obj(pageObj(i)) {
                pdf.text(
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] " +
                        "/Resources << /XObject << /Im0 ${pageObj(i) + 1} 0 R >> >> /Contents ${pageObj(i) + 2} 0 R >>",
                )
            }
            obj(pageObj(i) + 1) {
                pdf.text(
                    "<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} " +
                        "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.jpeg.size} >>\nstream\n",
                )
                pdf.bytes(page.jpeg)
                pdf.text("\nendstream")
            }
            obj(pageObj(i) + 2) {
                pdf.text("<< /Length ${content.size} >>\nstream\n")
                pdf.bytes(content)
                pdf.text("\nendstream")
            }
        }

        val xref = pdf.count
        pdf.text("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { pdf.text("%010d 00000 n \n".format(it)) }
        pdf.text("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        pdf.flush()
    }

    /** Coordinates with two decimals and a dot, whatever the phone's language. */
    private fun n(value: Double) = "%.2f".format(java.util.Locale.ROOT, value)

    /** Tracks the byte offset of each object, which the PDF's index (xref) lists. */
    private class CountingOutput(private val out: OutputStream) {
        var count = 0
            private set

        fun bytes(data: ByteArray) {
            out.write(data)
            count += data.size
        }

        fun text(value: String) = bytes(value.toByteArray(Charsets.US_ASCII))

        fun flush() = out.flush()
    }

    /** Convenience for tests and small documents. */
    fun toBytes(pages: List<JpegPage>): ByteArray = ByteArrayOutputStream().also { write(pages, it) }.toByteArray()
}
