package io.github.andrearandazzo.papyrus.ui.detail

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Attachment
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import io.github.andrearandazzo.papyrus.ui.home.label

/**
 * "Attachments": the document's extra files, each opened with a tap, shared or removed from its
 * menu; then buttons to add attachments and to replace the document's own file.
 */
@Composable
fun AttachmentsSection(
    attachments: List<Attachment>,
    working: Boolean,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
    onOpen: (Attachment) -> Unit,
    onShare: (Attachment) -> Unit,
    onRemove: (Attachment) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionLabel(
            if (attachments.isEmpty()) stringResource(R.string.attachments)
            else "${stringResource(R.string.attachments)} (${attachments.size})",
            Modifier.padding(bottom = 4.dp),
        )
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
        if (attachments.isEmpty()) {
            Text(
                stringResource(R.string.attachments_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        attachments.forEachIndexed { index, attachment ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AttachmentRow(attachment, onOpen = { onOpen(attachment) }, onShare = { onShare(attachment) }, onRemove = { onRemove(attachment) })
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAdd, enabled = !working, modifier = Modifier.height(44.dp)) {
                SymbolIcon(R.drawable.ic_attachment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.attachments_add))
            }
            OutlinedButton(onClick = onReplace, enabled = !working, modifier = Modifier.height(44.dp)) {
                SymbolIcon(R.drawable.ic_replace, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.replace_file))
            }
        }
    }
}

@Composable
private fun AttachmentRow(attachment: Attachment, onOpen: () -> Unit, onShare: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(attachment.kind.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(attachment.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(
                    stringResource(attachment.kind.label()),
                    Formatter.formatShortFileSize(context, attachment.sizeBytes),
                    formatInstantDate(attachment.addedAt),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                SymbolIcon(R.drawable.ic_more_vert, contentDescription = stringResource(R.string.attachment_actions, attachment.name))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open)) },
                    leadingIcon = { SymbolIcon(R.drawable.ic_open_in_new, contentDescription = null) },
                    onClick = { menu = false; onOpen() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { SymbolIcon(R.drawable.ic_share, contentDescription = null) },
                    onClick = { menu = false; onShare() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.remove)) },
                    leadingIcon = { SymbolIcon(R.drawable.ic_delete, contentDescription = null) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
    }
}

private fun DocumentKind.icon() = when (this) {
    DocumentKind.PDF -> R.drawable.ic_file_pdf
    DocumentKind.WORD -> R.drawable.ic_file_doc
    DocumentKind.IMAGE -> R.drawable.ic_image
    DocumentKind.OTHER -> R.drawable.ic_file
}
