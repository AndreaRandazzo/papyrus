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

    private val tilted = Corners(listOf(0.1f to 0.2f, 0.9f to 0.1f, 0.8f to 0.9f, 0.2f to 0.8f))

    @Test
    fun rotatingFourTimesGivesBackTheSameCrop() {
        var corners = tilted
        repeat(4) { corners = corners.rotatedClockwise() }
        corners.points.zip(tilted.points).forEach { (a, b) ->
            assertEquals(b.first, a.first, 1e-6f)
            assertEquals(b.second, a.second, 1e-6f)
        }
    }

    @Test
    fun rotatingKeepsTopLeftFirst() {
        // After a clockwise turn the old bottom-left (0.2, 0.8) sits top-left at (0.2, 0.2).
        val rotated = Corners.FULL.rotatedClockwise()
        assertEquals(Corners.FULL, rotated)
        val (x, y) = tilted.rotatedClockwise().points[0]
        assertEquals(0.2f, x, 1e-6f)
        assertEquals(0.2f, y, 1e-6f)
    }

    @Test
    fun rotatingAPageRotatesItsCrop() {
        val cropped = ScanPages.crop(pages, 0, tilted)
        assertEquals(tilted.rotatedClockwise(), ScanPages.rotate(cropped, 0)[0].corners)
    }

    @Test
    fun cropToTheWholePhotoMeansNoCrop() {
        assertEquals(null, ScanPages.crop(pages, 0, Corners.FULL)[0].corners)
    }

    @Test
    fun outputSizeAveragesOppositeEdges() {
        // A 1000×1000 photo; top edge 800 px, bottom 600 px, sides 1000 px.
        val trapezoid = Corners(listOf(0.1f to 0f, 0.9f to 0f, 0.8f to 1f, 0.2f to 1f))
        val (w, h) = trapezoid.outputSize(1000, 1000)
        assertEquals(700, w)
        assertEquals(1005, h) // the slanted sides are a little longer than 1000
    }

    @Test
    fun cornersSurviveSavedStateEncoding() {
        assertEquals(tilted, Corners.decode(tilted.encode()))
        assertEquals(null, Corners.decode(""))
    }

    @Test
    fun movingPastTheEndsChangesNothing() {
        assertEquals(pages, ScanPages.move(pages, 0, -1))
        assertEquals(pages, ScanPages.move(pages, 2, +1))
        assertEquals(pages, ScanPages.rotate(pages, 7))
    }
}
