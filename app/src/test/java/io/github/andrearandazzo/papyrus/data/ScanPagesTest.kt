package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanPagesTest {

    private val pages = listOf(ScanPage("a"), ScanPage("b"), ScanPage("c"))

    private fun List<ScanPage>.paths() = map { it.path }

    @Test
    fun rotateTurnsOnlyThatPageAndWrapsAround() {
        var result = pages
        repeat(5) { result = ScanPages.rotate(result, 1) }
        assertEquals(listOf(0, 90, 0), result.map { it.rotation }) // 5 × 90° = 450° = 90°
    }

    @Test
    fun removeDropsThePage() {
        assertEquals(listOf("a", "c"), ScanPages.remove(pages, 1).paths())
    }

    @Test
    fun moveSwapsWithNeighbour() {
        assertEquals(listOf("b", "a", "c"), ScanPages.move(pages, 1, -1).paths())
        assertEquals(listOf("a", "c", "b"), ScanPages.move(pages, 1, +1).paths())
    }

    @Test
    fun movingPastTheEndsChangesNothing() {
        assertEquals(pages, ScanPages.move(pages, 0, -1))
        assertEquals(pages, ScanPages.move(pages, 2, +1))
        assertEquals(pages, ScanPages.rotate(pages, 7))
    }
}
