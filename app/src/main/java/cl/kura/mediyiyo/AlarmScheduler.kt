package cl.kura.mediyiyo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {
    private const val PREFS = "mediyiyo_alarms"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun scheduleFromPreviousDose(
        context: Context,
        alarmId: String,
        medName: String,
        referenceDate: LocalDate,
        referenceTime: LocalTime
    ) {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var nextDate = referenceDate.plusDays(1)
        var next = ZonedDateTime.of(nextDate, referenceTime, zone)
        while (!next.isAfter(now)) {
            nextDate = nextDate.plusDays(1)
            next = ZonedDateTime.of(nextDate, referenceTime, zone)
        }
        val triggerAt = next.toInstant().toEpochMilli()
        save(context, alarmId, medName, referenceTime, triggerAt)
        scheduleAt(context, alarmId, medName, triggerAt)
    }

    private fun scheduleAt(context: Context, alarmId: String, medName: String, triggerAt: Long): Boolean {
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

        // Medication reminders must not silently fall back to an inexact alarm:
        // Android may delay those by many minutes while idle/batching alarms.
        if (!canScheduleExact(context)) return false

        return try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            true
        } catch (_: SecurityException) {
            false
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

    fun cancel(context: Context, alarmId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = (prefs.getStringSet("ids", emptySet()) ?: emptySet())
            .toMutableSet().apply { remove(alarmId) }
        prefs.edit()
            .putStringSet("ids", ids)
            .remove("${alarmId}_name")
            .remove("${alarmId}_time")
            .remove("${alarmId}_next")
            .apply()
        val pending = PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            Intent(context, MedicationAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
            pending.cancel()
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
            .cancel(alarmId.hashCode())
    }

    fun scheduleNextDay(context: Context, alarmId: String, medName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val time = LocalTime.parse(prefs.getString("${alarmId}_time", "08:00"))
        scheduleFromPreviousDose(context, alarmId, medName, LocalDate.now(ZoneId.systemDefault()), time)
    }

    fun restoreAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        prefs.getStringSet("ids", emptySet())?.forEach { id ->
            val name = prefs.getString("${id}_name", id) ?: id
            val time = LocalTime.parse(prefs.getString("${id}_time", "08:00"))
            var next = ZonedDateTime.of(LocalDate.now(zone), time, zone)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val millis = next.toInstant().toEpochMilli()
            save(context, id, name, time, millis)
            scheduleAt(context, id, name, millis)
        }
    }
}
