package io.github.andrearandazzo.papyrus

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import io.github.andrearandazzo.papyrus.ui.PapyrusNavHost
import io.github.andrearandazzo.papyrus.ui.theme.PapyrusActivity
import io.github.andrearandazzo.papyrus.ui.theme.setPapyrusContent

class MainActivity : PapyrusActivity() {
    /** A document to open, from tapping an expiry reminder; cleared once it's shown. */
    private val documentToOpen = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        syncStarterCategories()
        // Only a fresh launch: after rotation the document is already open.
        if (savedInstanceState == null) documentToOpen.value = intent.documentId()
        setPapyrusContent {
            val documentId by documentToOpen.collectAsStateWithLifecycle()
            PapyrusNavHost(documentToOpen = documentId, onDocumentOpened = { documentToOpen.value = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        documentToOpen.value = intent.documentId()
    }

    private fun Intent.documentId(): Long? = getLongExtra(EXTRA_DOCUMENT_ID, -1).takeIf { it > 0 }

    companion object {
        const val EXTRA_DOCUMENT_ID = "documentId"
    }
}
