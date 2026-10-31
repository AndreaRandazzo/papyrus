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
 * Previews of a PDF's first page or of a photo, kept in memory while the app runs.
 * Other types (e.g. Word) have no preview and return null.
 */
class ThumbnailLoader(private val repository: DocumentRepository) {

    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** At most two previews are rendered at once, so scrolling a long list stays smooth. */
    private val dispatcher = Dispatchers.IO.limitedParallelism(2)

    /** A preview about [widthPx] wide, or null if there is none or the file can't be read. */
    suspend fun load(document: Document, widthPx: Int): Bitmap? {
        if (document.kind != DocumentKind.PDF && document.kind != DocumentKind.IMAGE) return null
        val key = "${document.fileName}@$widthPx"
        cache.get(key)?.let { return it }
        return withContext(dispatcher) {
            cache.get(key) ?: try {
                val file = repository.fileOf(document)
                when (document.kind) {
                    DocumentKind.PDF -> renderPdfFirstPage(file, widthPx)
                    else -> decodeImage(file, widthPx)
                }
            } catch (e: Exception) {
                // A damaged or password-protected file just shows no preview.
                Log.w(TAG, "No preview for ${document.fileName}", e)
                null
            }?.also { cache.put(key, it) }
        }
    }

    private fun renderPdfFirstPage(file: File, widthPx: Int): Bitmap? =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return@use null
                renderer.openPage(0).use { page ->
                    val height = (widthPx.toLong() * page.height / page.width).toInt().coerceIn(1, widthPx * 3)
                    Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE) // PDF pages are transparent by default.
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
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
