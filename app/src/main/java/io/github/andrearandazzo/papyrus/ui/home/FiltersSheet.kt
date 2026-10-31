package io.github.andrearandazzo.papyrus.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.DocumentFilter
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.data.SortOrder
import io.github.andrearandazzo.papyrus.ui.components.DateField
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon

/** Type, date added and sort order. Changes apply immediately; the button just closes the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    filter: DocumentFilter,
    resultCount: Int,
    onChange: ((DocumentFilter) -> DocumentFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.filters_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    onChange { it.copy(kinds = emptySet(), addedFrom = null, addedTo = null, sort = SortOrder.NEWEST) }
                }) { Text(stringResource(R.string.reset)) }
            }

            SectionLabel(stringResource(R.string.filter_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DocumentKind.entries.forEach { kind ->
                    val selected = kind in filter.kinds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onChange { it.copy(kinds = if (kind in it.kinds) it.kinds - kind else it.kinds + kind) }
                        },
                        leadingIcon = if (selected) {
                            { SymbolIcon(R.drawable.ic_check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            null
                        },
                        label = { Text(stringResource(kind.filterLabel())) },
                        modifier = Modifier.height(40.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.filter_added))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = stringResource(R.string.date_from),
                    date = filter.addedFrom,
                    emptyText = stringResource(R.string.date_any),
                    onDateChange = { date -> onChange { it.copy(addedFrom = date) } },
                    modifier = Modifier.weight(1f),
                )
                DateField(
                    label = stringResource(R.string.date_to),
                    date = filter.addedTo,
                    emptyText = stringResource(R.string.date_today),
                    onDateChange = { date -> onChange { it.copy(addedTo = date) } },
                    modifier = Modifier.weight(1f),
                )
            }

            SectionLabel(stringResource(R.string.sort_by))
            Column {
                SortOrder.entries.forEach { order -> SortOption(order, selected = filter.sort == order) {
                    onChange { it.copy(sort = order) }
                } }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                SymbolIcon(R.drawable.ic_check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(pluralStringResource(R.plurals.show_results, resultCount, resultCount))
            }
        }
    }
}

@Composable
private fun SortOption(order: SortOrder, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(Modifier.width(32.dp)) {
            if (selected) SymbolIcon(R.drawable.ic_check, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(
            stringResource(order.sheetLabel()),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun DocumentKind.filterLabel() = when (this) {
    DocumentKind.PDF -> R.string.type_pdf
    DocumentKind.WORD -> R.string.type_word
    DocumentKind.IMAGE -> R.string.type_image
    DocumentKind.OTHER -> R.string.type_other
}

private fun SortOrder.sheetLabel() = when (this) {
    SortOrder.NEWEST -> R.string.sort_newest
    SortOrder.OLDEST -> R.string.sort_oldest
    SortOrder.TITLE -> R.string.sort_title
    SortOrder.EXPIRY -> R.string.sort_expiry
}
