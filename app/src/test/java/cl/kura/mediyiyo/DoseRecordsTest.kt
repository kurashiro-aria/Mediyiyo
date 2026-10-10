package cl.kura.mediyiyo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DoseRecordsTest {
    @Test fun alarmAndScreenAddressTheSameDose() {
        val (medicineId, dose) = DoseRecords.fromAlarmId("custom_abc_dose_2")!!
        assertEquals("custom_abc", medicineId)
        assertEquals(2, dose)
        assertEquals("custom_abc_2026-10-10_2",
            DoseRecords.key(medicineId, LocalDate.of(2026, 10, 10), dose))
        assertNull(DoseRecords.fromAlarmId("custom_abc_dose_invalid"))
    }

    @Test fun dayAndDoseHaveIndependentKeys() {
        val today = LocalDate.of(2026, 10, 10)
        val one = DoseRecords.key("med", today, 0)
        assertNotEquals(one, DoseRecords.key("med", today, 1))
        assertNotEquals(one, DoseRecords.key("med", today.plusDays(1), 0))
    }

    @Test fun multipleDosesAreChronologicalWithoutLosingTheirIdentity() {
        val slots = listOf(
            DoseSlot("a", 2, "20:00", 0),
            DoseSlot("b", 0, "14:00", 1),
            DoseSlot("a", 0, "08:00", 0),
            DoseSlot("a", 1, "14:00", 0)
        )
        assertEquals(listOf("a:0", "a:1", "b:0", "a:2"),
            DoseTimeline.chronological(slots).map { "${it.medicineId}:${it.dose}" })
    }
}
