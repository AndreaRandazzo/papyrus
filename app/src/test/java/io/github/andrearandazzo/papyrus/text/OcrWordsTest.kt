package io.github.andrearandazzo.papyrus.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Words and scores taken from real recognitions (a handwritten receipt, a photo of a screen). */
class OcrWordsTest {

    @Test
    fun keepsRealWordsEvenWithLowScores() {
        listOf("discussed" to 17, "made" to 0, "Gymfit" to 26, "He's" to 84, "I'd" to 50, "Fratelli" to 93,
            "n.12" to 92, "48800817" to 95, "$10,000." to 95, "(TP)" to 96, "EnglishClub.com" to 92, "One-word" to 92)
            .forEach { (word, score) -> assertTrue(word, OcrWords.keep(word, score)) }
    }

    @Test
    fun dropsNoise() {
        listOf("|" to 89, "\\" to 92, "_|" to 73, "*" to 91, "¢" to 80, "=" to 75, "RaAdsBr7rc" to 0,
            "ASBA2S" to 25, "A\\pl" to 21, "e" to 63, ")Q" to 63, "<>" to 64, "——_—" to 7)
            .forEach { (word, score) -> assertFalse(word, OcrWords.keep(word, score)) }
    }

    @Test
    fun singleCharactersOnlyWhenSure() {
        assertTrue(OcrWords.keep("€", 96).not()) // a symbol alone says nothing searchable
        assertTrue(OcrWords.keep("6", 96))
        assertFalse(OcrWords.keep("2", 73))
    }

    @Test
    fun joinsLinesAndSkipsEmptyOnes() {
        val lines = listOf(
            listOf("RICEVUTA" to 96, "n." to 91, "|" to 88),
            listOf("\\" to 92, "_|" to 63),
            listOf("Gymfit" to 66, "Via" to 93, "Fratelli" to 93),
        )
        assertEquals("RICEVUTA n.\nGymfit Via Fratelli", OcrWords.join(lines))
    }
}
