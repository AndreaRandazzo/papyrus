package io.github.andrearandazzo.papyrus.data

/**
 * One photographed page of a scan. [rotation] is the user's extra turn on top of the photo's own
 * orientation: 0, 90, 180 or 270 degrees clockwise.
 */
data class ScanPage(val path: String, val rotation: Int = 0)

/** Editing operations on a scan's pages. Each returns a new list; out-of-range indexes change nothing. */
object ScanPages {
    fun rotate(pages: List<ScanPage>, index: Int): List<ScanPage> = pages.mapIndexed { i, page ->
        if (i == index) page.copy(rotation = (page.rotation + 90) % 360) else page
    }

    fun remove(pages: List<ScanPage>, index: Int): List<ScanPage> = pages.filterIndexed { i, _ -> i != index }

    /** Moves the page at [index] by [delta] places (-1 = earlier, +1 = later). */
    fun move(pages: List<ScanPage>, index: Int, delta: Int): List<ScanPage> {
        val target = index + delta
        if (index !in pages.indices || target !in pages.indices) return pages
        return pages.toMutableList().apply { add(target, removeAt(index)) }
    }
}
