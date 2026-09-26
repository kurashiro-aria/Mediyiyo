package cl.kura.mediyiyo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {
    private const val PREFS = "mediyiyo_alarms"

    fun scheduleFromPreviousDose(
        context: Context,
        alarmId: String,
        medName: String,
        referenceDate: LocalDate,
        referenceTime: LocalTime
    ) {
        val now = ZonedDateTime.now()
        var nextDate = referenceDate.plusDays(1)
        var next = ZonedDateTime.of(nextDate, referenceTime, ZoneId.systemDefault())
        while (!next.isAfter(now)) {
            nextDate = nextDate.plusDays(1)
            next = ZonedDateTime.of(nextDate, referenceTime, ZoneId.systemDefault())
        }
        val triggerAt = next.toInstant().toEpochMilli()
        save(context, alarmId, medName, referenceTime, triggerAt)
        scheduleAt(context, alarmId, medName, triggerAt)
    }

    private fun scheduleAt(context: Context, alarmId: String, medName: String, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
            putExtra("medName", medName)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun save(context: Context, id: String, name: String, time: LocalTime, next: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = (prefs.getStringSet("ids", emptySet()) ?: emptySet()).toMutableSet().apply { add(id) }
        prefs.edit()
            .putString("${id}_name", name)
            .putString("${id}_time", time.toString())
            .putLong("${id}_next", next)
            .putStringSet("ids", ids)
            .apply()
    }

    fun scheduleNextDay(context: Context, alarmId: String, medName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = LocalTime.parse(prefs.getString("${alarmId}_time", "08:00"))
        scheduleFromPreviousDose(context, alarmId, medName, LocalDate.now(), time)
    }

    fun restoreAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet())?.forEach { id ->
            val name = prefs.getString("${id}_name", id) ?: id
            val time = LocalTime.parse(prefs.getString("${id}_time", "08:00"))
            var next = ZonedDateTime.of(LocalDate.now(), time, ZoneId.systemDefault())
            if (!next.isAfter(ZonedDateTime.now())) next = next.plusDays(1)
            val millis = next.toInstant().toEpochMilli()
            save(context, id, name, time, millis)
            scheduleAt(context, id, name, millis)
        }
    }
}
