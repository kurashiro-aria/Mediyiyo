package cl.kura.mediyiyo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MedicineCatalogTest {
    @Test fun restoresKnownDescriptionForPreviouslySavedMedicine() {
        val description = MedicineCatalog.descriptionFor("Omeprazol 20 mg", "")
        assertFalse(description.isBlank())
        assertEquals(MedicineCatalog.items.first { it.name == "Omeprazol" }.info, description)
    }

    @Test fun preservesPersonalTextAndDoesNotInventUnknownInformation() {
        assertEquals("Mi nota", MedicineCatalog.descriptionFor("Omeprazol", "Mi nota"))
        assertEquals("", MedicineCatalog.descriptionFor("Medicamento personal desconocido", ""))
    }
}
