package io.github.andrearandazzo.papyrus.ui.scan

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.PdfMaker
import io.github.andrearandazzo.papyrus.data.ScanPage
import io.github.andrearandazzo.papyrus.ui.components.PaperIllustration
import io.github.andrearandazzo.papyrus.ui.components.SectionLabel
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import io.github.andrearandazzo.papyrus.ui.components.categoryStyle
import io.github.andrearandazzo.papyrus.ui.formatInstantDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Photograph a paper document page by page, then save it as one PDF. Photos are taken by the
 * phone's camera app, so Papyrus needs no camera permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onClose: () -> Unit, viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val noCamera = stringResource(R.string.scan_no_camera)
    val saveFailed = stringResource(R.string.scan_failed)
    val savedMessage = stringResource(R.string.scan_saved)
    val defaultTitle = stringResource(R.string.scan_default_title, formatInstantDate(System.currentTimeMillis()))

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onPhotoResult)
    val takePhoto: () -> Unit = {
        val file = viewModel.newPhotoFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        try {
            camera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            viewModel.onPhotoResult(false)
            scope.launch { snackbarHostState.showSnackbar(noCamera) }
        }
    }

    // Open the camera straight away the first time; it's why the user came here.
    LaunchedEffect(Unit) {
        if (!viewModel.autoStarted && state.pages.isEmpty()) {
            viewModel.autoStarted = true
            takePhoto()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ScanEvent.SAVED -> {
                    Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                    onClose()
                }
                ScanEvent.FAILED -> snackbarHostState.showSnackbar(saveFailed)
            }
        }
    }

    // Leaving with photos taken would lose them: ask first.
    val leave = { if (state.pages.isEmpty()) onClose() else confirmDiscard = true }
    BackHandler(enabled = state.pages.isNotEmpty() && !state.saving) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = leave, enabled = !state.saving) {
                        SymbolIcon(R.drawable.ic_close, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
            )
        },
        bottomBar = {
            if (state.pages.isNotEmpty()) {
                Button(
                    onClick = { viewModel.save(defaultTitle) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                ) { Text(stringResource(R.string.scan_save)) }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.pages.isEmpty()) {
            NoPages(onTakePhoto = takePhoto, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    label = { Text(stringResource(R.string.title)) },
                    placeholder = { Text(defaultTitle) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SectionLabel(stringResource(R.string.category), Modifier.padding(top = 8.dp, bottom = 4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text(stringResource(R.string.no_category)) },
                        modifier = Modifier.height(40.dp),
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            leadingIcon = {
                                SymbolIcon(categoryStyle(category).icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            label = { Text(category.name) },
                            modifier = Modifier.height(40.dp),
                        )
                    }
                }
            }
            item {
                SectionLabel(
                    pluralStringResource(R.plurals.scan_pages, state.pages.size, state.pages.size),
                    Modifier.padding(top = 12.dp),
                )
            }
            itemsIndexed(state.pages, key = { _, page -> page.path }) { index, page ->
                PageRow(
                    page = page,
                    number = index + 1,
                    isFirst = index == 0,
                    isLast = index == state.pages.lastIndex,
                    enabled = !state.saving,
                    onRotate = { viewModel.rotate(index) },
                    onUp = { viewModel.move(index, -1) },
                    onDown = { viewModel.move(index, +1) },
                    onDelete = { viewModel.remove(index) },
                )
            }
            item {
                OutlinedButton(
                    onClick = takePhoto,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    SymbolIcon(R.drawable.ic_camera, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_add_page))
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.scan_discard_title)) },
            text = { Text(stringResource(R.string.scan_discard_body)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onClose() }) {
                    Text(stringResource(R.string.scan_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.scan_keep)) }
            },
        )
    }
}

@Composable
private fun PageRow(
    page: ScanPage,
    number: Int,
    isFirst: Boolean,
    isLast: Boolean,
    enabled: Boolean,
    onRotate: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(10.dp),
    ) {
        PageThumbnail(page, Modifier.size(width = 72.dp, height = 96.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.scan_page_number, number),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRotate, enabled = enabled) {
            SymbolIcon(R.drawable.ic_rotate, contentDescription = stringResource(R.string.scan_rotate))
        }
        IconButton(onClick = onUp, enabled = enabled && !isFirst) {
            SymbolIcon(R.drawable.ic_arrow_up, contentDescription = stringResource(R.string.scan_move_up))
        }
        IconButton(onClick = onDown, enabled = enabled && !isLast) {
            SymbolIcon(R.drawable.ic_arrow_down, contentDescription = stringResource(R.string.scan_move_down))
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            SymbolIcon(R.drawable.ic_delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

/** A small upright preview of the photo, redrawn when the page is rotated. */
@Composable
private fun PageThumbnail(page: ScanPage, modifier: Modifier) {
    val image by produceState<ImageBitmap?>(null, page.path, page.rotation) {
        value = withContext(Dispatchers.IO) {
            runCatching { PdfMaker.loadUpright(File(page.path), page.rotation, maxSide = 300).asImageBitmap() }.getOrNull()
        }
    }
    val shape = MaterialTheme.shapes.small
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        image?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun NoPages(onTakePhoto: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        PaperIllustration(R.drawable.ic_duo_camera, backColor = MaterialTheme.colorScheme.primaryContainer)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.scan_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.scan_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onTakePhoto, modifier = Modifier.height(52.dp)) {
            SymbolIcon(R.drawable.ic_camera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scan_take_photo))
        }
    }
}
