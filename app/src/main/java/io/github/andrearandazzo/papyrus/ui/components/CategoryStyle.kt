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
 * Icons a category can have (Phosphor duotone). [key] is what the database and backups store:
 * never change an existing key, or saved choices are lost (they fall back to automatic).
 * The order is the order of the picker, and of matching when suggesting from a name.
 */
enum class CategoryIcon(
    val key: String,
    @DrawableRes val drawable: Int,
    @StringRes val label: Int,
    val tint: Color,
    /** Words (English and Italian, no accents) in a category name that suggest this icon. */
    val keywords: Set<String> = emptySet(),
) {
    IDENTITY("identity", R.drawable.ic_cat_identity, R.string.icon_identity, GREEN,
        setOf("identity", "identita", "id", "documenti", "passport", "passaporto")),
    HEALTH("health", R.drawable.ic_cat_health, R.string.icon_health, PINK, setOf("health", "salute", "sanita")),
    MEDICAL("medical", R.drawable.ic_cat_medical, R.string.icon_medical, PINK,
        setOf("medical", "medico", "medici", "doctor", "hospital", "ospedale", "visite")),
    PRESCRIPTIONS("prescriptions", R.drawable.ic_cat_prescriptions, R.string.icon_prescriptions, PINK,
        setOf("prescriptions", "prescription", "ricette", "ricetta", "farmaci", "medicine", "pharmacy", "farmacia")),
    DENTAL("dental", R.drawable.ic_cat_dental, R.string.icon_dental, PINK,
        setOf("dental", "dentist", "dentista", "denti", "teeth")),
    CAR("car", R.drawable.ic_cat_car, R.string.icon_car, SAND, setOf("car", "auto", "vehicle", "veicolo", "macchina")),
    MOTORBIKE("motorbike", R.drawable.ic_cat_motorbike, R.string.icon_motorbike, SAND,
        setOf("motorbike", "motorcycle", "moto", "scooter", "motorino")),
    HOME("home", R.drawable.ic_cat_home, R.string.icon_home, BLUE, setOf("home", "casa", "house")),
    APARTMENT("apartment", R.drawable.ic_cat_apartment, R.string.icon_apartment, BLUE,
        setOf("apartment", "appartamento", "condominio", "condo", "rent", "affitto", "flat")),
    KEYS("keys", R.drawable.ic_cat_keys, R.string.icon_keys, SAND, setOf("keys", "chiavi", "property", "proprieta")),
    UTILITIES("utilities", R.drawable.ic_cat_utilities, R.string.icon_utilities, OLIVE,
        setOf("utilities", "utenze", "bills", "bollette", "energy", "energia", "luce", "gas", "electricity")),
    PHONE("phone", R.drawable.ic_cat_phone, R.string.icon_phone, LAVENDER,
        setOf("phone", "telefono", "mobile", "cellulare", "internet", "sim")),
    ELECTRONICS("electronics", R.drawable.ic_cat_electronics, R.string.icon_electronics, LAVENDER,
        setOf("electronics", "elettronica", "devices", "dispositivi", "computer", "laptop")),
    FINANCE("finance", R.drawable.ic_cat_finance, R.string.icon_finance, OLIVE,
        setOf("finance", "finanza", "finanze", "money", "soldi")),
    BANK("bank", R.drawable.ic_cat_bank, R.string.icon_bank, OLIVE,
        setOf("bank", "banca", "conto", "account", "tax", "taxes", "tasse", "fisco")),
    SAVINGS("savings", R.drawable.ic_cat_savings, R.string.icon_savings, OLIVE,
        setOf("savings", "risparmi", "investments", "investimenti", "pension", "pensione")),
    RECEIPTS("receipts", R.drawable.ic_cat_receipts, R.string.icon_receipts, SAND,
        setOf("receipts", "receipt", "ricevute", "scontrini", "warranty", "warranties", "garanzia", "garanzie")),
    INSURANCE("insurance", R.drawable.ic_cat_insurance, R.string.icon_insurance, BLUE,
        setOf("insurance", "assicurazione", "assicurazioni", "polizza", "polizze")),
    CONTRACTS("contracts", R.drawable.ic_cat_contracts, R.string.icon_contracts, LAVENDER,
        setOf("contracts", "contract", "contratti", "contratto", "agreements")),
    LEGAL("legal", R.drawable.ic_cat_legal, R.string.icon_legal, LAVENDER,
        setOf("legal", "legale", "lawyer", "avvocato", "court", "tribunale")),
    WORK("work", R.drawable.ic_cat_work, R.string.icon_work, LAVENDER, setOf("work", "lavoro", "job", "payslips", "buste")),
    SCHOOL("school", R.drawable.ic_cat_school, R.string.icon_school, OLIVE,
        setOf("school", "scuola", "university", "universita", "study", "studio")),
    CERTIFICATES("certificates", R.drawable.ic_cat_certificates, R.string.icon_certificates, OLIVE,
        setOf("certificates", "certificati", "diplomas", "diplomi", "attestati", "degree", "laurea")),
    TRAVEL("travel", R.drawable.ic_cat_travel, R.string.icon_travel, BLUE, setOf("travel", "viaggi", "viaggio", "trips")),
    TICKETS("tickets", R.drawable.ic_cat_tickets, R.string.icon_tickets, PINK,
        setOf("tickets", "biglietti", "events", "eventi", "concerts", "concerti")),
    FAMILY("family", R.drawable.ic_cat_family, R.string.icon_family, GREEN,
        setOf("family", "famiglia", "kids", "children", "figli")),
    BABY("baby", R.drawable.ic_cat_baby, R.string.icon_baby, PINK, setOf("baby", "bambini", "bambino", "neonato")),
    PETS("pets", R.drawable.ic_cat_pets, R.string.icon_pets, SAND,
        setOf("pets", "pet", "animals", "animali", "dog", "dogs", "cane", "cani", "cat", "cats", "gatto", "gatti", "vet", "veterinario")),
    FITNESS("fitness", R.drawable.ic_cat_fitness, R.string.icon_fitness, GREEN,
        setOf("fitness", "gym", "palestra", "sport", "sports")),
    SHOPPING("shopping", R.drawable.ic_cat_shopping, R.string.icon_shopping, LAVENDER,
        setOf("shopping", "acquisti", "spesa", "orders", "ordini")),
    GIFTS("gifts", R.drawable.ic_cat_gifts, R.string.icon_gifts, PINK, setOf("gifts", "gift", "regali", "regalo")),
    FOLDER("folder", R.drawable.ic_cat_folder, R.string.icon_folder, GREEN);

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
        CategoryStyle(R.drawable.ic_cat_folder, FALLBACK_TINTS[name.lowercase().hashCode().absoluteValue % FALLBACK_TINTS.size])
    }
}
