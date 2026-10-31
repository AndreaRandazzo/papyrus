package io.github.andrearandazzo.papyrus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.andrearandazzo.papyrus.ui.components.DocumentPlaceholder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Document

/** Loads a preview of [page] in the background, about [width] wide; null while loading or if there is none. */
@Composable
fun rememberPreview(document: Document, width: Dp, page: Int = 0): ImageBitmap? {
    val thumbnails = (LocalContext.current.applicationContext as PapyrusApp).thumbnails
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(null, document.fileName, widthPx, page) {
        value = thumbnails.load(document, widthPx, page)?.asImageBitmap()
    }
    return bitmap
}

/** A rounded tile showing the document's preview, or a paper/Word placeholder. Size it with [modifier]. */
@Composable
fun DocumentTile(document: Document, modifier: Modifier, previewWidth: Dp = 80.dp) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier
            .clip(shape)
            // Outline so a white page stands out from the background.
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        DocumentThumbnail(
            document = document,
            width = previewWidth,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
            placeholder = { DocumentPlaceholder(document.kind, Modifier.matchParentSize()) },
        )
    }
}

/**
 * Shows the document's preview, loaded in the background at about [width] wide.
 * Shows [placeholder] while loading or when the document has no preview.
 */
@Composable
fun DocumentThumbnail(
    document: Document,
    width: Dp,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit,
) {
    val image = rememberPreview(document, width)
    if (image == null) {
        placeholder()
    } else {
        // When cropped, keep the top: that's where a page's heading is, not the blank middle.
        Image(
            image,
            contentDescription = null,
            contentScale = contentScale,
            alignment = Alignment.TopCenter,
            modifier = modifier,
        )
    }
}
