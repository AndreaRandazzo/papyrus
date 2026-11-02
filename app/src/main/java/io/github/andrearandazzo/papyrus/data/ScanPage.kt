package io.github.andrearandazzo.papyrus.data

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * One photographed page of a scan. [rotation] is the user's extra turn on top of the photo's own
 * orientation: 0, 90, 180 or 270 degrees clockwise. [corners] are where the paper's corners are in
 * the upright photo, or null for the whole photo.
 */
data class ScanPage(val path: String, val rotation: Int = 0, val corners: Corners? = null)

/**
 * The paper's four corners as fractions of the image (0..1), in the order top-left, top-right,
 * bottom-right, bottom-left. Fractions keep them valid at any image resolution.
 */
data class Corners(val points: List<Pair<Float, Float>>) {
    init {
        require(points.size == 4) { "A page has four corners" }
    }

    /** The same corners after turning the image 90° clockwise. */
    fun rotatedClockwise(): Corners {
        // A point (x, y) moves to (1 - y, x); the old bottom-left becomes the new top-left.
        val (tl, tr, br, bl) = points.map { (x, y) -> 1f - y to x }
        return Corners(listOf(bl, tl, tr, br))
    }

    /**
     * Size of the straightened page for an image of [width] × [height] pixels: the average length of
     * opposite edges, so a sheet shot at an angle keeps its real proportions.
     */
    fun outputSize(width: Int, height: Int): Pair<Int, Int> {
        val px = points.map { (x, y) -> x * width to y * height }
        fun length(a: Int, b: Int) = hypot(px[a].first - px[b].first, px[a].second - px[b].second)
        val w = (length(0, 1) + length(3, 2)) / 2
        val h = (length(0, 3) + length(1, 2)) / 2
        return w.roundToInt().coerceAtLeast(1) to h.roundToInt().coerceAtLeast(1)
    }

    /** Compact text form for saved state: "x,y,x,y,x,y,x,y". */
    fun encode(): String = points.joinToString(",") { (x, y) -> "$x,$y" }

    companion object {
        /** The whole image. */
        val FULL = Corners(listOf(0f to 0f, 1f to 0f, 1f to 1f, 0f to 1f))

        /** Reads [encode]'s form; anything else (e.g. empty) means no crop. */
        fun decode(text: String): Corners? {
            val numbers = text.split(",").mapNotNull { it.toFloatOrNull() }
            if (numbers.size != 8) return null
            return Corners(numbers.chunked(2).map { (x, y) -> x to y })
        }
    }
}

/** Editing operations on a scan's pages. Each returns a new list; out-of-range indexes change nothing. */
object ScanPages {
    /** Turns a page 90° clockwise; its crop turns with it. */
    fun rotate(pages: List<ScanPage>, index: Int): List<ScanPage> = pages.mapIndexed { i, page ->
        if (i == index) page.copy(rotation = (page.rotation + 90) % 360, corners = page.corners?.rotatedClockwise()) else page
    }

    fun crop(pages: List<ScanPage>, index: Int, corners: Corners?): List<ScanPage> = pages.mapIndexed { i, page ->
        if (i == index) page.copy(corners = corners?.takeIf { it != Corners.FULL }) else page
    }

    fun remove(pages: List<ScanPage>, index: Int): List<ScanPage> = pages.filterIndexed { i, _ -> i != index }

    /** Moves the page at [index] by [delta] places (-1 = earlier, +1 = later). */
    fun move(pages: List<ScanPage>, index: Int, delta: Int): List<ScanPage> {
        val target = index + delta
        if (index !in pages.indices || target !in pages.indices) return pages
        return pages.toMutableList().apply { add(target, removeAt(index)) }
    }
}
