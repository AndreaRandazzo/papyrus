package io.github.andrearandazzo.papyrus.ui.home

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentKind

/** File types offered in the system file picker. */
private val IMPORTABLE_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/*",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources

    // The system picker needs no storage permission: the user chooses exactly what we can read.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris)
    }

    LaunchedEffect(viewModel) {
        viewModel.importResults.collect { result ->
            val message = if (result.failed == 0) {
                resources.getQuantityString(R.plurals.import_done, result.imported, result.imported)
            } else {
                resources.getQuantityString(R.plurals.import_failed, result.failed, result.failed)
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(IMPORTABLE_TYPES) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.import_documents))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (documents.isEmpty()) {
                EmptyState()
            } else {
                DocumentList(documents)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DocumentList(documents: List<Document>) {
    // Bottom padding keeps the last row clear of the "+" button.
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(documents, key = { it.id }) { document ->
            DocumentRow(document)
            HorizontalDivider()
        }
    }
}

@Composable
private fun DocumentRow(document: Document) {
    val context = LocalContext.current
    val size = Formatter.formatShortFileSize(context, document.sizeBytes)
    val date = DateUtils.formatDateTime(
        context,
        document.importedAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
    )
    ListItem(
        leadingContent = { KindBadge(document.kind) },
        headlineContent = { Text(document.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("$size · $date") },
    )
}

@Composable
private fun KindBadge(kind: DocumentKind) {
    val (label, color) = when (kind) {
        DocumentKind.PDF -> stringResource(R.string.kind_pdf) to Color(0xFFC62828)
        DocumentKind.WORD -> stringResource(R.string.kind_word) to Color(0xFF1565C0)
        DocumentKind.IMAGE -> stringResource(R.string.kind_image) to Color(0xFF2E7D32)
        DocumentKind.OTHER -> stringResource(R.string.kind_other) to Color(0xFF616161)
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
