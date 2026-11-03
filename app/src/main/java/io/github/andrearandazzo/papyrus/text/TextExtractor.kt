package io.github.andrearandazzo.papyrus.text

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ext.SdkExtensions
import androidx.core.graphics.createBitmap
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.data.PdfMaker
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reads the text inside a stored document. PDFs that already contain text are read directly;
 * pages that are only an image (scans) and photos go through [Ocr]. Word .docx is read from the file.
 */
class TextExtractor(private val ocr: Ocr) {

    fun read(file: File, kind: DocumentKind): String = when (kind) {
        DocumentKind.PDF -> readPdf(file)
        DocumentKind.IMAGE -> ocr.read(PdfMaker.loadUpright(file, rotation = 0, maxSide = MAX_SIDE)).trim()
        DocumentKind.WORD -> if (file.extension.equals("docx", ignoreCase = true)) WordText.read(file) else ""
        DocumentKind.OTHER -> ""
    }

    private fun readPdf(file: File): String {
        val embedded = embeddedPdfText(file)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pages = minOf(renderer.pageCount, MAX_PAGES)
                return (0 until pages).joinToString("\n\n") { index ->
                    // A page with (almost) no text of its own is probably a scan: read its picture.
                    embedded?.getOrNull(index)?.takeIf { it.trim().length >= MIN_PAGE_TEXT }
                        ?: renderer.openPage(index).use { page -> ocr.read(render(page)) }
                }.trim()
            }
        }
    }

    /** About 200 dpi, capped at [MAX_SIDE]: sharp enough for small print, light on memory. */
    private fun render(page: PdfRenderer.Page): Bitmap {
        val scale = minOf(200f / 72f, MAX_SIDE.toFloat() / max(page.width, page.height))
        return createBitmap((page.width * scale).roundToInt(), (page.height * scale).roundToInt())
            .apply {
                eraseColor(Color.WHITE)
                page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
    }

    /**
     * The text a PDF carries, page by page: Android 15+ reads it itself, Android 12–14 through the
     * PDF module Google Play updates. Null where neither is available; then every page is recognized.
     */
    @SuppressLint("NewApi") // Guarded by the version and extension checks.
    private fun embeddedPdfText(file: File): List<String>? = try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            when {
                Build.VERSION.SDK_INT >= 35 -> PdfRenderer(fd).use { renderer ->
                    (0 until minOf(renderer.pageCount, MAX_PAGES)).map { i ->
                        renderer.openPage(i).use { page -> page.textContents.joinToString("\n") { it.text } }
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13 -> PdfRendererPreV(fd).use { renderer ->
                    (0 until minOf(renderer.pageCount, MAX_PAGES)).map { i ->
                        renderer.openPage(i).use { page -> page.textContents.joinToString("\n") { it.text } }
                    }
                }
                else -> null
            }
        }
    } catch (e: Exception) {
        null // Unreadable text layer: fall back to recognizing the pages.
    }

    private companion object {
        /** Long documents: the first pages are enough to find them, and reading stays quick. */
        const val MAX_PAGES = 20
        const val MAX_SIDE = 2500
        const val MIN_PAGE_TEXT = 20
    }
}
