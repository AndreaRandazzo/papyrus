package io.github.andrearandazzo.papyrus.ui.settings

import java.net.URLEncoder

/** Device facts pre-filled into feedback, so reporters don't have to look them up. */
data class DeviceInfo(val appVersion: String, val androidVersion: String, val device: String)

/**
 * Links that open the GitHub issue forms in .github/ISSUE_TEMPLATE, pre-filled through query
 * parameters named after the forms' field ids. Nothing is sent: the user reviews and submits.
 */
object FeedbackLinks {
    const val REPOSITORY = "https://github.com/AndreaRandazzo/papyrus"
    const val LICENSE = "https://www.gnu.org/licenses/gpl-3.0.html"

    /** Shown as an option only when set. Public: it ends up in the app and its source code. */
    const val EMAIL = "papyrus.app00@gmail.com"

    fun bugReport(info: DeviceInfo): String = issueForm(
        "bug_report.yml",
        "app-version" to info.appVersion,
        "android-version" to info.androidVersion,
        "device" to info.device,
    )

    fun featureRequest(info: DeviceInfo): String = issueForm("feature_request.yml", "app-version" to info.appVersion)

    /** Body of a feedback email: the same facts as the forms, as plain text. */
    fun emailBody(info: DeviceInfo): String =
        "\n\n---\nPapyrus ${info.appVersion}\n${info.androidVersion}\n${info.device}\n"

    private fun issueForm(template: String, vararg fields: Pair<String, String>): String =
        "$REPOSITORY/issues/new?" + (listOf("template" to template) + fields)
            .joinToString("&") { (key, value) -> "$key=${encode(value)}" }

    // URLEncoder writes spaces as "+", which GitHub would show literally in the form.
    // The charset is passed by name: the Charset overload only exists on Android 13+.
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
