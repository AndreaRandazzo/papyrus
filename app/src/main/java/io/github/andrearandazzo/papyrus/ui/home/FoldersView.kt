package io.github.andrearandazzo.papyrus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Folder
import io.github.andrearandazzo.papyrus.data.LibraryView
import io.github.andrearandazzo.papyrus.ui.components.CategoryStyle
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle

/** List / Grid / Folders as one small segmented control; the chosen one is filled, on mint. */
@Composable
fun LibraryViewSwitch(selected: LibraryView, onSelect: (LibraryView) -> Unit) {
    Row(
        Modifier
            .padding(end = 8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        LibraryView.entries.forEach { view ->
            val isSelected = view == selected
            val (icon, selectedIcon, label) = when (view) {
                LibraryView.LIST -> Triple(R.drawable.ic_view_agenda, R.drawable.ic_view_agenda_filled, R.string.list_view)
                LibraryView.GRID -> Triple(R.drawable.ic_grid_view, R.drawable.ic_grid_view_filled, R.string.grid_view)
                LibraryView.FOLDERS -> Triple(R.drawable.ic_folder, R.drawable.ic_folder_filled, R.string.folders_view)
            }
            val description = stringResource(label)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 44.dp, height = 40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(view) }
                    .semantics { contentDescription = description },
            ) {
                SymbolIcon(if (isSelected) selectedIcon else icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** The Folders overview: a card per category, "No category" last. */
@Composable
fun FolderGrid(folders: List<Folder>, onOpen: (Folder) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        // Bottom padding keeps the last cards clear of the "+" button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp)) {
                SectionLabel(stringResource(R.string.nav_categories), Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.folder_count, folders.size, folders.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(folders, key = { it.filterId }) { folder -> FolderCard(folder, onClick = { onOpen(folder) }) }
    }
}

@Composable
private fun FolderCard(folder: Folder, onClick: () -> Unit) {
    val style = folder.style()
    Column(
        Modifier
            .fillMaxWidth()
            // A minimum, not a fixed height: larger text sizes need more room.
            .heightIn(min = 140.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CategoryTile(style, size = 48)
            Spacer(Modifier.weight(1f))
            if (folder.urgentCount > 0) UrgentBadge(folder.urgentCount)
        }
        Spacer(Modifier.weight(1f))
        // Full width and up to two lines, so longer names like "Nessuna categoria" fit.
        Text(
            folder.name(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(R.plurals.category_document_count, folder.documentCount, folder.documentCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(R.drawable.ic_folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Clock and count of documents that are expired or expire soon. */
@Composable
private fun UrgentBadge(count: Int) {
    val description = pluralStringResource(R.plurals.folder_urgent, count, count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .padding(horizontal = 7.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Icon(
            painterResource(R.drawable.ic_schedule),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
        )
    }
}

/** Top bar inside a folder: back, the category's icon and name, and how many documents it holds. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderTopBar(folder: Folder, onBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                SymbolIcon(R.drawable.ic_arrow_back, contentDescription = stringResource(R.string.back))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryTile(folder.style(), size = 40)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        folder.name(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        pluralStringResource(R.plurals.category_document_count, folder.documentCount, folder.documentCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun CategoryTile(style: CategoryStyle, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .background(style.tint.copy(alpha = 0.3f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(style.icon), contentDescription = null, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun Folder.name(): String = category?.name ?: stringResource(R.string.no_category)

private fun Folder.style(): CategoryStyle =
    category?.let(::categoryStyle) ?: CategoryStyle(R.drawable.ic_cat_tray, NO_CATEGORY_TINT)

/** Neutral grey: "No category" isn't a category of its own. */
private val NO_CATEGORY_TINT = Color(0xFF9A9C94)
