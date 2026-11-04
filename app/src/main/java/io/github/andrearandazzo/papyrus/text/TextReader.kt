package io.github.andrearandazzo.papyrus.text

import android.util.Log
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.DocumentText
import io.github.andrearandazzo.papyrus.data.DocumentTextDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
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
            dao.observeUnread().conflate().collect { first ->
                var unread = first
                while (unread.isNotEmpty()) {
                    var skipped = false
                    for (document in unread) {
                        // The document's own file, then its attachments, each read on its own.
                        val attached = documents.attachmentsOf(document.id)
                        val files = listOf(documents.fileOf(document) to document.kind) +
                            attached.map { documents.fileOf(it) to it.kind }
                        val text = files.joinToString("\n\n") { (file, kind) ->
                            try {
                                extractor.read(file, kind)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Throwable) {
                                // A damaged or unusual file: nothing to read in it, don't retry forever.
                                Log.w(TAG, "Couldn't read the text of ${file.name} (document ${document.id})", e)
                                ""
                            }
                        }.trim()
                        // Files changed while reading (attachment added, file replaced): leave it unread,
                        // the next round reads the new set.
                        val now = documents.observe(document.id).first()
                        if (now?.fileName != document.fileName || documents.attachmentsOf(document.id) != attached) {
                            skipped = true
                            continue
                        }
                        try {
                            dao.upsert(DocumentText(document.id, text))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "Document ${document.id} was deleted while being read", e)
                        }
                    }
                    // Only what's still unread, if a document changed while being read.
                    unread = if (skipped) dao.getUnread() else emptyList()
                }
                ocr.close()
            }
        }
    }

    private companion object {
        const val TAG = "TextReader"
    }
}
