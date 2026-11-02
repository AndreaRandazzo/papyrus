package io.github.andrearandazzo.papyrus.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Corners
import io.github.andrearandazzo.papyrus.data.PdfMaker
import io.github.andrearandazzo.papyrus.data.ScanPage
import io.github.andrearandazzo.papyrus.ui.components.SymbolIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Full-screen editor: drag the four corners onto the paper's corners. [onDone] gets the new corners,
 * or null if the user cancels.
 */
@Composable
fun CropEditor(page: ScanPage, onDone: (Corners?) -> Unit) {
    // The upright photo without any crop, so the user sees what they're cropping from.
    val image by produceState<ImageBitmap?>(null, page.path, page.rotation) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                PdfMaker.loadUpright(File(page.path), page.rotation, maxSide = 1600).asImageBitmap()
            }.getOrNull()
        }
    }
    var points by remember(page.path, page.rotation) {
        mutableStateOf((page.corners ?: Corners.FULL).points.map { (x, y) -> Offset(x, y) })
    }

    Dialog(
        onDismissRequest = { onDone(null) },
        // Back button or ✕ to leave; a stray tap must not throw the crop away.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(color = Color(0xFF101310), contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    IconButton(onClick = { onDone(null) }) {
                        SymbolIcon(R.drawable.ic_close, contentDescription = stringResource(android.R.string.cancel))
                    }
                    Text(
                        stringResource(R.string.crop_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { onDone(Corners(points.map { it.x to it.y })) }) {
                        Text(stringResource(R.string.crop_done))
                    }
                }
                Text(
                    stringResource(R.string.crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    val current = image
                    if (current == null) {
                        CircularProgressIndicator()
                    } else {
                        CornerCanvas(current, points) { points = it }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    TextButton(onClick = { points = Corners.FULL.points.map { (x, y) -> Offset(x, y) } }) {
                        SymbolIcon(R.drawable.ic_crop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.crop_whole_photo), color = Color.White)
                    }
                }
            }
        }
    }
}

/** The photo, fitted to the space, with the paper outline and four draggable corner handles. */
@Composable
private fun CornerCanvas(image: ImageBitmap, points: List<Offset>, onChange: (List<Offset>) -> Unit) {
    val handleRadius = with(LocalDensity.current) { 14.dp.toPx() }
    val touchRadius = with(LocalDensity.current) { 48.dp.toPx() }
    // Space around the photo, so handles on its edges are fully touchable and clear of the
    // screen edges (where Android's back gesture starts).
    val margin = with(LocalDensity.current) { 32.dp.toPx() }
    val sage = MaterialTheme.colorScheme.primary
    // The drag handler lives across recompositions: it must read the latest corners, not the first.
    val currentPoints by rememberUpdatedState(points)
    val currentOnChange by rememberUpdatedState(onChange)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        val scale = min((boxW - 2 * margin) / image.width, (boxH - 2 * margin) / image.height)
        val imgW = image.width * scale
        val imgH = image.height * scale
        val left = (boxW - imgW) / 2
        val top = (boxH - imgH) / 2
        fun toScreen(p: Offset) = Offset(left + p.x * imgW, top + p.y * imgH)

        Image(
            image,
            contentDescription = null,
            modifier = Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(LocalDensity.current) { imgW.toDp() }, with(LocalDensity.current) { imgH.toDp() }),
        )

        var dragging by remember { mutableIntStateOf(-1) }
        Canvas(
            Modifier
                .fillMaxSize()
                // Dragging a handle near the edge must not trigger Android's back gesture.
                .systemGestureExclusion()
                .pointerInput(image, imgW, imgH) {
                    detectDragGestures(
                        onDragStart = { start ->
                            // Grab the nearest corner, if the finger is close enough to one.
                            val nearest = currentPoints.indices.minByOrNull { (toScreen(currentPoints[it]) - start).getDistance() }
                            dragging = nearest?.takeIf { (toScreen(currentPoints[it]) - start).getDistance() <= touchRadius } ?: -1
                        },
                        onDragEnd = { dragging = -1 },
                        onDragCancel = { dragging = -1 },
                        onDrag = { change, _ ->
                            if (dragging >= 0) {
                                change.consume()
                                // The corner sits under the finger, so it ends exactly where the finger lifts.
                                val finger = change.position
                                val moved = Offset(
                                    ((finger.x - left) / imgW).coerceIn(0f, 1f),
                                    ((finger.y - top) / imgH).coerceIn(0f, 1f),
                                )
                                currentOnChange(currentPoints.toMutableList().also { it[dragging] = moved })
                            }
                        },
                    )
                },
        ) {
            val screen = points.map(::toScreen)
            val outline = Path().apply {
                moveTo(screen[0].x, screen[0].y)
                screen.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            // Dim everything outside the paper.
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(left, top, left + imgW, top + imgH))
                addPath(outline)
            }
            drawPath(dim, Color.Black.copy(alpha = 0.5f))
            drawPath(outline, sage, style = Stroke(width = 2.dp.toPx()))
            screen.forEachIndexed { i, point ->
                drawCircle(Color.White, radius = if (i == dragging) handleRadius * 1.3f else handleRadius, center = point)
                drawCircle(sage, radius = handleRadius, center = point, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}
