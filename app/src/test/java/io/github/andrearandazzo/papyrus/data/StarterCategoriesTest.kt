package io.github.andrearandazzo.papyrus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StarterCategoriesTest {

    @Test
    fun recognizesEveryTranslationIgnoringCaseAndSpaces() {
        assertEquals("health", StarterCategories.keyForName("Health"))
        assertEquals("health", StarterCategories.keyForName("salute"))
        assertEquals("identity", StarterCategories.keyForName(" IDENTITÀ "))
    }

    @Test
    fun otherNamesAreNotStarters() {
        assertNull(StarterCategories.keyForName("Dog"))
        assertNull(StarterCategories.keyForName("Health 2"))
    }

    @Test
    fun everyKeyHasKnownNames() {
        assertEquals(StarterCategories.KEYS.toSet(), StarterCategories.knownNames.map { it.first }.toSet())
    }
}
