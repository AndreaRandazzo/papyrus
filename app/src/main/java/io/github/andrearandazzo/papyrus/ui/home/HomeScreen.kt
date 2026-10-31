package io.github.andrearandazzo.papyrus.ui.home

import android.content.res.Resources
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentFilter
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.data.SortOrder
import io.github.andrearandazzo.papyrus.ui.DocumentIntents
import io.github.andrearandazzo.papyrus.ui.DocumentTile
import io.github.andrearandazzo.papyrus.ui.components.PaperIllustration
import io.github.andrearandazzo.papyrus.ui.components.PrivacyNote
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle
import io.github.andrearandazzo.papyrus.ui.daysUntil
import io.github.andrearandazzo.papyrus.ui.expiryText
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import kotlinx.coroutines.launch
import java.time.LocalDate

/** File types offered in the system file picker. */
private val IMPORTABLE_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/*",
)

/** What a document's "⋮" menu can do. */
private class DocumentActions(
    val open: (Document) -> Unit,
    val share: (Document) -> Unit,
    val edit: (Document) -> Unit,
    val delete: (Document) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Document?>(null) }
    val context = LocalContext.current
    // Follows language changes, unlike context.resources.
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val noAppMessage = stringResource(R.string.no_app_to_open)

    // The system picker needs no storage permission: the user chooses exactly what we can read.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris)
    }
    val addDocuments = { picker.launch(IMPORTABLE_TYPES) }

    val actions = DocumentActions(
        open = { document ->
            scope.launch {
                val file = viewModel.fileForOtherApps(document)
                if (!DocumentIntents.open(context, file, document.mimeType)) snackbarHostState.showSnackbar(noAppMessage)
            }
        },
        share = { document ->
            scope.launch { DocumentIntents.share(context, viewModel.fileForOtherApps(document), document.mimeType) }
        },
        edit = { onOpenDocument(it.id) },
        delete = { pendingDelete = it },
    )

    LaunchedEffect(viewModel) {
        viewModel.importResults.collect { result ->
            snackbarHostState.showSnackbar(result.text(resources))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (!state.libraryEmpty) {
                        IconButton(onClick = viewModel::toggleView) {
                            if (viewModel.gridView) {
                                SymbolIcon(R.drawable.ic_view_agenda, contentDescription = stringResource(R.string.list_view))
                            } else {
                                SymbolIcon(R.drawable.ic_grid_view, contentDescription = stringResource(R.string.grid_view))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            if (!state.libraryEmpty) {
                FloatingActionButton(onClick = addDocuments, shape = MaterialTheme.shapes.medium) {
                    SymbolIcon(R.drawable.ic_add, contentDescription = stringResource(R.string.import_documents))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.libraryEmpty) {
                FirstLaunch(onAdd = addDocuments, onRestore = onOpenBackup, busy = importing)
                return@Column
            }
            SearchField(viewModel.query, viewModel::search)
            FilterRow(
                categories = state.categories,
                filter = state.filter,
                onSelectCategory = viewModel::selectCategory,
                onOpenFilters = { showFilters = true },
            )
            when {
                state.documents.isNotEmpty() -> {
                    ResultsHeader(
                        count = state.documents.size,
                        sort = state.filter.sort,
                        onSort = { order -> viewModel.updateFilter { it.copy(sort = order) } },
                    )
                    val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
                    if (viewModel.gridView) {
                        DocumentGrid(state.documents, categoryNames, actions)
                    } else {
                        DocumentList(state.documents, categoryNames, actions)
                    }
                }
                // Only a category is selected: suggest adding documents to it.
                state.filter.categoryId != null && !state.filter.copy(categoryId = null).isFiltering -> EmptyState(
                    illustration = R.drawable.ic_duo_folder,
                    title = stringResource(R.string.empty_category_title),
                    body = stringResource(R.string.empty_category_body),
                    actionIcon = R.drawable.ic_add,
                    actionLabel = stringResource(R.string.import_documents),
                    onAction = addDocuments,
                )
                else -> EmptyState(
                    illustration = R.drawable.ic_duo_search,
                    title = stringResource(R.string.no_results_title),
                    body = stringResource(R.string.no_results_body),
                    actionIcon = R.drawable.ic_filter_alt_off,
                    actionLabel = stringResource(R.string.clear_filters),
                    onAction = viewModel::clearFilters,
                )
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            filter = state.filter,
            resultCount = state.documents.size,
            onChange = viewModel::updateFilter,
            onDismiss = { showFilters = false },
        )
    }

    pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { SymbolIcon(R.drawable.ic_delete, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_document_title, document.title)) },
            text = { Text(stringResource(R.string.delete_document_body)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.delete(document)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MenuItem(icon: Int, text: String, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) },
        leadingIcon = { SymbolIcon(icon, contentDescription = null) },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.width(240.dp),
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { SymbolIcon(R.drawable.ic_search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    SymbolIcon(R.drawable.ic_close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Results update while typing, so the search key just closes the keyboard.
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** "Filters" chip with a count of active filters, then "All" and one chip per category. */
@Composable
private fun FilterRow(
    categories: List<Category>,
    filter: DocumentFilter,
    onSelectCategory: (Category?) -> Unit,
    onOpenFilters: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val count = filter.panelFilterCount
            FilterChip(
                selected = count > 0,
                onClick = onOpenFilters,
                leadingIcon = { SymbolIcon(R.drawable.ic_tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = {
                    Text(stringResource(R.string.filters))
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                count.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                },
                modifier = Modifier.height(40.dp),
            )
        }
        item {
            CategoryChip(stringResource(R.string.filter_all), icon = null, filter.categoryId == null) {
                onSelectCategory(null)
            }
        }
        items(categories, key = { it.id }) { category ->
            CategoryChip(category.name, categoryStyle(category).icon, category.id == filter.categoryId) {
                onSelectCategory(category)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, icon: Int?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon = icon?.let { { SymbolIcon(it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
        label = { Text(label) },
        modifier = Modifier.height(40.dp),
    )
}

/** "6 DOCUMENTS" on the left, the sort order as a small menu on the right. */
@Composable
private fun ResultsHeader(count: Int, sort: SortOrder, onSort: (SortOrder) -> Unit) {
    var showSort by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp),
    ) {
        SectionLabel(pluralStringResource(R.plurals.document_count, count, count), Modifier.weight(1f))
        Box {
            TextButton(onClick = { showSort = true }) {
                Text(stringResource(sort.headerLabel()))
                SymbolIcon(R.drawable.ic_arrow_drop_down, contentDescription = null)
            }
            DropdownMenu(
                expanded = showSort,
                onDismissRequest = { showSort = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(stringResource(order.headerLabel())) },
                        trailingIcon = {
                            if (order == sort) SymbolIcon(R.drawable.ic_check, contentDescription = null)
                        },
                        onClick = { showSort = false; onSort(order) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentList(documents: List<Document>, categoryNames: Map<Long, String>, actions: DocumentActions) {
    // Bottom padding keeps the last row clear of the "+" button.
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(documents, key = { it.id }) { document ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { actions.edit(document) }
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            ) {
                DocumentTile(document, Modifier.size(width = 64.dp, height = 76.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DocumentText(document, categoryNames[document.categoryId])
                }
                DocumentMenu(document, actions)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DocumentGrid(documents: List<Document>, categoryNames: Map<Long, String>, actions: DocumentActions) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(documents, key = { it.id }) { document ->
            // Same corner shape as the preview tile, so the press highlight doesn't cut its border.
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { actions.edit(document) },
            ) {
                DocumentTile(document, Modifier.fillMaxWidth().aspectRatio(0.8f), previewWidth = 200.dp)
                // Inset so text near the bottom corners isn't clipped by the card's rounded shape.
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)) {
                    Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DocumentText(document, categoryNames[document.categoryId], compact = true)
                    }
                    DocumentMenu(document, actions)
                }
            }
        }
    }
}

/** Title, "Identity · PDF · 2.4 MB · 8 Mar 2024", and the expiry if set. */
@Composable
private fun DocumentText(document: Document, categoryName: String?, compact: Boolean = false) {
    val context = LocalContext.current
    Text(
        document.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    val details = if (compact) {
        listOfNotNull(categoryName, stringResource(document.kind.label()))
    } else {
        listOfNotNull(
            categoryName,
            stringResource(document.kind.label()),
            Formatter.formatShortFileSize(context, document.sizeBytes),
            formatInstantDate(document.importedAt),
        )
    }
    Text(
        details.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    document.expiresAt?.let { expiry ->
        val expired = daysUntil(expiry) < 0
        val color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Icon(
                painterResource(if (expired) R.drawable.ic_error else R.drawable.ic_calendar_today),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                expiryText(expiry),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DocumentMenu(document: Document, actions: DocumentActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            SymbolIcon(R.drawable.ic_more_vert, contentDescription = stringResource(R.string.document_actions))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            MenuItem(R.drawable.ic_open_in_new, stringResource(R.string.open)) { expanded = false; actions.open(document) }
            MenuItem(R.drawable.ic_share, stringResource(R.string.share)) { expanded = false; actions.share(document) }
            MenuItem(R.drawable.ic_edit, stringResource(R.string.edit_details)) { expanded = false; actions.edit(document) }
            MenuItem(R.drawable.ic_delete, stringResource(R.string.delete)) { expanded = false; actions.delete(document) }
        }
    }
}

@Composable
private fun FirstLaunch(onAdd: () -> Unit, onRestore: () -> Unit, busy: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        PaperIllustration(R.drawable.ic_duo_document, backColor = MaterialTheme.colorScheme.primaryContainer)
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onAdd, enabled = !busy, modifier = Modifier.height(52.dp)) {
            SymbolIcon(R.drawable.ic_add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_first_document))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestore, enabled = !busy) {
            SymbolIcon(R.drawable.ic_history, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.backup_restore_from))
        }
        Spacer(Modifier.weight(1.4f))
        PrivacyNote(stringResource(R.string.privacy_note))
    }
}

@Composable
private fun EmptyState(
    illustration: Int,
    title: String,
    body: String,
    actionIcon: Int,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        PaperIllustration(illustration, backColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onAction, modifier = Modifier.height(48.dp)) {
            SymbolIcon(actionIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel)
        }
    }
}

fun DocumentKind.label() = when (this) {
    DocumentKind.PDF -> R.string.kind_pdf
    DocumentKind.WORD -> R.string.kind_word
    DocumentKind.IMAGE -> R.string.kind_image
    DocumentKind.OTHER -> R.string.kind_other
}

private fun SortOrder.headerLabel() = when (this) {
    SortOrder.NEWEST -> R.string.sort_recently_added
    SortOrder.OLDEST -> R.string.sort_oldest
    SortOrder.TITLE -> R.string.sort_title
    SortOrder.EXPIRY -> R.string.sort_expiry
}

private fun ImportResult.text(resources: Resources): String =
    if (failed == 0) resources.getQuantityString(R.plurals.import_done, imported, imported)
    else resources.getQuantityString(R.plurals.import_failed, failed, failed)
