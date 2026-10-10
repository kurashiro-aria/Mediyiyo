package cl.kura.mediyiyo

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/** Existing on-device key format. Do not change without migrating old records. */
object DoseRecords {
    fun key(medicineId: String, date: LocalDate, dose: Int): String = "${medicineId}_${date}_${dose}"

    fun fromAlarmId(alarmId: String): Pair<String, Int>? {
        val marker = "_dose_"
        val split = alarmId.lastIndexOf(marker)
        if (split <= 0) return null
        val dose = alarmId.substring(split + marker.length).toIntOrNull() ?: return null
        return if (dose >= 0) alarmId.substring(0, split) to dose else null
    }

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("mediyiyo", Context.MODE_PRIVATE)

    fun read(context: Context, medicineId: String, date: LocalDate, dose: Int): String? =
        prefs(context).getString(key(medicineId, date, dose), null)

    fun confirm(context: Context, medicineId: String, date: LocalDate, dose: Int, actualTime: String) {
        prefs(context).edit().putString(key(medicineId, date, dose), actualTime).apply()
    }

    fun undo(context: Context, medicineId: String, date: LocalDate, dose: Int) {
        prefs(context).edit().remove(key(medicineId, date, dose)).apply()
    }
}

data class DoseSlot(val medicineId: String, val dose: Int, val plannedTime: String?, val medicineOrder: Int)

object DoseTimeline {
    fun chronological(slots: List<DoseSlot>): List<DoseSlot> = slots.sortedWith(
        compareBy<DoseSlot> { it.plannedTime ?: "99:99" }
            .thenBy { it.medicineOrder }
            .thenBy { it.dose }
    )
}
