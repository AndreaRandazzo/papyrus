package io.github.andrearandazzo.papyrus.ui.expiring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Document
import io.github.andrearandazzo.papyrus.ui.DocumentTile
import io.github.andrearandazzo.papyrus.ui.components.PaperIllustration
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.expiryText
import io.github.andrearandazzo.papyrus.ui.home.label

private enum class Urgency { EXPIRED, SOON, LATER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringScreen(
    onOpenDocument: (Long) -> Unit,
    viewModel: ExpiringViewModel = viewModel(factory = ExpiringViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showInfo by remember { mutableStateOf(false) }
    val groups = state.groups

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_expiring), style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        SymbolIcon(R.drawable.ic_info, contentDescription = stringResource(R.string.expiring_info_title))
                    }
                },
            )
        },
    ) { padding ->
        if (groups.isEmpty) {
            NoExpiryDates(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { AttentionCard(groups.urgentCount) }
            section(R.string.section_expired, groups.expired, Urgency.EXPIRED, state.categoryNames, onOpenDocument)
            section(R.string.section_soon, groups.soon, Urgency.SOON, state.categoryNames, onOpenDocument)
            section(R.string.section_later, groups.later, Urgency.LATER, state.categoryNames, onOpenDocument)
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            icon = { SymbolIcon(R.drawable.ic_event_upcoming, contentDescription = null) },
            title = { Text(stringResource(R.string.expiring_info_title)) },
            text = { Text(stringResource(R.string.expiring_info_body)) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text(stringResource(android.R.string.ok)) }
            },
        )
    }
}

/** "2 need your attention", or a calm "nothing to do" when all is fine. */
@Composable
private fun AttentionCard(urgentCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (urgentCount > 0) R.drawable.ic_event_upcoming else R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                if (urgentCount > 0) pluralStringResource(R.plurals.attention_title, urgentCount, urgentCount)
                else stringResource(R.string.all_clear_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                stringResource(if (urgentCount > 0) R.string.attention_body else R.string.all_clear_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

private fun LazyListScope.section(
    title: Int,
    documents: List<Document>,
    urgency: Urgency,
    categoryNames: Map<Long, String>,
    onOpenDocument: (Long) -> Unit,
) {
    if (documents.isEmpty()) return
    item(key = "header-$urgency") { SectionHeader(title, documents.size, urgency) }
    items(documents, key = { it.id }) { document ->
        ExpiringCard(document, categoryNames[document.categoryId], urgency) { onOpenDocument(document.id) }
    }
}

@Composable
private fun SectionHeader(title: Int, count: Int, urgency: Urgency) {
    val expired = urgency == Urgency.EXPIRED
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp),
    ) {
        Text(
            stringResource(title).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(
                    if (expired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape,
                )
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ExpiringCard(document: Document, categoryName: String?, urgency: Urgency, onClick: () -> Unit) {
    val expired = urgency == Urgency.EXPIRED
    val shape = MaterialTheme.shapes.medium
    val accent = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (expired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .border(1.dp, if (expired) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
        DocumentTile(document, Modifier.size(width = 56.dp, height = 66.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(document.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(categoryName, stringResource(document.kind.label())).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (urgency) {
                    Urgency.EXPIRED -> R.drawable.ic_error
                    Urgency.SOON -> R.drawable.ic_schedule
                    Urgency.LATER -> R.drawable.ic_calendar_today
                }
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    expiryText(document.expiresAt!!),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun NoExpiryDates(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        PaperIllustration(R.drawable.ic_event_upcoming, backColor = MaterialTheme.colorScheme.primaryContainer)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.expiring_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.expiring_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

