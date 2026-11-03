package io.github.andrearandazzo.papyrus.text

import org.junit.Assert.assertEquals
import org.junit.Test

class WordTextTest {
    @Test
    fun keepsParagraphsAndJoinsRunsOfTheSameParagraph() {
        val xml = """<w:document><w:body>
            <w:p><w:r><w:t>Contratto di </w:t></w:r><w:r><w:rPr><w:b/></w:rPr><w:t xml:space="preserve">locazione</w:t></w:r></w:p>
            <w:p><w:r><w:t>Canone: 750 &amp; spese</w:t><w:tab/><w:t>mensili</w:t></w:r></w:p>
            <w:p></w:p>
        </w:body></w:document>"""
        assertEquals("Contratto di locazione\nCanone: 750 & spese mensili", WordText.fromXml(xml))
    }
}
