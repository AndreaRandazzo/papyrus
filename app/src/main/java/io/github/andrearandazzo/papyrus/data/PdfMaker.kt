package io.github.andrearandazzo.papyrus.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Turns photographed pages into one PDF. */
object PdfMaker {
    /** Longest side a page photo is reduced to: sharp enough to read and print, small enough to store. */
    private const val MAX_SIDE_PX = 2000

    /** JPEG quality per page: clear text at a few hundred KB per page. */
    private const val JPEG_QUALITY = 80

    /** One A4 page per photo, in order, each turned upright (see [JpegPdfWriter]). */
    suspend fun write(pages: List<ScanPage>, out: File) = withContext(Dispatchers.IO) {
        val jpegPages = pages.map { page ->
            val bitmap = render(page, MAX_SIDE_PX)
            try {
                val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
                JpegPage(jpeg.toByteArray(), bitmap.width, bitmap.height)
            } finally {
                bitmap.recycle()
            }
        }
        out.outputStream().buffered().use { JpegPdfWriter.write(jpegPages, it) }
    }

    /** The page as it goes into the PDF: upright, then cropped and straightened if it has corners. */
    fun render(page: ScanPage, maxSide: Int): Bitmap {
        val upright = loadUpright(File(page.path), page.rotation, maxSide)
        val corners = page.corners ?: return upright
        return try {
            straighten(upright, corners)
        } finally {
            upright.recycle()
        }
    }

    /**
     * Maps the paper's four [corners] onto a rectangle, like photographing it from straight above.
     * Uses Android's own perspective transform (Matrix.setPolyToPoly).
     */
    private fun straighten(source: Bitmap, corners: Corners): Bitmap {
        val (width, height) = corners.outputSize(source.width, source.height)
        val from = corners.points.flatMap { (x, y) -> listOf(x * source.width, y * source.height) }.toFloatArray()
        val to = floatArrayOf(0f, 0f, width.toFloat(), 0f, width.toFloat(), height.toFloat(), 0f, height.toFloat())
        val matrix = Matrix().apply { setPolyToPoly(from, 0, to, 0, 4) }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { result ->
            Canvas(result).apply {
                drawColor(Color.WHITE)
                drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            }
        }
    }

    /**
     * Decodes a photo at most [maxSide] pixels on its longest side, turned upright: first by the
     * camera's own orientation tag, then by the user's extra [rotation].
     */
    fun loadUpright(file: File, rotation: Int, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("Not an image: ${file.name}")

        val degrees = (exifRotation(file) + rotation) % 360
        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
        if (degrees == 0 && scale == 1f) return decoded
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(degrees.toFloat())
        }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }

    private fun exifRotation(file: File): Int = try {
        when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}
