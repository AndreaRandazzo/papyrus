package io.github.andrearandazzo.papyrus.ui.home

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import io.github.andrearandazzo.papyrus.data.DocumentFilter
import io.github.andrearandazzo.papyrus.ui.DocumentThumbnail
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.ui.formatDay
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import io.github.andrearandazzo.papyrus.ui.isPast

/** File types offered in the system file picker. */
private val IMPORTABLE_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/*",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDocument: (Long) -> Unit,
    onManageCategories: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by rememberSaveable { mutableStateOf(false) }
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onManageCategories) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(R.string.manage_categories),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(IMPORTABLE_TYPES) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.import_documents))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.libraryEmpty) {
                EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body))
            } else {
                SearchField(viewModel.query, viewModel::search)
                FilterRow(
                    categories = state.categories,
                    filter = state.filter,
                    onSelectCategory = viewModel::selectCategory,
                    onOpenFilters = { showFilters = true },
                )
                when {
                    state.documents.isNotEmpty() -> DocumentList(state.documents, state.categories, onOpenDocument)
                    // Only a category is selected: suggest adding documents to it.
                    state.filter.categoryId != null && state.filter.copy(categoryId = null).isFiltering.not() ->
                        EmptyState(
                            stringResource(R.string.empty_category_title),
                            stringResource(R.string.empty_category_body),
                        )
                    else -> EmptyState(
                        stringResource(R.string.no_results_title),
                        stringResource(R.string.no_results_body),
                        action = {
                            TextButton(onClick = viewModel::clearFilters) {
                                Text(stringResource(R.string.clear_filters))
                            }
                        },
                    )
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            filter = state.filter,
            onChange = viewModel::updateFilter,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Results update while typing, so the search key just closes the keyboard.
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** "Filters" chip, then "All" and one chip per category. */
@Composable
private fun FilterRow(
    categories: List<Category>,
    filter: DocumentFilter,
    onSelectCategory: (Category?) -> Unit,
    onOpenFilters: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val count = filter.panelFilterCount
            FilterChip(
                selected = count > 0,
                onClick = onOpenFilters,
                label = {
                    Text(
                        if (count > 0) stringResource(R.string.filters_count, count)
                        else stringResource(R.string.filters),
                    )
                },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            )
        }
        item {
            FilterChip(
                selected = filter.categoryId == null,
                onClick = { onSelectCategory(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = category.id == filter.categoryId,
                onClick = { onSelectCategory(category) },
                label = { Text(category.name) },
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        action()
    }
}

@Composable
private fun DocumentList(
    documents: List<Document>,
    categories: List<Category>,
    onOpenDocument: (Long) -> Unit,
) {
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    // Bottom padding keeps the last row clear of the "+" button.
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(documents, key = { it.id }) { document ->
            DocumentRow(document, categoryNames[document.categoryId], onClick = { onOpenDocument(document.id) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun DocumentRow(document: Document, categoryName: String?, onClick: () -> Unit) {
    val context = LocalContext.current
    val details = listOfNotNull(
        categoryName,
        Formatter.formatShortFileSize(context, document.sizeBytes),
        formatInstantDate(document.importedAt),
    ).joinToString(" · ")
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(shape)
                    // Outline so a white page stands out from the background.
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
            ) {
                DocumentThumbnail(
                    document = document,
                    width = 44.dp,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    placeholder = { KindBadge(document.kind) },
                )
            }
        },
        headlineContent = { Text(document.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(details, maxLines = 1, overflow = TextOverflow.Ellipsis)
                document.expiresAt?.let { expiry ->
                    val expired = isPast(expiry)
                    Text(
                        text = stringResource(
                            if (expired) R.string.expired_on else R.string.expires_on,
                            formatDay(expiry),
                        ),
                        color = if (expired) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        },
    )
}

@Composable
fun KindBadge(kind: DocumentKind) {
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
