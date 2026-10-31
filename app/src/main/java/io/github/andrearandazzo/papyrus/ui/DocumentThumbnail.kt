package io.github.andrearandazzo.papyrus.ui

import androidx.compose.foundation.Image
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
    val thumbnails = (LocalContext.current.applicationContext as PapyrusApp).thumbnails
    val widthPx = with(LocalDensity.current) { width.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(null, document.fileName, widthPx) {
        value = thumbnails.load(document, widthPx)?.asImageBitmap()
    }
    val image = bitmap
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
