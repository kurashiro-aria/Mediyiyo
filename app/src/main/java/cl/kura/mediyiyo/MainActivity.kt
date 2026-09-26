package cl.kura.mediyiyo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class Medicine(val id:String,val name:String,val detail:String,val note:String?=null,val doses:Int=1)
data class DoseState(val checked:Boolean=false,val time:String?=null)
data class PendingDelete(val key:String,val medicine:Medicine)
data class TimeEdit(val key:String,val medicine:Medicine,val date:LocalDate,val dose:Int,val current:String)

class MainActivity:ComponentActivity(){
 private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 private val ringtonePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==RESULT_OK){@Suppress("DEPRECATION") val u=r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);u?.let{getSharedPreferences("mediyiyo_settings",MODE_PRIVATE).edit().putString("alarm_sound",it.toString()).apply()}}}
 override fun onCreate(s:Bundle?){super.onCreate(s);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MediyiyoApp(this){pickSound()}}}
 private fun pickSound(){val saved=getSharedPreferences("mediyiyo_settings",MODE_PRIVATE).getString("alarm_sound",null);ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply{putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Sonido de alarma Mediyiyo");putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);saved?.let{putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,Uri.parse(it))}})}
}

private val meds=listOf(
 Medicine("omeprazol","Omeprazol 20 mg","1 vez al día","AYUNO"),
 Medicine("dapagliflozina","Dapagliflozina 10 mg","1 vez al día"),
 Medicine("aspirina","Aspirina 100 mg","1 vez al día","ALMUERZO"),
 Medicine("clopidogrel","Clopidogrel 75 mg","1 vez al día","ALMUERZO"),
 Medicine("atorvastatina","Atorvastatina 80 mg","1 vez al día"),
 Medicine("bisoprolol","Bisoprolol 1,25 mg","2 tomas diarias",doses=2)
)

@Composable fun MediyiyoApp(context:Context,onPickSound:()->Unit){
 val prefs=remember{context.getSharedPreferences("mediyiyo",Context.MODE_PRIVATE)}
 var weekOffset by remember{mutableIntStateOf(0)};var pendingDelete by remember{mutableStateOf<PendingDelete?>(null)};var timeEdit by remember{mutableStateOf<TimeEdit?>(null)}
 val states=remember{mutableStateMapOf<String,DoseState>()};val today=LocalDate.now();val monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weekOffset.toLong());val days=(0..6).map{monday.plusDays(it.toLong())};val dayNames=listOf("Lun","Mar","Mié","Jue","Vie","Sáb","Dom")
 fun doseKey(m:Medicine,d:LocalDate,dose:Int)="${m.id}_${d}_$dose"
 fun alarmId(m:Medicine,dose:Int)="${m.id}_dose_$dose"
 fun reschedule(m:Medicine,date:LocalDate,dose:Int,time:String){AlarmScheduler.scheduleFromPreviousDose(context,alarmId(m,dose),m.name,date,LocalTime.parse(time,DateTimeFormatter.ofPattern("HH:mm")))}
 LaunchedEffect(weekOffset){states.clear();meds.forEach{m->days.forEach{d->repeat(m.doses){dose->val k=doseKey(m,d,dose);prefs.getString(k,null)?.let{states[k]=DoseState(true,it)}}}}}
 pendingDelete?.let{p->AlertDialog(onDismissRequest={pendingDelete=null},title={Text("¿Desmarcar esta toma?")},text={Text("Se eliminarán la confirmación y la hora registrada de ${p.medicine.name}.")},confirmButton={Button(onClick={prefs.edit().remove(p.key).apply();states[p.key]=DoseState();pendingDelete=null}){Text("Sí, desmarcar")}},dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancelar")}})}
 timeEdit?.let{e->TypedTimeDialog(e.current,onDismiss={timeEdit=null}){newTime->prefs.edit().putString(e.key,newTime).apply();states[e.key]=DoseState(true,newTime);reschedule(e.medicine,e.date,e.dose,newTime);timeEdit=null}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF2DD4BF),background=Color(0xFF07131F),surface=Color(0xFF102235))){Column(Modifier.fillMaxSize().background(Color(0xFF07131F)).verticalScroll(rememberScrollState()).padding(vertical=12.dp)){
  Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Button(onClick={weekOffset--}){Text("‹")};Column(horizontalAlignment=Alignment.CenterHorizontally){Text("Calendario de tomas",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp);Text("${days.first()} — ${days.last()}",color=Color(0xFF94A9BE),fontSize=12.sp)};Button(onClick={weekOffset++}){Text("›")}}
  Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={weekOffset=0}){Text("HOY")};Button(onClick=onPickSound){Text("🔔 Elegir sonido")}}
  Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)){Column(Modifier.width(205.dp)){Header("Medicamento");meds.forEach{MedicineCard(it)}};days.forEachIndexed{index,date->Column(Modifier.width(76.dp)){Header("${dayNames[index]}\n${date.dayOfMonth}",date==today);meds.forEach{m->Column(Modifier.height(rowHeight(m)).fillMaxWidth().padding(2.dp).background(Color(0xFF0D1D2D),RoundedCornerShape(10.dp)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){repeat(m.doses){dose->val k=doseKey(m,date,dose);val s=states[k]?:DoseState();DoseCheck(s,onCheck={if(!s.checked){val t=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));prefs.edit().putString(k,t).apply();states[k]=DoseState(true,t);reschedule(m,date,dose,t)}else pendingDelete=PendingDelete(k,m)},onEditTime={s.time?.let{timeEdit=TimeEdit(k,m,date,dose,it)}})}}}}}}
  Text("La alarma usa la hora de la toma anterior correspondiente. Toca la hora para escribir una nueva hora directamente.",color=Color(0xFF91A6BA),fontSize=13.sp,modifier=Modifier.padding(16.dp))
 }}
}

@Composable private fun TypedTimeDialog(current:String,onDismiss:()->Unit,onSave:(String)->Unit){
 var hour by remember(current){mutableStateOf(current.substringBefore(":"))};var minute by remember(current){mutableStateOf(current.substringAfter(":"))};val h=hour.toIntOrNull();val m=minute.toIntOrNull();val valid=h!=null&&h in 0..23&&m!=null&&m in 0..59
 AlertDialog(onDismissRequest=onDismiss,title={Text("Cambiar hora")},text={Column{Text("Escribe la hora (formato 24 horas)");Spacer(Modifier.height(10.dp));Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(hour,{if(it.length<=2)hour=it.filter(Char::isDigit)},label={Text("Hora")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp));Text(" : ",fontSize=24.sp);OutlinedTextField(minute,{if(it.length<=2)minute=it.filter(Char::isDigit)},label={Text("Min")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp))}}},confirmButton={Button(enabled=valid,onClick={onSave(String.format("%02d:%02d",h,m))}){Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
private fun rowHeight(m:Medicine)=if(m.doses==2)154.dp else 112.dp
@Composable private fun Header(t:String,today:Boolean=false){Box(Modifier.height(64.dp).fillMaxWidth().padding(2.dp).background(if(today)Color(0xFF173D43)else Color(0xFF172A3D),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text(t,color=if(today)Color(0xFF5EEAD4)else Color.White,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,fontSize=13.sp)}}
@Composable private fun MedicineCard(m:Medicine){Column(Modifier.height(rowHeight(m)).fillMaxWidth().padding(2.dp).background(Color(0xFF102235),RoundedCornerShape(10.dp)).padding(horizontal=12.dp),verticalArrangement=Arrangement.Center){Text(m.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp);Text(m.detail,color=Color(0xFFA7B7C8),fontSize=12.sp);m.note?.let{Text(it,color=if(it=="AYUNO")Color(0xFF5EEAD4)else Color(0xFFFACB5A),fontWeight=FontWeight.Bold,fontSize=12.sp)}}
@Composable private fun DoseCheck(s:DoseState,onCheck:()->Unit,onEditTime:()->Unit){Column(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(44.dp).clickable(onClick=onCheck).background(if(s.checked)Color(0xFF19B995)else Color.Transparent,RoundedCornerShape(9.dp)).border(2.dp,if(s.checked)Color(0xFF5EEAD4)else Color(0xFF526B80),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){Text(if(s.checked)"✓"else "",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold)};Text(s.time?:"--:--",color=if(s.checked)Color.White else Color(0xFF526B80),fontSize=11.sp,modifier=if(s.checked)Modifier.clickable(onClick=onEditTime).padding(3.dp)else Modifier.padding(3.dp))}}
