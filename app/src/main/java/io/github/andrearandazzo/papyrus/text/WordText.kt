package io.github.andrearandazzo.papyrus.text

import java.io.File
import java.util.zip.ZipFile

/**
 * The text of a Word document (.docx): it's a zip, and the text sits in word/document.xml as
 * <w:t> runs inside <w:p> paragraphs. Old .doc files are a different format and aren't read.
 */
object WordText {
    fun read(file: File): String = ZipFile(file).use { zip ->
        val entry = zip.getEntry("word/document.xml") ?: return ""
        fromXml(zip.getInputStream(entry).bufferedReader().readText())
    }

    private val PARAGRAPH_END = Regex("</w:p>")
    private val TAB_OR_BREAK = Regex("<w:(tab|br|cr)\\b[^>]*/>")
    private val TEXT_RUN = Regex("<w:t(?:\\s[^>]*)?>([^<]*)</w:t>")
    private val TAG = Regex("<[^>]+>")

    fun fromXml(xml: String): String = xml
        .replace(PARAGRAPH_END, "\n")
        .replace(TAB_OR_BREAK, "<w:t> </w:t>")
        .let { withBreaks ->
            // Keep only the text runs, and the line breaks between paragraphs.
            withBreaks.split("\n").joinToString("\n") { paragraph ->
                TEXT_RUN.findAll(paragraph).joinToString("") { it.groupValues[1] }
            }
        }
        .replace(TAG, "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}
