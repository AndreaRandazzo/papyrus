package io.github.andrearandazzo.papyrus.ui.detail

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.ui.formatDay
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import io.github.andrearandazzo.papyrus.ui.home.KindBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onBack: () -> Unit,
    viewModel: DocumentDetailViewModel = viewModel(factory = DocumentDetailViewModel.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val form = viewModel.form
    val document = viewModel.document

    // Leave once saved, or if the document disappeared.
    LaunchedEffect(viewModel.saved, viewModel.notFound) {
        if (viewModel.saved || viewModel.notFound) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.document_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = form?.isValid == true) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        if (form != null && document != null) {
            DocumentForm(
                form = form,
                document = document,
                categories = categories,
                onEdit = viewModel::edit,
                modifier = Modifier.padding(padding),
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
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = form.title,
            onValueChange = { title -> onEdit { it.copy(title = title) } },
            label = { Text(stringResource(R.string.title)) },
            isError = !form.isValid,
            supportingText = if (form.isValid) null else ({ Text(stringResource(R.string.title_required)) }),
            singleLine = true,
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
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.expiry_date), modifier = Modifier.weight(1f))
            TextButton(onClick = { showDatePicker = true }) {
                Text(form.expiresAt?.let(::formatDay) ?: stringResource(R.string.set_date))
            }
            if (form.expiresAt != null) {
                IconButton(onClick = { onEdit { it.copy(expiresAt = null) } }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_date))
                }
            }
        }

        FileInfo(document)
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = form.expiresAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEdit { it.copy(expiresAt = pickerState.selectedDateMillis) }
                    showDatePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        ) {
            DatePicker(pickerState)
        }
    }
}

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
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noCategory) },
                onClick = { onSelect(null); expanded = false },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = { onSelect(category.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun FileInfo(document: Document) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KindBadge(document.kind)
        Column {
            Text(
                Formatter.formatShortFileSize(context, document.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.added_on, formatInstantDate(document.importedAt)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
