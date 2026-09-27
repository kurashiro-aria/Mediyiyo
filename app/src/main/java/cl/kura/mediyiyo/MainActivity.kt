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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import java.util.UUID

data class Medicine(val id:String,val name:String,val detail:String,val note:String?=null,val doses:Int=1,val info:String="")
data class DoseState(val checked:Boolean=false,val time:String?=null)
data class PendingDelete(val key:String,val medicine:Medicine)
data class TimeEdit(val key:String,val medicine:Medicine,val date:LocalDate,val dose:Int,val current:String)

class MainActivity:ComponentActivity(){
 private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 private val ringtonePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==RESULT_OK){@Suppress("DEPRECATION") val u=r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);u?.let{getSharedPreferences("mediyiyo_settings",MODE_PRIVATE).edit().putString("alarm_sound",it.toString()).apply()}}}
 override fun onCreate(s:Bundle?){super.onCreate(s);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MediyiyoApp(this){pickSound()}}}
 private fun pickSound(){val saved=getSharedPreferences("mediyiyo_settings",MODE_PRIVATE).getString("alarm_sound",null);ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply{putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE,"Sonido de alarma Mediyiyo");putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false);saved?.let{putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,Uri.parse(it))}})}
}

private val defaultMeds=listOf(
 Medicine("omeprazol","Omeprazol 20 mg","1 vez al día","AYUNO",info="Reduce la producción de ácido del estómago. Se usa, entre otras indicaciones, para reflujo, úlceras y protección gástrica cuando corresponde."),
 Medicine("dapagliflozina","Dapagliflozina 10 mg","1 vez al día",info="Ayuda a eliminar glucosa por la orina. Se utiliza en determinadas personas con diabetes tipo 2 y también puede indicarse para insuficiencia cardíaca o enfermedad renal crónica."),
 Medicine("aspirina","Aspirina 100 mg","1 vez al día","ALMUERZO",info="A dosis bajas disminuye la agregación de las plaquetas y puede indicarse para reducir la formación de coágulos en determinadas enfermedades cardiovasculares."),
 Medicine("clopidogrel","Clopidogrel 75 mg","1 vez al día","ALMUERZO",info="Es un antiagregante plaquetario. Reduce la capacidad de las plaquetas para agruparse y puede utilizarse para prevenir ciertos coágulos cardiovasculares."),
 Medicine("atorvastatina","Atorvastatina 80 mg","1 vez al día",info="Es una estatina. Disminuye principalmente el colesterol LDL y se utiliza para reducir el riesgo cardiovascular cuando está indicada."),
 Medicine("bisoprolol","Bisoprolol 1,25 mg","2 tomas diarias",doses=2,info="Es un betabloqueador. Disminuye la frecuencia y el trabajo del corazón y puede utilizarse en determinadas enfermedades cardíacas o para controlar la presión arterial."))

private fun customPrefs(c:Context)=c.getSharedPreferences("mediyiyo_custom_meds",Context.MODE_PRIVATE)
private fun loadCustomMeds(c:Context):List<Medicine>{val p=customPrefs(c);val ids=p.getStringSet("ids",emptySet())?:emptySet();return ids.mapNotNull{id->val n=p.getString("${id}_name",null)?:return@mapNotNull null;val d=p.getInt("${id}_doses",1).coerceIn(1,4);Medicine(id,n,if(d==1)"1 vez al día" else "$d tomas diarias",doses=d,info=p.getString("${id}_info","")?:"")}}
private fun saveCustomMed(c:Context,m:Medicine){val p=customPrefs(c);val ids=(p.getStringSet("ids",emptySet())?:emptySet()).toMutableSet().apply{add(m.id)};p.edit().putStringSet("ids",ids).putString("${m.id}_name",m.name).putInt("${m.id}_doses",m.doses).putString("${m.id}_info",m.info).apply()}
private fun orderedMeds(c:Context,custom:List<Medicine>):List<Medicine>{
    val hidden=c.getSharedPreferences("mediyiyo_hidden_meds",Context.MODE_PRIVATE)
        .getStringSet("ids",emptySet())?:emptySet()
    val all=(defaultMeds+custom).filterNot{it.id in hidden}
    val order=c.getSharedPreferences("mediyiyo_order",Context.MODE_PRIVATE)
        .getString("ids",null)?.split("|")?.filter{it.isNotBlank()}?:emptyList()
    return all.sortedBy{val i=order.indexOf(it.id);if(i<0)Int.MAX_VALUE else i}
}
private fun hideMedicine(c:Context,m:Medicine){
    val p=c.getSharedPreferences("mediyiyo_hidden_meds",Context.MODE_PRIVATE)
    val ids=(p.getStringSet("ids",emptySet())?:emptySet()).toMutableSet().apply{add(m.id)}
    p.edit().putStringSet("ids",ids).apply()
    val schedule=c.getSharedPreferences("mediyiyo_schedule",Context.MODE_PRIVATE).edit()
    repeat(m.doses){dose->
        val id="${m.id}_dose_$dose"
        AlarmScheduler.cancel(c,id)
        schedule.remove(id)
    }
    schedule.apply()
}
private fun saveOrder(c:Context,list:List<Medicine>){c.getSharedPreferences("mediyiyo_order",Context.MODE_PRIVATE).edit().putString("ids",list.joinToString("|"){it.id}).apply()}

@Composable fun MediyiyoApp(context:Context,onPickSound:()->Unit){
 val prefs=remember{context.getSharedPreferences("mediyiyo",Context.MODE_PRIVATE)}
 val schedulePrefs=remember{context.getSharedPreferences("mediyiyo_schedule",Context.MODE_PRIVATE)}
 val alarmPrefs=remember{context.getSharedPreferences("mediyiyo_alarms",Context.MODE_PRIVATE)}
 var custom by remember{mutableStateOf(loadCustomMeds(context))}
 var medicines by remember{mutableStateOf(orderedMeds(context,custom))}
 var currentDate by remember{mutableStateOf(LocalDate.now())}
 var pendingDelete by remember{mutableStateOf<PendingDelete?>(null)}
 var pendingRemoval by remember{mutableStateOf<Medicine?>(null)}
 var timeEdit by remember{mutableStateOf<TimeEdit?>(null)}
 var adding by remember{mutableStateOf(false)}
 var infoMed by remember{mutableStateOf<Medicine?>(null)}
 var reorder by remember{mutableStateOf(false)}
 val states=remember{mutableStateMapOf<String,DoseState>()}
 val schedules=remember{mutableStateMapOf<String,String>()}
 val fmt=DateTimeFormatter.ofPattern("HH:mm")
 val locale=Locale.forLanguageTag("es-CL")
 val dayLabel=currentDate.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",locale))
     .replaceFirstChar { it.titlecase(locale) }
 fun key(m:Medicine,d:LocalDate,dose:Int)="${m.id}_${d}_$dose"
 fun alarmId(m:Medicine,dose:Int)="${m.id}_dose_$dose"
 fun validTime(raw:String?)=raw?.let{runCatching{LocalTime.parse(it).format(fmt)}.getOrNull()}
 fun previousTime(m:Medicine,dose:Int):String?{
     val prefix="${m.id}_"
     val suffix="_$dose"
     return prefs.all.entries.asSequence()
         .filter{it.key.startsWith(prefix)&&it.key.endsWith(suffix)}
         .mapNotNull{entry->
             val date=runCatching{LocalDate.parse(entry.key.removePrefix(prefix).removeSuffix(suffix))}.getOrNull()
             val time=validTime(entry.value as? String)
             if(date!=null&&time!=null) date to time else null
         }.maxByOrNull{it.first}?.second
 }
 fun move(index:Int,delta:Int){val target=index+delta;if(target in medicines.indices){val list=medicines.toMutableList();val item=list.removeAt(index);list.add(target,item);medicines=list;saveOrder(context,list)}}
 LaunchedEffect(Unit){
     while(true){
         val now=ZonedDateTime.now()
         val midnight=now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
         delay(Duration.between(now,midnight).toMillis().coerceAtLeast(1000L))
         currentDate=LocalDate.now()
     }
 }
 LaunchedEffect(currentDate,medicines){
     states.clear()
     schedules.clear()
     medicines.forEach{m->
         repeat(m.doses){dose->
             val id=alarmId(m,dose)
             val planned=validTime(schedulePrefs.getString(id,null))
                 ?:validTime(alarmPrefs.getString("${id}_time",null))
                 ?:previousTime(m,dose)
             if(planned!=null){
                 schedules[id]=planned
                 schedulePrefs.edit().putString(id,planned).apply()
             }
             val k=key(m,currentDate,dose)
             prefs.getString(k,null)?.let{states[k]=DoseState(true,it)}
         }
     }
 }
 pendingDelete?.let{p->AlertDialog(onDismissRequest={pendingDelete=null},title={Text("¿Desmarcar esta toma?")},text={Text("Se quitará la confirmación de hoy para ${p.medicine.name}. La hora programada se conservará.")},confirmButton={Button(onClick={prefs.edit().remove(p.key).apply();states[p.key]=DoseState();pendingDelete=null}){Text("Sí, desmarcar")}},dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancelar")}})}
 pendingRemoval?.let{m->AlertDialog(
     onDismissRequest={pendingRemoval=null},
     title={Text("¿Quitar ${m.name}?")},
     text={Text("Se quitará de la lista y se cancelarán sus alarmas. Las tomas guardadas no se borrarán.")},
     confirmButton={Button(onClick={
         hideMedicine(context,m)
         medicines=medicines.filterNot{it.id==m.id}
         saveOrder(context,medicines)
         pendingRemoval=null
     }){Text("Sí, quitar")}},
     dismissButton={TextButton(onClick={pendingRemoval=null}){Text("Cancelar")}}
 )}
 timeEdit?.let{e->TypedTimeDialog(e.current,{timeEdit=null}){t->
     val id=alarmId(e.medicine,e.dose)
     schedulePrefs.edit().putString(id,t).apply()
     schedules[id]=t
     AlarmScheduler.scheduleFromPreviousDose(context,id,e.medicine.name,LocalDate.now().minusDays(1),LocalTime.parse(t,fmt))
     timeEdit=null
 }}
 infoMed?.let{m->MedicineInfoDialog(m,{infoMed=null}){infoMed=null;pendingRemoval=m}}
 if(adding)AddMedicineDialog({adding=false}){name,doses,info->saveCustomMed(context,Medicine("custom_${UUID.randomUUID()}",name,if(doses==1)"1 vez al día" else "$doses tomas diarias",doses=doses,info=info));custom=loadCustomMeds(context);medicines=orderedMeds(context,custom);adding=false}
 MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF2DD4BF),background=Color(0xFF07131F),surface=Color(0xFF102235))){Column(Modifier.fillMaxSize().background(Color(0xFF07131F)).verticalScroll(rememberScrollState()).padding(vertical=12.dp)){
  Column(Modifier.fillMaxWidth().padding(horizontal=12.dp)){
      Image(
          painter=painterResource(R.drawable.mediyiyo_brand_header),
          contentDescription="Mediyiyo, tomate las pastillas wn!",
          modifier=Modifier.fillMaxWidth().height(74.dp),
          alignment=Alignment.CenterStart,
          contentScale=ContentScale.Fit
      )
      Spacer(Modifier.height(6.dp))
      Text("Calendario de tomas",color=Color.White,fontWeight=FontWeight.Bold,fontSize=20.sp)
      Text(dayLabel,color=Color(0xFF5EEAD4),fontWeight=FontWeight.Bold,fontSize=16.sp)
  }
  Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.SpaceBetween){Row{Button(onClick={adding=true}){Text("＋ Medicamento")};Spacer(Modifier.width(5.dp));Button(onClick={reorder=!reorder}){Text(if(reorder)"✓ Orden" else "↕ Orden")};Spacer(Modifier.width(5.dp));Button(onClick=onPickSound){Text("🔔")}}}
  Row(Modifier.fillMaxWidth().padding(horizontal=8.dp)){
      Column(Modifier.weight(1f)){
          Header("Medicamento")
          medicines.forEachIndexed{i,m->MedicineCard(m,reorder,{infoMed=m},{move(i,-1)},{move(i,1)})}
      }
      Column(Modifier.width(112.dp)){
          Header("Hoy\n${currentDate.dayOfMonth}",true)
          medicines.forEach{m->
              Column(Modifier.height(rowHeight(m)).fillMaxWidth().padding(2.dp).background(Color(0xFF0D1D2D),RoundedCornerShape(10.dp)),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceEvenly){
                  repeat(m.doses){dose->
                      val k=key(m,currentDate,dose)
                      val id=alarmId(m,dose)
                      val checked=states[k]?.checked==true
                      DoseCheck(checked,schedules[id],{
                          if(!checked){
                              val now=LocalTime.now().format(fmt)
                              prefs.edit().putString(k,now).apply()
                              states[k]=DoseState(true,now)
                              if(schedules[id]==null){
                                  schedulePrefs.edit().putString(id,now).apply()
                                  schedules[id]=now
                                  AlarmScheduler.scheduleFromPreviousDose(context,id,m.name,currentDate,LocalTime.parse(now,fmt))
                              }
                          }else pendingDelete=PendingDelete(k,m)
                      },{
                          timeEdit=TimeEdit(k,m,currentDate,dose,schedules[id]?:LocalTime.now().format(fmt))
                      })
                  }
              }
          }
      }
  }
  Text(if(reorder)"Usa ▲ y ▼ para cambiar el orden. El historial y las alarmas no cambian." else "Toca el nombre para ver información o quitar el medicamento. Toca la hora para configurarla; se conserva al cambiar de día.",color=Color(0xFF91A6BA),fontSize=13.sp,modifier=Modifier.padding(16.dp))
 }}
}
@Composable private fun MedicineInfoDialog(m:Medicine,onDismiss:()->Unit,onRemove:()->Unit){
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text(m.name)},
        text={Column{
            Text(m.detail,fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(if(m.info.isBlank())"No hay una descripción guardada para este medicamento." else m.info)
            Spacer(Modifier.height(12.dp))
            Text("Información general. Sigue siempre la indicación y dosis entregadas por tu profesional de salud.",fontSize=12.sp)
        }},
        confirmButton={Button(onClick=onDismiss){Text("Cerrar")}},
        dismissButton={TextButton(onClick=onRemove){Text("Quitar")}}
    )
}
@Composable
private fun AddMedicineDialog(onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var doses by remember { mutableStateOf("1") }
    var info by remember { mutableStateOf("") }
    var selectedSuggestion by remember { mutableStateOf(false) }
    val suggestions = remember(name, selectedSuggestion) {
        if (selectedSuggestion) emptyList() else MedicineCatalog.search(name)
    }
    val n = doses.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar medicamento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        selectedSuggestion = false
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suggestions.isNotEmpty()) {
                    Text(
                        "Sugerencias del catálogo · toca para seleccionar",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        suggestions.forEach { medicine ->
                            TextButton(
                                onClick = {
                                    name = medicine.name
                                    info = medicine.info
                                    selectedSuggestion = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    medicine.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = doses,
                    onValueChange = { if (it.length <= 1) doses = it.filter(Char::isDigit) },
                    label = { Text("Tomas al día (1 a 4)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = info,
                    onValueChange = { info = it },
                    label = { Text("Descripción breve (opcional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && n != null && n in 1..4,
                onClick = { onSave(name.trim(), n!!, info.trim()) }
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
@Composable private fun TypedTimeDialog(current:String,onDismiss:()->Unit,onSave:(String)->Unit){var h by remember(current){mutableStateOf(current.substringBefore(":"))};var m by remember(current){mutableStateOf(current.substringAfter(":"))};val ph=h.toIntOrNull();val pm=m.toIntOrNull();val valid=ph!=null&&ph in 0..23&&pm!=null&&pm in 0..59;AlertDialog(onDismissRequest=onDismiss,title={Text("Cambiar hora")},text={Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(h,{if(it.length<=2)h=it.filter(Char::isDigit)},label={Text("Hora")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp));Text(" : ");OutlinedTextField(m,{if(it.length<=2)m=it.filter(Char::isDigit)},label={Text("Min")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp))}},confirmButton={Button(enabled=valid,onClick={onSave(String.format("%02d:%02d",ph,pm))}){Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})}
private fun rowHeight(m:Medicine)=if(m.doses>=2)(112+42*(m.doses-1)).dp else 112.dp
@Composable private fun Header(t:String,today:Boolean=false){Box(Modifier.height(64.dp).fillMaxWidth().padding(2.dp).background(if(today)Color(0xFF173D43)else Color(0xFF172A3D),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){Text(t,color=if(today)Color(0xFF5EEAD4)else Color.White,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,fontSize=13.sp)}}
@Composable private fun MedicineCard(m:Medicine,reorder:Boolean,onInfo:()->Unit,onUp:()->Unit,onDown:()->Unit){Row(Modifier.height(rowHeight(m)).fillMaxWidth().padding(2.dp).background(Color(0xFF102235),RoundedCornerShape(10.dp)).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f).clickable(enabled=!reorder,onClick=onInfo)){Text(m.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=14.sp);Text(m.detail,color=Color(0xFFA7B7C8),fontSize=11.sp);m.note?.let{Text(it,color=if(it=="AYUNO")Color(0xFF5EEAD4)else Color(0xFFFACB5A),fontWeight=FontWeight.Bold,fontSize=11.sp)}};if(reorder)Column{TextButton(onClick=onUp,contentPadding=PaddingValues(2.dp)){Text("▲")};TextButton(onClick=onDown,contentPadding=PaddingValues(2.dp)){Text("▼")}}}}
@Composable private fun DoseCheck(checked:Boolean,time:String?,onCheck:()->Unit,onEditTime:()->Unit){Column(Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(40.dp).clickable(onClick=onCheck).background(if(checked)Color(0xFF19B995)else Color.Transparent,RoundedCornerShape(9.dp)).border(2.dp,if(checked)Color(0xFF5EEAD4)else Color(0xFF526B80),RoundedCornerShape(9.dp)),contentAlignment=Alignment.Center){Text(if(checked)"✓"else"",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold)};Text(time?:"--:--",color=if(time!=null)Color.White else Color(0xFF526B80),fontSize=11.sp,modifier=Modifier.clickable(onClick=onEditTime).padding(2.dp))}}
