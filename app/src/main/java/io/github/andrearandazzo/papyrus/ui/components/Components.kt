package io.github.andrearandazzo.papyrus.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.DocumentKind
import io.github.andrearandazzo.papyrus.ui.theme.Paper
import io.github.andrearandazzo.papyrus.ui.theme.WordBlue

/** A Material Symbols icon from res/drawable, tinted with the current content color. */
@Composable
fun SymbolIcon(@DrawableRes icon: Int, contentDescription: String?, modifier: Modifier = Modifier) {
    Icon(painterResource(icon), contentDescription, modifier)
}

/** Small uppercase section label, e.g. "6 DOCUMENTS" or "SORT BY". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Stand-in for a document without a preview: a sheet of paper with text lines,
 * or a blue tile for Word files.
 */
@Composable
fun DocumentPlaceholder(kind: DocumentKind, modifier: Modifier = Modifier, large: Boolean = false) {
    if (kind == DocumentKind.WORD) {
        Box(modifier.background(WordBlue), contentAlignment = Alignment.BottomStart) {
            Text(
                "W",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = if (large) 48.sp else 22.sp,
                modifier = Modifier.padding(if (large) 20.dp else 8.dp),
            )
        }
        return
    }
    val lineColor = Color(0x33000000)
    Column(
        modifier.background(Paper).padding(if (large) 24.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (large) 10.dp else 6.dp),
    ) {
        Spacer(Modifier.height(if (large) 8.dp else 4.dp))
        listOf(1f, 0.7f, 0.85f).forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(if (large) 4.dp else 3.dp)
                    .background(lineColor, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Two stacked sheets with a symbol on top, used by empty states. */
@Composable
fun PaperIllustration(@DrawableRes symbol: Int, backColor: Color, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val sheet = Modifier.size(width = size * 0.78f, height = size * 0.92f)
        Box(
            sheet
                .offset(x = size * 0.1f, y = size * 0.06f)
                .rotate(4f)
                .background(backColor, RoundedCornerShape(16.dp)),
        )
        Box(
            sheet
                .offset(x = -size * 0.04f)
                .rotate(-3f)
                .background(Paper, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(symbol),
                contentDescription = null,
                tint = Color(0xFF526351),
                modifier = Modifier.size(size * 0.32f),
            )
        }
    }
}

/** Rounded card with a lock, reminding that nothing leaves the phone. */
@Composable
fun PrivacyNote(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SymbolIcon(R.drawable.ic_lock, contentDescription = null)
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}
