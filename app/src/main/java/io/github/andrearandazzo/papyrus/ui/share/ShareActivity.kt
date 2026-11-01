package io.github.andrearandazzo.papyrus.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.syncStarterCategories
import io.github.andrearandazzo.papyrus.data.ImportResult
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle
import io.github.andrearandazzo.papyrus.ui.theme.setPapyrusContent

/**
 * Receives files from Android's share menu ("Share → Papyrus"). It opens on top of the app the user
 * shares from and closes after saving, so they land back where they were.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels { ShareViewModel.factory(sharedUris(intent)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (sharedUris(intent).isEmpty()) {
            Toast.makeText(this, R.string.share_nothing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        syncStarterCategories()
        setPapyrusContent {
            ShareScreen(viewModel, onClose = ::finish, onDone = { result ->
                Toast.makeText(this, result.message(), Toast.LENGTH_SHORT).show()
                finish()
            })
        }
    }

    private fun ImportResult.message(): String =
        if (failed == 0) resources.getQuantityString(R.plurals.import_done, imported, imported)
        else resources.getQuantityString(R.plurals.import_failed, failed, failed)

    private companion object {
        /** The files of a SEND or SEND_MULTIPLE intent; text-only shares have none. */
        fun sharedUris(intent: Intent): List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareScreen(viewModel: ShareViewModel, onClose: () -> Unit, onDone: (ImportResult) -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val names = viewModel.fileNames

    LaunchedEffect(viewModel.result) { viewModel.result?.let(onDone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !viewModel.saving) {
                        SymbolIcon(R.drawable.ic_close, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::save,
                enabled = names != null && !viewModel.saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
            ) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (viewModel.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (names == null) {
                CircularProgressIndicator(Modifier.padding(24.dp).align(Alignment.CenterHorizontally))
                return@Column
            }
            SectionLabel(
                pluralStringResource(R.plurals.share_files, names.size, names.size),
                Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            names.forEach { name ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    SymbolIcon(R.drawable.ic_duo_document, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            SectionLabel(stringResource(R.string.category), Modifier.padding(top = 20.dp, bottom = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = viewModel.categoryId == null,
                    onClick = { viewModel.categoryId = null },
                    label = { Text(stringResource(R.string.no_category)) },
                    modifier = Modifier.height(40.dp),
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = viewModel.categoryId == category.id,
                        onClick = { viewModel.categoryId = category.id },
                        leadingIcon = {
                            SymbolIcon(categoryStyle(category).icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(category.name) },
                        modifier = Modifier.height(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
