package io.github.andrearandazzo.papyrus.text

/**
 * Which recognized words to keep. Handwriting, stamps and creases come out as noise like
 * `—— € @l_ | ____TTtTr=oent`; real words can still get a low score, so the score alone isn't enough.
 * A word is kept when it looks like a word or a number, or when Tesseract is very sure of it.
 */
object OcrWords {
    /** Score (0–100) needed to keep something that doesn't look like a word, or a single character. */
    private const val SURE = 85

    /** Score needed to keep a code that mixes letters and digits. */
    private const val CODE = 50

    private val EDGE_PUNCTUATION = Regex("^[\\p{Punct}“”‘’«»—–]+|[\\p{Punct}“”‘’«»—–]+$")

    fun keep(word: String, confidence: Int): Boolean {
        val core = word.replace(EDGE_PUNCTUATION, "")
        val letters = core.count { it.isLetter() }
        val digits = core.count { it.isDigit() }
        return when {
            letters + digits == 0 -> false // only symbols: | \ _ * ¢ =
            letters + digits == 1 -> confidence >= SURE && core.length == 1
            // Codes mixing letters and digits (n.12, AB123CD) are easy to invent from noise: need a decent score.
            letters > 0 && digits > 0 -> looksLikeWord(core, letters, digits) && confidence >= CODE || confidence >= SURE
            looksLikeWord(core, letters, digits) -> true
            else -> confidence >= SURE
        }
    }

    private fun looksLikeWord(core: String, letters: Int, digits: Int): Boolean {
        // Apostrophes and hyphens belong to words (l'importo, He's, One-word); dots to codes (n.12).
        val inner = core.count { it == '\'' || it == '’' || it == '-' || it == '.' }
        if (letters + digits + inner < core.length) return false
        if (letters == 0) return true // 48800817, 21.11.2026, 1,249.00
        val alpha = core.filter { it.isLetter() }
        val oneCase = alpha == alpha.lowercase() || alpha == alpha.uppercase()
        // Codes mix digits with letters of one case: n.12, CA00000DEMO; not RaAdsBr7rc.
        if (digits > 0) return oneCase
        // Words: "Gymfit", "RICEVUTA", "fratelli"; not "RaAdsBr".
        return oneCase || (alpha.first().isUpperCase() && alpha.drop(1) == alpha.drop(1).lowercase())
    }

    /** Joins kept words back into lines; empty lines are dropped. */
    fun join(lines: List<List<Pair<String, Int>>>): String = lines
        .map { line -> line.filter { (word, confidence) -> keep(word, confidence) }.joinToString(" ") { it.first } }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}
