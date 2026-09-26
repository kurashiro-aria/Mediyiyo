package cl.kura.mediyiyo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    private const val PREFS = "mediyiyo_alarms"

    fun schedule(context: Context, medId: String, medName: String, intervalHours: Int, lastTakenMillis: Long) {
        val intervalMillis = intervalHours * 60L * 60L * 1000L
        var next = lastTakenMillis + intervalMillis
        val now = System.currentTimeMillis()
        if (next <= now) {
            next += ((now - next) / intervalMillis + 1) * intervalMillis
        }
        save(context, medId, medName, intervalHours, next)
        scheduleAt(context, medId, medName, intervalHours, next)
    }

    fun scheduleFromNow(context: Context, medId: String, medName: String, intervalHours: Int) {
        schedule(context, medId, medName, intervalHours, System.currentTimeMillis())
    }

    private fun scheduleAt(context: Context, medId: String, medName: String, intervalHours: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra("medId", medId); putExtra("medName", medName); putExtra("intervalHours", intervalHours)
        }
        val pending = PendingIntent.getBroadcast(context, medId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms())
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            else
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun save(context: Context, id:String, name:String, hours:Int, next:Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("${id}_name",name).putInt("${id}_hours",hours).putLong("${id}_next",next)
            .putStringSet("ids", (context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getStringSet("ids", emptySet()) ?: emptySet()) + id).apply()
    }

    fun restoreAll(context: Context) {
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        p.getStringSet("ids", emptySet())?.forEach { id ->
            val name=p.getString("${id}_name",id) ?: id
            val hours=p.getInt("${id}_hours",24)
            var next=p.getLong("${id}_next",0L)
            val intervalMillis = hours * 60L * 60L * 1000L
            val now = System.currentTimeMillis()
            if (next <= now) next += ((now - next) / intervalMillis + 1) * intervalMillis
            save(context,id,name,hours,next)
            scheduleAt(context,id,name,hours,next)
        }
    }
}
