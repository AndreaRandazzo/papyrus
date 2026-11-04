package io.github.andrearandazzo.papyrus.ui.home

import android.content.res.Resources
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
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
import io.github.andrearandazzo.papyrus.data.ImportResult
import io.github.andrearandazzo.papyrus.data.LibraryView
import io.github.andrearandazzo.papyrus.data.SortField
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
import java.time.LocalDate
import kotlinx.coroutines.launch

/** What a document's "⋮" menu can do, and the empty states' buttons. */
private class DocumentActions(
    val open: (Document) -> Unit,
    val share: (Document) -> Unit,
    val edit: (Document) -> Unit,
    val delete: (Document) -> Unit,
    val add: () -> Unit,
    val clearFilters: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    /** Opens the scanner; new scans go into the given category. */
    onScan: (categoryId: Long?) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Document?>(null) }
    // Originals of the last import the user asked to delete, waiting for confirmation.
    var pendingOriginals by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    // Follows language changes, unlike context.resources.
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val noAppMessage = stringResource(R.string.no_app_to_open)

    // The system picker needs no storage permission: the user chooses exactly what we can read.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.import(uris)
    }
    val pickFiles = { picker.launch(DocumentIntents.IMPORTABLE_TYPES) }
    // "+" offers both ways of adding: scanning paper or picking files.
    val addDocuments = { showAddMenu = true }

    val inFolders = viewModel.view == LibraryView.FOLDERS
    // Inside a folder of the Folders view; back returns to the folder overview.
    val openFolder = if (inFolders) state.openFolder else null
    BackHandler(enabled = openFolder != null) { viewModel.openFolder(null) }

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
        add = addDocuments,
        clearFilters = viewModel::clearFilters,
    )

    LaunchedEffect(viewModel) {
        launch {
            viewModel.originalsDeleted.collect { (deleted, asked) ->
                snackbarHostState.showSnackbar(
                    if (deleted == asked) resources.getQuantityString(R.plurals.originals_deleted, deleted, deleted)
                    else resources.getQuantityString(R.plurals.originals_not_deleted, asked - deleted, asked - deleted),
                )
            }
        }
        viewModel.importResults.collect { result ->
            if (result.failed > 0 || result.deletableOriginals.isEmpty()) {
                snackbarHostState.showSnackbar(result.text(resources))
            } else {
                // Imports are copies: offer to free the space the originals still take.
                val choice = snackbarHostState.showSnackbar(
                    message = resources.getQuantityString(R.plurals.import_done_originals, result.imported, result.imported),
                    actionLabel = resources.getString(R.string.delete_originals),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (choice == SnackbarResult.ActionPerformed) pendingOriginals = result.deletableOriginals
            }
        }
    }

    Scaffold(
        topBar = {
            if (openFolder != null) {
                FolderTopBar(openFolder, onBack = { viewModel.openFolder(null) })
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            SymbolIcon(R.drawable.ic_settings, contentDescription = stringResource(R.string.settings))
                        }
                        if (!state.libraryEmpty) LibraryViewSwitch(viewModel.view, viewModel::showView)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
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
            // One search field for every view, kept outside the branches below: if each branch had its
            // own, typing the first letter in Folders would swap in a new field and close the keyboard.
            if (openFolder == null) SearchField(viewModel.query, viewModel::search)
            when {
                openFolder != null -> {
                    FolderToolbar(
                        filterCount = state.filter.panelFilterCount,
                        sort = state.filter.sort,
                        onOpenFilters = { showFilters = true },
                        onSort = { order -> viewModel.updateFilter { it.copy(sort = order) } },
                    )
                    Documents(state, grid = false, actions, emptyCategory = openFolder.documentCount == 0)
                }
                // The folder overview; typing a search shows matching documents from every folder.
                inFolders && viewModel.query.isBlank() -> FolderGrid(state.folders, onOpen = viewModel::openFolder)
                else -> {
                    if (!inFolders) {
                        FilterRow(
                            categories = state.categories,
                            filter = state.filter,
                            onSelectCategory = viewModel::selectCategory,
                            onOpenFilters = { showFilters = true },
                        )
                    }
                    ResultsHeader(
                        count = state.documents.size,
                        sort = state.filter.sort,
                        onSort = { order -> viewModel.updateFilter { it.copy(sort = order) } },
                    )
                    // Only a category is selected: suggest adding documents to it.
                    val emptyCategory = state.filter.categoryId != null && !state.filter.copy(categoryId = null).isFiltering
                    Documents(state, grid = viewModel.view == LibraryView.GRID, actions, emptyCategory)
                }
            }
        }
    }

    if (showAddMenu) {
        AddDocumentSheet(
            onScan = { showAddMenu = false; onScan(viewModel.targetCategoryId) },
            onPickFiles = { showAddMenu = false; pickFiles() },
            onDismiss = { showAddMenu = false },
        )
    }

    if (showFilters) {
        FiltersSheet(
            filter = state.filter,
            resultCount = state.documents.size,
            onChange = viewModel::updateFilter,
            onDismiss = { showFilters = false },
        )
    }

    if (pendingOriginals.isNotEmpty()) {
        val count = pendingOriginals.size
        AlertDialog(
            onDismissRequest = { pendingOriginals = emptyList() },
            icon = { SymbolIcon(R.drawable.ic_delete, contentDescription = null) },
            title = { Text(pluralStringResource(R.plurals.delete_originals_title, count, count)) },
            text = { Text(stringResource(R.string.delete_originals_body)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteOriginals(pendingOriginals)
                    pendingOriginals = emptyList()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingOriginals = emptyList() }) { Text(stringResource(android.R.string.cancel)) }
            },
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
        item { FiltersChip(filter.panelFilterCount, onOpenFilters) }
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

/** "Filters", with the number of filters set in the filters panel. */
@Composable
private fun FiltersChip(count: Int, onClick: () -> Unit) {
    FilterChip(
        selected = count > 0,
        onClick = onClick,
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

/**
 * The documents matching the filter as a list or grid, or an empty state: [emptyCategory] means
 * only a category (or folder) is selected and it has nothing yet, so the button adds documents.
 */
@Composable
private fun Documents(state: HomeState, grid: Boolean, actions: DocumentActions, emptyCategory: Boolean) {
    when {
        state.documents.isNotEmpty() -> {
            val categoryNames = remember(state.categories) { state.categories.associate { it.id to it.name } }
            // A new sort order starts again from the top instead of following the visible item.
            key(state.filter.sort) {
                if (grid) {
                    DocumentGrid(state.documents, categoryNames, state.snippets, state.attachmentCounts, actions)
                } else {
                    DocumentList(state.documents, categoryNames, state.snippets, state.attachmentCounts, actions)
                }
            }
        }
        emptyCategory -> EmptyState(
            illustration = R.drawable.ic_duo_folder,
            title = stringResource(R.string.empty_category_title),
            body = stringResource(R.string.empty_category_body),
            actionIcon = R.drawable.ic_add,
            actionLabel = stringResource(R.string.import_documents),
            onAction = actions.add,
        )
        else -> EmptyState(
            illustration = R.drawable.ic_duo_search,
            title = stringResource(R.string.no_results_title),
            body = stringResource(R.string.no_results_body),
            actionIcon = R.drawable.ic_filter_alt_off,
            actionLabel = stringResource(R.string.clear_filters),
            onAction = actions.clearFilters,
        )
    }
}

/** "6 DOCUMENTS" on the left, the sort controls on the right. Hidden when nothing matches. */
@Composable
private fun ResultsHeader(count: Int, sort: SortOrder, onSort: (SortOrder) -> Unit) {
    if (count == 0) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 8.dp),
    ) {
        SectionLabel(pluralStringResource(R.plurals.document_count, count, count), Modifier.weight(1f))
        SortControls(sort, onSort)
    }
}

/** Inside a folder: the Filters chip on the left, the sort controls on the right. */
@Composable
private fun FolderToolbar(filterCount: Int, sort: SortOrder, onOpenFilters: () -> Unit, onSort: (SortOrder) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        FiltersChip(filterCount, onOpenFilters)
        Spacer(Modifier.weight(1f))
        SortControls(sort, onSort)
    }
}

/** The sort order as a small menu of fields, and an arrow that reverses it. */
@Composable
private fun SortControls(sort: SortOrder, onSort: (SortOrder) -> Unit) {
    var showSort by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { showSort = true }) {
                Text(stringResource(sort.label()))
                SymbolIcon(R.drawable.ic_arrow_drop_down, contentDescription = null)
            }
            DropdownMenu(
                expanded = showSort,
                onDismissRequest = { showSort = false },
                shape = MaterialTheme.shapes.medium,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                SortField.entries.forEach { field ->
                    DropdownMenuItem(
                        text = { Text(stringResource(field.label())) },
                        trailingIcon = {
                            if (field == sort.field) SymbolIcon(R.drawable.ic_check, contentDescription = null)
                        },
                        onClick = {
                            showSort = false
                            if (field != sort.field) onSort(SortOrder(field))
                        },
                    )
                }
            }
        }
        IconButton(onClick = { onSort(sort.reversed()) }) {
            SymbolIcon(
                if (sort.ascending) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down,
                contentDescription = stringResource(R.string.sort_reverse),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DocumentList(
    documents: List<Document>,
    categoryNames: Map<Long, String>,
    snippets: Map<Long, String>,
    attachmentCounts: Map<Long, Int>,
    actions: DocumentActions,
) {
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
                    DocumentText(document, categoryNames[document.categoryId], snippets[document.id], attachmentCounts[document.id] ?: 0)
                }
                DocumentMenu(document, actions)
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun DocumentGrid(
    documents: List<Document>,
    categoryNames: Map<Long, String>,
    snippets: Map<Long, String>,
    attachmentCounts: Map<Long, Int>,
    actions: DocumentActions,
) {
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
                        DocumentText(document, categoryNames[document.categoryId], snippets[document.id], attachmentCounts[document.id] ?: 0, compact = true)
                    }
                    DocumentMenu(document, actions)
                }
            }
        }
    }
}

/**
 * Title, "Identity · PDF · 2.4 MB · 8 Mar 2024", the expiry if set, and for a search found inside
 * the document, the words around the match.
 */
@Composable
private fun DocumentText(document: Document, categoryName: String?, snippet: String?, attachments: Int, compact: Boolean = false) {
    val context = LocalContext.current
    Text(
        document.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    val attached = if (attachments > 0) pluralStringResource(R.plurals.attachment_count, attachments, attachments) else null
    val details = if (compact) {
        listOfNotNull(categoryName, stringResource(document.kind.label()), attached)
    } else {
        listOfNotNull(
            categoryName,
            stringResource(document.kind.label()),
            attached,
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
    snippet?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

/** "+" menu: scan a paper document with the camera, or pick existing files. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentSheet(onScan: () -> Unit, onPickFiles: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            AddOption(R.drawable.ic_duo_camera, stringResource(R.string.add_scan), stringResource(R.string.add_scan_body), onScan)
            AddOption(R.drawable.ic_file_plus, stringResource(R.string.add_files), stringResource(R.string.add_files_body), onPickFiles)
        }
    }
}

@Composable
private fun AddOption(icon: Int, title: String, body: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(
            Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) { SymbolIcon(icon, contentDescription = null) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(
    illustration: Int,
    title: String,
    body: String,
    actionIcon: Int,
    actionLabel: String,
    onAction: () -> Unit,
) {
    // Often shown while typing a search. With the keyboard open it stays above it, without the
    // picture, so the message and the button stay visible; it can scroll if that's still too much.
    val keyboardOpen = WindowInsets.isImeVisible
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = if (keyboardOpen) 16.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!keyboardOpen) {
            Spacer(Modifier.height(16.dp))
            PaperIllustration(illustration, backColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            Spacer(Modifier.height(28.dp))
        }
        EmptyStateText(title, body, actionIcon, actionLabel, onAction)
    }
}

@Composable
private fun EmptyStateText(title: String, body: String, actionIcon: Int, actionLabel: String, onAction: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

private fun ImportResult.text(resources: Resources): String =
    if (failed == 0) resources.getQuantityString(R.plurals.import_done, imported, imported)
    else resources.getQuantityString(R.plurals.import_failed, failed, failed)
