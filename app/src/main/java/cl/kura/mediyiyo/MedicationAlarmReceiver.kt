package cl.kura.mediyiyo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("medName") ?: "Medicamento"
        val intervalHours = intent.getIntExtra("intervalHours", 24)
        val medId = intent.getStringExtra("medId") ?: medName
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "mediyiyo_medicamentos"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Recordatorios de medicamentos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisos cuando corresponde la siguiente toma"
                enableVibration(true)
            })
        }
        val openIntent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, medId.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora de tu medicamento")
            .setContentText("Corresponde tomar $medName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(medId.hashCode(), notification)
        AlarmScheduler.scheduleFromNow(context, medId, medName, intervalHours)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AlarmScheduler.restoreAll(context)
    }
}
