package io.github.andrearandazzo.papyrus.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import io.github.andrearandazzo.papyrus.R
import java.text.Normalizer
import kotlin.math.absoluteValue

/** The icon and color shown next to a category. */
data class CategoryStyle(@DrawableRes val icon: Int, val tint: Color)

private val GREEN = Color(0xFF7FA37A)
private val PINK = Color(0xFFD98C9A)
private val SAND = Color(0xFFC9A27A)
private val BLUE = Color(0xFF8BA3D1)
private val OLIVE = Color(0xFFA9AE7B)
private val LAVENDER = Color(0xFFA99AC9)
private val FALLBACK_TINTS = listOf(GREEN, PINK, SAND, BLUE, OLIVE, LAVENDER)

/** Words (English and Italian, without accents) that pick a matching icon. */
private val KNOWN = listOf(
    listOf("identity", "identita", "id", "documenti", "passport", "passaporto") to CategoryStyle(R.drawable.ic_badge, GREEN),
    listOf("health", "salute", "medical", "medico", "medici", "sanita") to CategoryStyle(R.drawable.ic_favorite, PINK),
    listOf("car", "auto", "vehicle", "veicolo", "moto") to CategoryStyle(R.drawable.ic_directions_car, SAND),
    listOf("home", "casa", "house") to CategoryStyle(R.drawable.ic_home, BLUE),
    listOf("finance", "finanza", "finanze", "bank", "banca", "money", "soldi", "tax", "tasse") to
        CategoryStyle(R.drawable.ic_account_balance_wallet, OLIVE),
    listOf("work", "lavoro", "job") to CategoryStyle(R.drawable.ic_work, LAVENDER),
    listOf("travel", "viaggi", "viaggio", "trips") to CategoryStyle(R.drawable.ic_flight, BLUE),
    listOf("school", "scuola", "university", "universita", "study", "studio") to CategoryStyle(R.drawable.ic_school, OLIVE),
)

/**
 * Categories are user-named, so the look is derived from the name: well-known names get a
 * matching icon, others a folder in a color that stays the same for that name.
 */
fun categoryStyle(name: String): CategoryStyle {
    val words = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .split(Regex("[^a-z]+"))
        .filter { it.isNotEmpty() }
    KNOWN.firstOrNull { (keys, _) -> words.any { it in keys } }?.let { return it.second }
    return CategoryStyle(R.drawable.ic_folder, FALLBACK_TINTS[name.lowercase().hashCode().absoluteValue % FALLBACK_TINTS.size])
}
