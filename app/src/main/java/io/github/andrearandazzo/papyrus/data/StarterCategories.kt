package io.github.andrearandazzo.papyrus.data

import java.util.Locale

/**
 * The categories created on first launch. While the user hasn't renamed them they carry a
 * [Category.starterKey] and follow the app's language; their names come from string resources
 * (see PapyrusApp). [KNOWN_NAMES] lists every translation, to recognize them in data written
 * before keys existed (old databases and backups).
 */
object StarterCategories {
    /** Creation order on first launch. Never change a key: they are stored in databases and backups. */
    val KEYS = listOf("identity", "health", "home", "car", "work", "finance")

    /** Every shipped translation of each starter's name. Add new languages here too. */
    private val KNOWN_NAMES = mapOf(
        "identity" to listOf("Identity", "Identità"),
        "health" to listOf("Health", "Salute"),
        "home" to listOf("Home", "Casa"),
        "car" to listOf("Car", "Auto"),
        "work" to listOf("Work", "Lavoro"),
        "finance" to listOf("Finance", "Finanze"),
    )

    /** The starter a name is a translation of (ignoring case), or null for any other name. */
    fun keyForName(name: String): String? {
        val wanted = name.trim().lowercase(Locale.ROOT)
        return KNOWN_NAMES.entries.firstOrNull { (_, names) -> names.any { it.lowercase(Locale.ROOT) == wanted } }?.key
    }

    /** Every (key, name) pair, for the database upgrade that marks existing starters. */
    val knownNames: List<Pair<String, String>> get() = KNOWN_NAMES.flatMap { (key, names) -> names.map { key to it } }
}
