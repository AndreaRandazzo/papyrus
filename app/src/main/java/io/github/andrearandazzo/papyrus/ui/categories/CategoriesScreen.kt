package io.github.andrearandazzo.papyrus.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle
import io.github.andrearandazzo.papyrus.ui.components.CategoryIcon
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch

/** Which dialog is open, if any. */
private sealed interface CategoryDialog {
    data object Add : CategoryDialog
    data class Rename(val category: Category) : CategoryDialog
    data class Delete(val category: Category) : CategoryDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory),
) {
    val rows by viewModel.categories.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<CategoryDialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        SymbolIcon(R.drawable.ic_arrow_back, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { dialog = CategoryDialog.Add },
                shape = MaterialTheme.shapes.medium,
                icon = { SymbolIcon(R.drawable.ic_add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_category_button)) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.categories_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(rows, key = { it.category.id }) { row ->
                CategoryItem(
                    row = row,
                    onRename = { dialog = CategoryDialog.Rename(row.category) },
                    onDelete = { dialog = CategoryDialog.Delete(row.category) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        CategoryDialog.Add -> CategoryEditDialog(
            title = stringResource(R.string.add_category),
            confirmLabel = stringResource(R.string.add),
            initialName = "",
            initialIcon = null,
            onSave = viewModel::add,
            onDismiss = { dialog = null },
        )
        is CategoryDialog.Rename -> CategoryEditDialog(
            title = stringResource(R.string.edit_category),
            confirmLabel = stringResource(R.string.save),
            initialName = current.category.name,
            initialIcon = current.category.icon,
            onSave = { name, icon -> viewModel.update(current.category, name, icon) },
            onDismiss = { dialog = null },
        )
        is CategoryDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { SymbolIcon(R.drawable.ic_delete, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_category_title, current.category.name)) },
            text = { Text(stringResource(R.string.delete_category_body)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.delete(current.category)
                    dialog = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun CategoryItem(row: CategoryRow, onRename: () -> Unit, onDelete: () -> Unit) {
    val style = categoryStyle(row.category)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(style.tint.copy(alpha = 0.3f), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(style.icon), contentDescription = null, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(row.category.name, style = MaterialTheme.typography.titleMedium)
            Text(
                pluralStringResource(R.plurals.category_document_count, row.documentCount, row.documentCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename) {
            SymbolIcon(R.drawable.ic_edit, contentDescription = stringResource(R.string.rename))
        }
        IconButton(onClick = onDelete) {
            SymbolIcon(R.drawable.ic_delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

/**
 * Asks for a category name and icon; stays open with an error if the name is taken.
 * Until the user taps an icon, the one suggested by the name is shown and nothing is saved,
 * so the category keeps following its name.
 */
@Composable
private fun CategoryEditDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialIcon: String?,
    onSave: suspend (name: String, icon: String?) -> Boolean,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var picked by remember { mutableStateOf(CategoryIcon.fromKey(initialIcon)) }
    var nameTaken by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val shown = picked ?: CategoryIcon.suggestedFor(name)
    val preview = categoryStyle(name, picked?.key)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier.size(48.dp).background(preview.tint.copy(alpha = 0.3f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(preview.icon), contentDescription = null)
            }
        },
        title = { Text(title, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.category_name_hint))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameTaken = false },
                    label = { Text(stringResource(R.string.name)) },
                    isError = nameTaken,
                    supportingText = if (nameTaken) ({ Text(stringResource(R.string.category_exists)) }) else null,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                SectionLabel(stringResource(R.string.category_icon))
                IconPicker(selected = shown, onSelect = { picked = it })
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    scope.launch {
                        if (onSave(name, picked?.key)) onDismiss() else nameTaken = true
                    }
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun IconPicker(selected: CategoryIcon?, onSelect: (CategoryIcon) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CategoryIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            val shape = MaterialTheme.shapes.small
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(if (isSelected) icon.tint.copy(alpha = 0.35f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = shape,
                    )
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(icon) }),
            ) {
                Icon(painterResource(icon.drawable), contentDescription = stringResource(icon.label))
            }
        }
    }
}
