package io.github.andrearandazzo.papyrus.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.ui.formatDate
import io.github.andrearandazzo.papyrus.ui.toUtcMillis
import io.github.andrearandazzo.papyrus.ui.utcDay
import java.time.LocalDate

/**
 * A read-only outlined field showing a date; tapping it opens a date picker.
 * The picker offers "Remove" when a date is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    emptyText: String,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    // A read-only text field swallows clicks, so watch its presses instead.
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(interactions) {
        interactions.interactions.collect { if (it is PressInteraction.Release) showPicker = true }
    }

    OutlinedTextField(
        value = date?.let(::formatDate) ?: emptyText,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        trailingIcon = { SymbolIcon(R.drawable.ic_calendar_month, contentDescription = null) },
        interactionSource = interactions,
        modifier = modifier,
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date?.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        onDateChange(state.selectedDateMillis?.let(::utcDay))
                        showPicker = false
                    },
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                if (date != null) {
                    TextButton(onClick = {
                        onDateChange(null)
                        showPicker = false
                    }) { Text(stringResource(R.string.remove)) }
                }
                TextButton(onClick = { showPicker = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        ) {
            DatePicker(state)
        }
    }
}
