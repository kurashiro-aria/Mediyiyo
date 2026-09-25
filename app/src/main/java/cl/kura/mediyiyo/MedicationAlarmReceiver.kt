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

class MedicationAlarmReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val medName=intent.getStringExtra("medName")?:"Medicamento";val hours=intent.getIntExtra("intervalHours",24);val medId=intent.getStringExtra("medId")?:medName
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  val saved=context.getSharedPreferences("mediyiyo_settings",Context.MODE_PRIVATE).getString("alarm_sound",null)
  val sound:Uri= saved?.let{Uri.parse(it)} ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
  // El URI forma parte del ID: al cambiar el tono se crea un canal nuevo, necesario en Android 8+ porque el sonido de un canal no puede modificarse después de crearlo.
  val channelId="mediyiyo_"+sound.toString().hashCode()
  if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){manager.createNotificationChannel(NotificationChannel(channelId,"Alarmas Mediyiyo",NotificationManager.IMPORTANCE_HIGH).apply{description="Avisos de medicamentos";enableVibration(true);setSound(sound,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())})}
  val pending=PendingIntent.getActivity(context,medId.hashCode(),Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  manager.notify(medId.hashCode(),NotificationCompat.Builder(context,channelId).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Hora de tu medicamento").setContentText("Corresponde tomar $medName").setSound(sound).setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_ALARM).setAutoCancel(true).setContentIntent(pending).build())
  AlarmScheduler.scheduleFromNow(context,medId,medName,hours)
 }
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action==Intent.ACTION_BOOT_COMPLETED)AlarmScheduler.restoreAll(context)}}
