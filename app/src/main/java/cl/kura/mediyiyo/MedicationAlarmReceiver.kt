package cl.kura.mediyiyo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("medName") ?: "Medicamento"
        val alarmId = intent.getStringExtra("alarmId") ?: medName
        val active = context.getSharedPreferences("mediyiyo_alarms", Context.MODE_PRIVATE)
            .getStringSet("ids", emptySet()) ?: emptySet()
        if (alarmId !in active) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val saved = context.getSharedPreferences("mediyiyo_settings", Context.MODE_PRIVATE)
            .getString("alarm_sound", null)
        val sound: Uri = saved?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channelId = "mediyiyo_alarm_${sound.toString().hashCode()}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Alarmas Mediyiyo", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos de medicamentos"
                    enableVibration(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                }
            )
        }

        val notificationId = alarmId.hashCode()
        val open = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, LauncherActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acknowledge = PendingIntent.getBroadcast(
            context, (alarmId + "ack").hashCode(),
            Intent(context, AcknowledgeReceiver::class.java)
                .putExtra("notificationId", notificationId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val displayTime = java.time.LocalTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("💊 $medName • $displayTime")
            .setContentText("Es hora de tomar tu medicamento")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Es hora de tomar $medName. ¡Que no se te pase!"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 500, 300, 500))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "¡YA SÉ, WN!", acknowledge)
            .build()

        manager.notify(notificationId, notification)
        AlarmScheduler.scheduleNextDay(context, alarmId, medName)
    }
}

class AcknowledgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("notificationId", 0)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) AlarmScheduler.restoreAll(context)
    }
}
