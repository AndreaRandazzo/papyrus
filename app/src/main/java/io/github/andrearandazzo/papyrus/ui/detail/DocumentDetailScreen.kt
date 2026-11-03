package io.github.andrearandazzo.papyrus.ui.detail

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentText
import io.github.andrearandazzo.papyrus.ui.DocumentIntents
import io.github.andrearandazzo.papyrus.ui.components.DateField
import io.github.andrearandazzo.papyrus.ui.components.DocumentPlaceholder
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import io.github.andrearandazzo.papyrus.ui.home.label
import io.github.andrearandazzo.papyrus.ui.rememberPreview
import io.github.andrearandazzo.papyrus.ui.toUtcMillis
import io.github.andrearandazzo.papyrus.ui.utcDay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onBack: () -> Unit,
    viewModel: DocumentDetailViewModel = viewModel(factory = DocumentDetailViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val form = viewModel.form
    val document = viewModel.document
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    val noAppMessage = stringResource(R.string.no_app_to_open)

    // Leave once saved or deleted, or if the document disappeared.
    LaunchedEffect(viewModel.saved, viewModel.deleted, viewModel.notFound) {
        if (viewModel.saved || viewModel.deleted || viewModel.notFound) onBack()
    }

    val open: () -> Unit = {
        scope.launch {
            val file = viewModel.fileForOtherApps() ?: return@launch
            if (!DocumentIntents.open(context, file, document!!.mimeType)) {
                snackbarHostState.showSnackbar(noAppMessage)
            }
        }
    }
    val share: () -> Unit = {
        scope.launch {
            val file = viewModel.fileForOtherApps() ?: return@launch
            DocumentIntents.share(context, file, document!!.mimeType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.document_details), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(R.drawable.ic_arrow_back, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = document != null,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.delete)) }
                    TextButton(onClick = viewModel::save, enabled = form?.isValid == true) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (form != null && document != null) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                PreviewPager(document, onOpen = open)
                DocumentForm(
                    form = form,
                    document = document,
                    categories = categories,
                    onEdit = viewModel::edit,
                    onOpen = open,
                    onShare = share,
                )
                val text by viewModel.text.collectAsStateWithLifecycle()
                TextInside(text)
            }
        }
    }

    if (confirmDelete && document != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { SymbolIcon(R.drawable.ic_delete, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_document_title, document.title)) },
            text = { Text(stringResource(R.string.delete_document_body)) },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

/** Full-width preview; swipe through a PDF's pages. Tap to open the document. */
@Composable
private fun PreviewPager(document: Document, onOpen: () -> Unit) {
    val thumbnails = (LocalContext.current.applicationContext as PapyrusApp).thumbnails
    val pageCount by produceState(1, document.fileName) {
        value = thumbnails.pageCount(document).coerceAtLeast(1)
    }
    val pagerState = rememberPagerState { pageCount }

    Box(
        Modifier
            .fillMaxWidth()
            .height(290.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen),
    ) {
        HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                val image = rememberPreview(document, width = 360.dp, page = page)
                val sheet = RoundedCornerShape(6.dp)
                if (image != null) {
                    Image(
                        image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(image.width.toFloat() / image.height, matchHeightConstraintsFirst = true)
                            .shadow(6.dp, sheet),
                    )
                } else {
                    DocumentPlaceholder(
                        document.kind,
                        large = true,
                        modifier = Modifier.size(width = 180.dp, height = 230.dp).shadow(6.dp, sheet),
                    )
                }
            }
        }
        if (pageCount > 1) {
            Text(
                "${pagerState.currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentForm(
    form: DocumentForm,
    document: Document,
    categories: List<Category>,
    onEdit: ((DocumentForm) -> DocumentForm) -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onOpen, modifier = Modifier.weight(1f).height(48.dp)) {
                SymbolIcon(R.drawable.ic_open_in_new, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.open))
            }
            FilledTonalButton(onClick = onShare, modifier = Modifier.weight(1f).height(48.dp)) {
                SymbolIcon(R.drawable.ic_share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share))
            }
        }

        OutlinedTextField(
            value = form.title,
            onValueChange = { title -> onEdit { it.copy(title = title) } },
            label = { Text(stringResource(R.string.title)) },
            isError = !form.isValid,
            supportingText = if (form.isValid) null else ({ Text(stringResource(R.string.title_required)) }),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        CategoryDropdown(
            categories = categories,
            selectedId = form.categoryId,
            onSelect = { id -> onEdit { it.copy(categoryId = id) } },
        )

        OutlinedTextField(
            value = form.note,
            onValueChange = { note -> onEdit { it.copy(note = note) } },
            label = { Text(stringResource(R.string.note)) },
            minLines = 2,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        DateField(
            label = stringResource(R.string.expiry_date),
            date = form.expiresAt?.let(::utcDay),
            emptyText = stringResource(R.string.no_expiry),
            onDateChange = { date -> onEdit { it.copy(expiresAt = date?.toUtcMillis()) } },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            listOf(
                stringResource(document.kind.label()),
                Formatter.formatShortFileSize(context, document.sizeBytes),
                stringResource(R.string.added_on, formatInstantDate(document.importedAt)),
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/**
 * "Text in the document": what was read from it (PDF text or recognized on the phone), selectable
 * and with a copy button. Long texts show their beginning and expand on request. While it's being
 * read, a short note says so; it fills in by itself.
 */
@Composable
private fun TextInside(text: DocumentText?) {
    val clipboard = LocalClipboardManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.text_inside), Modifier.weight(1f))
            if (!text?.text.isNullOrBlank()) {
                TextButton(onClick = { clipboard.setText(AnnotatedString(text!!.text)) }) {
                    SymbolIcon(R.drawable.ic_copy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.copy))
                }
            }
        }
        when {
            text == null -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.text_reading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            text.text.isBlank() -> Text(
                stringResource(R.string.text_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            else -> {
                val long = text.text.lines().size > COLLAPSED_LINES
                SelectionContainer {
                    Text(
                        text.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (long && !expanded) COLLAPSED_LINES else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                            .padding(12.dp),
                    )
                }
                if (long) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(stringResource(if (expanded) R.string.show_less else R.string.show_all))
                    }
                }
            }
        }
        Text(
            stringResource(R.string.text_inside_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Lines of a long text shown before "Show all". */
private const val COLLAPSED_LINES = 12

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noCategory = stringResource(R.string.no_category)
    val selectedName = categories.find { it.id == selectedId }?.name ?: noCategory

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.category)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text(noCategory) },
                onClick = { onSelect(null); expanded = false },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    leadingIcon = { SymbolIcon(categoryStyle(category).icon, contentDescription = null) },
                    onClick = { onSelect(category.id); expanded = false },
                )
            }
        }
    }
}
