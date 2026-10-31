package io.github.andrearandazzo.papyrus.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Previews of PDF pages and photos, kept in memory while the app runs.
 * Other types (e.g. Word) have no preview.
 */
class ThumbnailLoader(private val repository: DocumentRepository) {

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** At most two previews are rendered at once, so scrolling a long list stays smooth. */
    private val dispatcher = Dispatchers.IO.limitedParallelism(2)

    /** How many pages can be previewed: all pages of a PDF, 1 for a photo, 0 otherwise. */
    suspend fun pageCount(document: Document): Int = when (document.kind) {
        DocumentKind.IMAGE -> 1
        DocumentKind.PDF -> withContext(dispatcher) {
            try {
                openPdf(repository.fileOf(document)) { it.pageCount }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot count pages of ${document.fileName}", e)
                0
            }
        }
        else -> 0
    }

    /** A preview of [page] about [widthPx] wide, or null if there is none or the file can't be read. */
    suspend fun load(document: Document, widthPx: Int, page: Int = 0): Bitmap? {
        if (document.kind != DocumentKind.PDF && document.kind != DocumentKind.IMAGE) return null
        val key = "${document.fileName}#$page@$widthPx"
        cache.get(key)?.let { return it }
        return withContext(dispatcher) {
            cache.get(key) ?: try {
                val file = repository.fileOf(document)
                when (document.kind) {
                    DocumentKind.PDF -> renderPdfPage(file, page, widthPx)
                    else -> decodeImage(file, widthPx)
                }
            } catch (e: Exception) {
                // A damaged or password-protected file just shows no preview.
                Log.w(TAG, "No preview for ${document.fileName}", e)
                null
            }?.also { cache.put(key, it) }
        }
    }

    private fun <T> openPdf(file: File, block: (PdfRenderer) -> T): T =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use(block)
        }

    private fun renderPdfPage(file: File, page: Int, widthPx: Int): Bitmap? = openPdf(file) { renderer ->
        if (page !in 0 until renderer.pageCount) return@openPdf null
        renderer.openPage(page).use { pdfPage ->
            val height = (widthPx.toLong() * pdfPage.height / pdfPage.width).toInt().coerceIn(1, widthPx * 3)
            Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE) // PDF pages are transparent by default.
                pdfPage.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        }
    }

    private fun decodeImage(file: File, widthPx: Int): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also applies the photo's rotation (EXIF), so it isn't sideways.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                decoder.setTargetSampleSize((info.size.width / widthPx).coerceAtLeast(1))
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= widthPx) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }

    private companion object {
        const val TAG = "ThumbnailLoader"
    }
}
