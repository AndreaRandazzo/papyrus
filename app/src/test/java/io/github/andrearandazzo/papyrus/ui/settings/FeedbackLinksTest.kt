package io.github.andrearandazzo.papyrus.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackLinksTest {

    private val info = DeviceInfo(appVersion = "1.1.0", androidVersion = "Android 15", device = "Google Pixel 8 & Co")

    @Test
    fun bugReportOpensTheBugFormPreFilled() {
        assertEquals(
            "https://github.com/AndreaRandazzo/papyrus/issues/new?template=bug_report.yml" +
                "&app-version=1.1.0&android-version=Android%2015&device=Google%20Pixel%208%20%26%20Co",
            FeedbackLinks.bugReport(info),
        )
    }

    @Test
    fun featureRequestOpensTheFeatureFormWithTheVersion() {
        assertEquals(
            "https://github.com/AndreaRandazzo/papyrus/issues/new?template=feature_request.yml&app-version=1.1.0",
            FeedbackLinks.featureRequest(info),
        )
    }

    @Test
    fun templateNamesMatchTheFormFiles() {
        // Renaming a form in .github/ISSUE_TEMPLATE without updating the app would break the links.
        val root = generateSequence(java.io.File("").absoluteFile) { it.parentFile }
            .first { java.io.File(it, ".github").isDirectory }
        listOf("bug_report.yml", "feature_request.yml").forEach {
            assertTrue(it, java.io.File(root, ".github/ISSUE_TEMPLATE/$it").isFile)
        }
    }
}
