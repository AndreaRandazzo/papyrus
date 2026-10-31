package io.github.andrearandazzo.papyrus.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.andrearandazzo.papyrus.R
import io.github.andrearandazzo.papyrus.data.Category
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

/**
 * Icons a category can have. [key] is what the database and backups store:
 * never change an existing key, or saved choices are lost (they fall back to automatic).
 */
enum class CategoryIcon(
    val key: String,
    @DrawableRes val drawable: Int,
    @StringRes val label: Int,
    val tint: Color,
    /** Words (English and Italian, no accents) in a category name that suggest this icon. */
    val keywords: Set<String> = emptySet(),
) {
    IDENTITY("identity", R.drawable.ic_badge, R.string.icon_identity, GREEN,
        setOf("identity", "identita", "id", "documenti", "passport", "passaporto")),
    HEALTH("health", R.drawable.ic_favorite, R.string.icon_health, PINK,
        setOf("health", "salute", "sanita")),
    MEDICAL("medical", R.drawable.ic_medical_services, R.string.icon_medical, PINK,
        setOf("medical", "medico", "medici", "doctor", "hospital", "ospedale")),
    CAR("car", R.drawable.ic_directions_car, R.string.icon_car, SAND,
        setOf("car", "auto", "vehicle", "veicolo", "moto")),
    HOME("home", R.drawable.ic_home, R.string.icon_home, BLUE, setOf("home", "casa", "house")),
    FINANCE("finance", R.drawable.ic_account_balance_wallet, R.string.icon_finance, OLIVE,
        setOf("finance", "finanza", "finanze", "bank", "banca", "money", "soldi", "tax", "tasse")),
    SAVINGS("savings", R.drawable.ic_savings, R.string.icon_savings, OLIVE, setOf("savings", "risparmi")),
    RECEIPTS("receipts", R.drawable.ic_receipt_long, R.string.icon_receipts, SAND,
        setOf("receipts", "receipt", "ricevute", "scontrini", "bills", "bollette", "warranty", "garanzia", "garanzie")),
    INSURANCE("insurance", R.drawable.ic_shield, R.string.icon_insurance, BLUE,
        setOf("insurance", "assicurazione", "assicurazioni", "polizza", "polizze")),
    WORK("work", R.drawable.ic_work, R.string.icon_work, LAVENDER, setOf("work", "lavoro", "job")),
    SCHOOL("school", R.drawable.ic_school, R.string.icon_school, OLIVE,
        setOf("school", "scuola", "university", "universita", "study", "studio")),
    TRAVEL("travel", R.drawable.ic_flight, R.string.icon_travel, BLUE, setOf("travel", "viaggi", "viaggio", "trips")),
    FAMILY("family", R.drawable.ic_family_restroom, R.string.icon_family, GREEN,
        setOf("family", "famiglia", "kids", "children", "figli", "bambini")),
    PETS("pets", R.drawable.ic_pets, R.string.icon_pets, SAND,
        setOf("pets", "pet", "animals", "animali", "dog", "dogs", "cane", "cani", "cat", "cats", "gatto", "gatti")),
    SHOPPING("shopping", R.drawable.ic_shopping_bag, R.string.icon_shopping, LAVENDER,
        setOf("shopping", "acquisti", "spesa")),
    FOLDER("folder", R.drawable.ic_folder, R.string.icon_folder, GREEN);

    companion object {
        fun fromKey(key: String?): CategoryIcon? = entries.firstOrNull { it.key == key }

        /** The icon a name suggests, or null if no word matches. */
        fun suggestedFor(name: String): CategoryIcon? {
            val words = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .split(Regex("[^a-z]+"))
                .filter { it.isNotEmpty() }
            return entries.firstOrNull { icon -> words.any { it in icon.keywords } }
        }
    }
}

/**
 * The icon the user picked, else one suggested by the name, else a folder in a color
 * that stays the same for that name.
 */
fun categoryStyle(category: Category): CategoryStyle = categoryStyle(category.name, category.icon)

fun categoryStyle(name: String, iconKey: String?): CategoryStyle {
    val icon = CategoryIcon.fromKey(iconKey) ?: CategoryIcon.suggestedFor(name)
    return if (icon != null) {
        CategoryStyle(icon.drawable, icon.tint)
    } else {
        CategoryStyle(R.drawable.ic_folder, FALLBACK_TINTS[name.lowercase().hashCode().absoluteValue % FALLBACK_TINTS.size])
    }
}
