package io.github.andrearandazzo.papyrus.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.DocumentFilter
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.data.SortOrder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Type, date added and sort order. Changes apply immediately. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(
    filter: DocumentFilter,
    onChange: ((DocumentFilter) -> DocumentFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var showDateRange by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.filter_type))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DocumentKind.entries.forEach { kind ->
                    FilterChip(
                        selected = kind in filter.kinds,
                        onClick = {
                            onChange { it.copy(kinds = if (kind in it.kinds) it.kinds - kind else it.kinds + kind) }
                        },
                        label = { Text(stringResource(kind.labelRes())) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.filter_added))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDateRange = true }, modifier = Modifier.weight(1f)) {
                    Text(dateRangeLabel(filter.addedFrom, filter.addedTo), modifier = Modifier.fillMaxWidth())
                }
                if (filter.addedFrom != null || filter.addedTo != null) {
                    IconButton(onClick = { onChange { it.copy(addedFrom = null, addedTo = null) } }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_date))
                    }
                }
            }

            SectionTitle(stringResource(R.string.sort_by))
            SortOrder.entries.forEach { order ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = filter.sort == order,
                            role = Role.RadioButton,
                            onClick = { onChange { it.copy(sort = order) } },
                        ),
                ) {
                    RadioButton(selected = filter.sort == order, onClick = null)
                    Text(stringResource(order.labelRes()), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }

    if (showDateRange) {
        DateRangeDialog(
            from = filter.addedFrom,
            to = filter.addedTo,
            onConfirm = { from, to -> onChange { it.copy(addedFrom = from, addedTo = to) } },
            onDismiss = { showDateRange = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    from: LocalDate?,
    to: LocalDate?,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    // The picker works with UTC midnight milliseconds.
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = from?.toUtcMillis(),
        initialSelectedEndDateMillis = to?.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val start = state.selectedStartDateMillis?.toUtcDate()
                    // A single tapped day means "just that day".
                    onConfirm(start, state.selectedEndDateMillis?.toUtcDate() ?: start)
                    onDismiss()
                },
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    ) {
        DateRangePicker(state, modifier = Modifier.height(500.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun dateRangeLabel(from: LocalDate?, to: LocalDate?): String {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    return when {
        from == null && to == null -> stringResource(R.string.any_date)
        from == to -> from!!.format(format)
        else -> "${from?.format(format) ?: "…"} – ${to?.format(format) ?: "…"}"
    }
}

private fun DocumentKind.labelRes() = when (this) {
    DocumentKind.PDF -> R.string.type_pdf
    DocumentKind.WORD -> R.string.type_word
    DocumentKind.IMAGE -> R.string.type_image
    DocumentKind.OTHER -> R.string.type_other
}

private fun SortOrder.labelRes() = when (this) {
    SortOrder.NEWEST -> R.string.sort_newest
    SortOrder.OLDEST -> R.string.sort_oldest
    SortOrder.TITLE -> R.string.sort_title
    SortOrder.EXPIRY -> R.string.sort_expiry
}

private fun LocalDate.toUtcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
