package io.github.andrearandazzo.papyrus.text

import android.util.Log
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.DocumentText
import io.github.andrearandazzo.papyrus.data.DocumentTextDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Reads the text of every document that hasn't been read yet, one at a time, in the background:
 * new imports and scans, and after an update or a restore, the whole library. Runs while Papyrus is
 * open; what's left is picked up next time.
 */
class TextReader(
    private val dao: DocumentTextDao,
    private val documents: DocumentRepository,
    private val ocr: Ocr,
) {
    private val extractor = TextExtractor(ocr)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        // One document at a time: recognition is heavy and the phone should stay responsive.
        scope.launch(Dispatchers.Default.limitedParallelism(1)) {
            dao.observeUnread().conflate().collect { unread ->
                for (document in unread) {
                    val text = try {
                        extractor.read(documents.fileOf(document), document.kind)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        // A damaged or unusual file: remember there's nothing to read, don't retry forever.
                        Log.w(TAG, "Couldn't read the text of document ${document.id}", e)
                        ""
                    }
                    try {
                        dao.upsert(DocumentText(document.id, text))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Document ${document.id} was deleted while being read", e)
                    }
                }
                ocr.close()
            }
        }
    }

    private companion object {
        const val TAG = "TextReader"
    }
}
