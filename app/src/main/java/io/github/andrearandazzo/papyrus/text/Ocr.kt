package io.github.andrearandazzo.papyrus.text

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File

/**
 * Text recognition with Tesseract, entirely on the phone. The Italian and English models ship
 * inside the app (assets/tessdata, Apache-2.0); nothing is downloaded and nothing leaves the phone.
 */
class Ocr(private val context: Context) {
    private var api: TessBaseAPI? = null

    /**
     * Recognizes [bitmap] and returns the words worth keeping, line by line (see [OcrWords]).
     * "Sparse text" mode looks for text anywhere on the page instead of expecting paragraphs, which
     * reads forms, receipts and stamps much better and normal pages just as well.
     */
    fun read(bitmap: Bitmap): String {
        val tess = api ?: TessBaseAPI().also {
            check(it.init(dataPath(), LANGUAGES)) { "Tesseract couldn't start" }
            it.pageSegMode = TessBaseAPI.PageSegMode.PSM_SPARSE_TEXT
            api = it
        }
        tess.setImage(bitmap)
        tess.utF8Text // runs the recognition
        val lines = mutableListOf<MutableList<Pair<String, Int>>>()
        val words = tess.resultIterator
        try {
            words.begin()
            do {
                val word = words.getUTF8Text(WORD) ?: continue
                if (lines.isEmpty() || words.isAtBeginningOf(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)) {
                    lines += mutableListOf<Pair<String, Int>>()
                }
                lines.last() += word to words.confidence(WORD).toInt()
            } while (words.next(WORD))
        } finally {
            words.delete()
            tess.clear()
        }
        return OcrWords.join(lines)
    }

    /** Frees the engine's memory (tens of MB) when there's nothing left to read. */
    fun close() {
        api?.recycle()
        api = null
    }

    /**
     * Tesseract reads its models from files: copied once from the app, outside backups. Copied to a
     * temporary name first, so an interrupted copy never leaves a broken model behind. Shipping new
     * models means bumping [MODELS_VERSION].
     */
    private fun dataPath(): String {
        val root = File(context.noBackupFilesDir, "tesseract-$MODELS_VERSION")
        val dir = File(root, "tessdata").apply { mkdirs() }
        for (language in LANGUAGES.split("+")) {
            val model = File(dir, "$language.traineddata")
            if (model.exists()) continue
            val partial = File(dir, "$language.partial")
            context.assets.open("tessdata/$language.traineddata").use { input ->
                partial.outputStream().use { input.copyTo(it) }
            }
            check(partial.renameTo(model)) { "Couldn't install the $language model" }
        }
        return root.path
    }

    private companion object {
        const val WORD = TessBaseAPI.PageIteratorLevel.RIL_WORD
        const val LANGUAGES = "ita+eng"
        const val MODELS_VERSION = 1
    }
}
