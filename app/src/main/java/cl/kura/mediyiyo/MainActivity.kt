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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
data class TimeEdit(val key:String,val medicine:Medicine,val date:LocalDate,val dose:Int,val current:String)
data class RecordEdit(val medicine:Medicine,val date:LocalDate,val dose:Int,val current:String)
data class DoseAction(val medicine:Medicine,val date:LocalDate,val dose:Int)

class MainActivity:ComponentActivity(){
 private val refreshSignal=mutableIntStateOf(0)
 private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 private val ringtonePicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){r->if(r.resultCode==RESULT_OK){@Suppress("DEPRECATION") val u=r.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);u?.let{getSharedPreferences("mediyiyo_settings",MODE_PRIVATE).edit().putString("alarm_sound",it.toString()).apply()}}}
 override fun onCreate(s:Bundle?){super.onCreate(s);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{MediyiyoApp(this,refreshSignal.intValue){pickSound()}}}
 override fun onResume(){super.onResume();refreshSignal.intValue++}
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
    val edits=c.getSharedPreferences("mediyiyo_med_overrides",Context.MODE_PRIVATE)
    val all=(defaultMeds+custom).filterNot{it.id in hidden}.map { m ->
        if (!edits.contains("${m.id}_name")) m else {
            val doses=edits.getInt("${m.id}_doses",m.doses).coerceIn(1,4)
            m.copy(name=edits.getString("${m.id}_name",m.name)?:m.name,
                doses=doses,detail=if(doses==1)"1 vez al día" else "$doses tomas diarias",
                info=edits.getString("${m.id}_info",m.info)?:m.info)
        }
    }
    val order=c.getSharedPreferences("mediyiyo_order",Context.MODE_PRIVATE)
        .getString("ids",null)?.split("|")?.filter{it.isNotBlank()}?:emptyList()
    return all.sortedBy{val i=order.indexOf(it.id);if(i<0)Int.MAX_VALUE else i}
}
private fun editMedicine(c:Context,m:Medicine,name:String,doses:Int,info:String){
    if(m.id.startsWith("custom_")) saveCustomMed(c,m.copy(name=name,doses=doses,info=info))
    else c.getSharedPreferences("mediyiyo_med_overrides",Context.MODE_PRIVATE).edit()
        .putString("${m.id}_name",name).putInt("${m.id}_doses",doses)
        .putString("${m.id}_info",info).apply()
    if(doses<m.doses){
        val schedule=c.getSharedPreferences("mediyiyo_schedule",Context.MODE_PRIVATE).edit()
        for(dose in doses until m.doses){
            val id="${m.id}_dose_$dose"
            AlarmScheduler.cancel(c,id)
            schedule.remove(id)
        }
        schedule.apply()
    }
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

@Composable fun MediyiyoApp(context:Context,refreshSignal:Int,onPickSound:()->Unit){
    val prefs=remember{DoseRecords.prefs(context)}
    val schedulePrefs=remember{context.getSharedPreferences("mediyiyo_schedule",Context.MODE_PRIVATE)}
    val alarmPrefs=remember{context.getSharedPreferences("mediyiyo_alarms",Context.MODE_PRIVATE)}
    var custom by remember{mutableStateOf(loadCustomMeds(context))}
    var medicines by remember{mutableStateOf(orderedMeds(context,custom))}
    var currentDate by remember{mutableStateOf(LocalDate.now())}
    var weekStart by remember{mutableStateOf(currentDate.with(DayOfWeek.MONDAY))}
    var pendingConfirm by remember{mutableStateOf<DoseAction?>(null)}
    var pendingUndo by remember{mutableStateOf<DoseAction?>(null)}
    var selectedTaken by remember{mutableStateOf<DoseAction?>(null)}
    var pendingRemoval by remember{mutableStateOf<Medicine?>(null)}
    var timeEdit by remember{mutableStateOf<TimeEdit?>(null)}
    var recordEdit by remember{mutableStateOf<RecordEdit?>(null)}
    var editingMedicine by remember{mutableStateOf<Medicine?>(null)}
    var adding by remember{mutableStateOf(false)}
    var infoMed by remember{mutableStateOf<Medicine?>(null)}
    var history by remember{mutableStateOf(false)}
    var reorder by remember{mutableStateOf(false)}
    val states=remember{mutableStateMapOf<String,DoseState>()}
    val schedules=remember{mutableStateMapOf<String,String>()}
    val fmt=remember{DateTimeFormatter.ofPattern("HH:mm")}
    val locale=remember{Locale.forLanguageTag("es-CL")}
    val dayLabel=currentDate.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",locale))
        .replaceFirstChar { it.titlecase(locale) }
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
    fun move(index:Int,delta:Int){
        val target=index+delta
        if(target in medicines.indices){
            val list=medicines.toMutableList()
            val item=list.removeAt(index)
            list.add(target,item)
            medicines=list
            saveOrder(context,list)
        }
    }
    LaunchedEffect(Unit){
        while(true){
            val now=ZonedDateTime.now()
            val midnight=now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now,midnight).toMillis().coerceAtLeast(1000L))
            if(currentDate==now.toLocalDate()){
                currentDate=LocalDate.now()
                weekStart=currentDate.with(DayOfWeek.MONDAY)
            }
        }
    }
    LaunchedEffect(currentDate,medicines,refreshSignal){
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
                DoseRecords.read(context,m.id,currentDate,dose)?.let{
                    states[DoseRecords.key(m.id,currentDate,dose)]=DoseState(true,it)
                }
            }
        }
    }
    DisposableEffect(prefs,currentDate,medicines){
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if(changed!=null){
                medicines.forEach{m->repeat(m.doses){dose->
                    val key=DoseRecords.key(m.id,currentDate,dose)
                    if(changed==key){
                        val time=DoseRecords.read(context,m.id,currentDate,dose)
                        states[key]=if(time==null)DoseState() else DoseState(true,time)
                    }
                }}
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose{prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }

    val slots=DoseTimeline.chronological(medicines.flatMapIndexed{index,m->
        (0 until m.doses).map{dose->DoseSlot(m.id,dose,schedules[alarmId(m,dose)],index)}
    })
    MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF1166C9),background=Color(0xFFF9FBFD),surface=Color.White)){
        pendingConfirm?.let{a->AlertDialog(
            onDismissRequest={pendingConfirm=null},
            title={Text("¿Confirmar esta toma?")},
            text={Text("${a.medicine.name} · toma ${a.dose+1}. Se guardará la hora real de ahora.")},
            confirmButton={Button(onClick={
                val now=LocalTime.now().format(fmt)
                DoseRecords.confirm(context,a.medicine.id,a.date,a.dose,now)
                states[DoseRecords.key(a.medicine.id,a.date,a.dose)]=DoseState(true,now)
                val id=alarmId(a.medicine,a.dose)
                if(schedules[id]==null){
                    schedulePrefs.edit().putString(id,now).apply()
                    schedules[id]=now
                    AlarmScheduler.scheduleFromPreviousDose(context,id,a.medicine.name,LocalDate.now().minusDays(1),LocalTime.parse(now,fmt))
                }
                pendingConfirm=null
            }){Text("Confirmar")}},
            dismissButton={TextButton(onClick={pendingConfirm=null}){Text("Cancelar")}}
        )}
        selectedTaken?.let{a->
            val time=states[DoseRecords.key(a.medicine.id,a.date,a.dose)]?.time
            AlertDialog(onDismissRequest={selectedTaken=null},
                title={Text("Toma registrada")},
                text={Text("${a.medicine.name} · ${a.date}\nHora real: ${time?:"--:--"}")},
                confirmButton={TextButton(onClick={selectedTaken=null}){Text("Cerrar")}},
                dismissButton={Row{
                    TextButton(onClick={recordEdit=RecordEdit(a.medicine,a.date,a.dose,time?:LocalTime.now().format(fmt));selectedTaken=null}){Text("Editar registro")}
                    TextButton(onClick={pendingUndo=a;selectedTaken=null}){Text("Deshacer")}
                }}
            )
        }
        pendingUndo?.let{a->AlertDialog(onDismissRequest={pendingUndo=null},
            title={Text("¿Deshacer esta toma?")},
            text={Text("Se quitará el registro de ${a.medicine.name} del ${a.date}. La alarma programada se conservará.")},
            confirmButton={Button(onClick={
                DoseRecords.undo(context,a.medicine.id,a.date,a.dose)
                states[DoseRecords.key(a.medicine.id,a.date,a.dose)]=DoseState()
                pendingUndo=null
            }){Text("Sí, deshacer")}},
            dismissButton={TextButton(onClick={pendingUndo=null}){Text("Cancelar")}}
        )}
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
        recordEdit?.let{e->TypedTimeDialog(e.current,{recordEdit=null}){t->
            DoseRecords.confirm(context,e.medicine.id,e.date,e.dose,t)
            states[DoseRecords.key(e.medicine.id,e.date,e.dose)]=DoseState(true,t)
            recordEdit=null
        }}
        infoMed?.let{m->MedicineInfoDialog(m,{infoMed=null},
            {infoMed=null;editingMedicine=m},
            {infoMed=null;pendingRemoval=m})}
        if(adding)AddMedicineDialog({adding=false}){name,doses,info->
            saveCustomMed(context,Medicine("custom_${UUID.randomUUID()}",name,if(doses==1)"1 vez al día" else "$doses tomas diarias",doses=doses,info=info))
            custom=loadCustomMeds(context)
            medicines=orderedMeds(context,custom)
            adding=false
        }
        editingMedicine?.let{m->AddMedicineDialog({editingMedicine=null},m){name,doses,info->
            editMedicine(context,m,name,doses,info)
            custom=loadCustomMeds(context)
            medicines=orderedMeds(context,custom)
            editingMedicine=null
        }}
        if(history)HistoryDialog(prefs,defaultMeds+custom,{history=false}){date->
            currentDate=date
            weekStart=date.with(DayOfWeek.MONDAY)
            history=false
        }

        Column(Modifier.fillMaxSize().background(Color(0xFFF9FBFD)).safeDrawingPadding()){
            BrandHeader()
            Text("Calendario de tomas",color=Color(0xFF15273C),fontWeight=FontWeight.Bold,
                fontSize=20.sp,modifier=Modifier.padding(start=20.dp,top=6.dp))
            Text(dayLabel,color=Color(0xFF1166C9),fontSize=15.sp,
                modifier=Modifier.padding(start=20.dp,bottom=8.dp))
            CalendarStrip(weekStart,currentDate,
                onPrevious={weekStart=weekStart.minusWeeks(1)},
                onNext={weekStart=weekStart.plusWeeks(1)},
                onSelect={currentDate=it})
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp,vertical=8.dp),
                verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={adding=true}){Text("＋ Medicamento")}
                TextButton(onClick={reorder=!reorder}){Text(if(reorder)"Listo" else "↕ Orden")}
                TextButton(onClick={history=true}){Text("Historial")}
                TextButton(onClick=onPickSound){Text("🔔 Sonido")}
            }
            if(slots.isEmpty()){
                Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){
                    Text("Agrega un medicamento para comenzar",color=Color(0xFF526171))
                }
            }else LazyColumn(
                modifier=Modifier.fillMaxWidth().weight(1f),
                contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=24.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)
            ){
                items(slots,key={"${it.medicineId}_${it.dose}"}){slot->
                    val m=medicines.first{it.id==slot.medicineId}
                    val k=DoseRecords.key(m.id,currentDate,slot.dose)
                    DoseCard(m,slot.dose,slot.plannedTime,states[k]?.time,
                        reorder,
                        onTap={if(states[k]?.checked==true) selectedTaken=DoseAction(m,currentDate,slot.dose)
                            else pendingConfirm=DoseAction(m,currentDate,slot.dose)},
                        onSchedule={timeEdit=TimeEdit(k,m,currentDate,slot.dose,slot.plannedTime?:LocalTime.now().format(fmt))},
                        onDetails={infoMed=m},
                        onUp={move(slot.medicineOrder,-1)},
                        onDown={move(slot.medicineOrder,1)})
                }
            }
        }
    }
}
@Composable private fun BrandHeader(){
    Row(Modifier.fillMaxWidth().padding(start=18.dp,end=18.dp,top=12.dp,bottom=2.dp),
        verticalAlignment=Alignment.CenterVertically){
        Image(painter=painterResource(R.drawable.ic_mediyiyo),
            contentDescription="Cápsula Mediyiyo",modifier=Modifier.size(64.dp),
            contentScale=ContentScale.Fit)
        Spacer(Modifier.width(8.dp))
        Column{
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Medi",color=Color(0xFF0869DD),fontWeight=FontWeight.ExtraBold,fontSize=29.sp)
                Text("yiyo",color=Color(0xFF05BFA9),fontWeight=FontWeight.ExtraBold,fontSize=29.sp)
            }
            Text("tomate las pastillas wn!",color=Color(0xFF34465A),fontSize=13.sp)
        }
    }
}

@Composable private fun CalendarStrip(
    weekStart:LocalDate,selected:LocalDate,
    onPrevious:()->Unit,onNext:()->Unit,onSelect:(LocalDate)->Unit
){
    val locale=Locale.forLanguageTag("es-CL")
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
        TextButton(onClick=onPrevious,contentPadding=PaddingValues(0.dp),modifier=Modifier.width(28.dp)){
            Text("‹",fontSize=30.sp)
        }
        repeat(7){offset->
            val date=weekStart.plusDays(offset.toLong())
            val active=date==selected
            Column(Modifier.weight(1f).padding(horizontal=1.dp)
                .background(if(active)Color(0xFFD9EBFF)else Color.Transparent,RoundedCornerShape(14.dp))
                .clickable{onSelect(date)}.padding(vertical=9.dp),
                horizontalAlignment=Alignment.CenterHorizontally){
                Text(date.format(DateTimeFormatter.ofPattern("EEEEE",locale)).uppercase(locale),
                    color=if(active)Color(0xFF115DB3)else Color(0xFF677788),fontSize=12.sp)
                Text("${date.dayOfMonth}",color=Color(0xFF15273C),
                    fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,fontSize=18.sp)
            }
        }
        TextButton(onClick=onNext,contentPadding=PaddingValues(0.dp),modifier=Modifier.width(28.dp)){
            Text("›",fontSize=30.sp)
        }
    }
}

@Composable private fun DoseCard(
    medicine:Medicine,dose:Int,planned:String?,actual:String?,reorder:Boolean,
    onTap:()->Unit,onSchedule:()->Unit,onDetails:()->Unit,onUp:()->Unit,onDown:()->Unit
){
    val completed=actual!=null
    Row(Modifier.fillMaxWidth().heightIn(min=112.dp)
        .background(if(completed)Color(0xFFD5F2D8)else Color(0xFFF0F2F5),RoundedCornerShape(22.dp))
        .clickable(onClick=onTap).padding(horizontal=14.dp,vertical=14.dp),
        verticalAlignment=Alignment.CenterVertically){
        Box(Modifier.size(46.dp).background(Color.White,RoundedCornerShape(14.dp)),
            contentAlignment=Alignment.Center){
            Image(painter=painterResource(R.drawable.ic_mediyiyo),contentDescription=null,
                modifier=Modifier.size(38.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)){
            Text(medicine.name,color=Color(0xFF172536),fontWeight=FontWeight.Bold,fontSize=16.sp,
                lineHeight=19.sp)
            Text(if(medicine.doses>1)"Toma ${dose+1} · ${medicine.detail}" else medicine.detail,
                color=Color(0xFF39485A),fontSize=12.sp)
            medicine.note?.let{Text(it,color=Color(0xFF276E63),fontSize=11.sp)}
            if(medicine.info.isNotBlank()) Text(medicine.info,color=Color(0xFF536273),
                maxLines=2,fontSize=11.sp,lineHeight=14.sp)
            if(completed) Text("Tomado a las $actual",color=Color(0xFF216642),fontSize=11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment=Alignment.End){
            Text(planned?:"--:--",color=Color(0xFF15273C),fontSize=20.sp,
                fontWeight=FontWeight.Bold,modifier=Modifier.clickable(onClick=onSchedule))
            Text(if(planned==null)"Configurar" else "Editar hora",color=Color(0xFF476277),
                fontSize=10.sp,modifier=Modifier.clickable(onClick=onSchedule))
        }
        if(reorder) Column{
            TextButton(onClick=onUp,contentPadding=PaddingValues(0.dp)){Text("▲")}
            TextButton(onClick=onDown,contentPadding=PaddingValues(0.dp)){Text("▼")}
        } else Text("›",color=Color(0xFF687989),fontSize=28.sp,
            modifier=Modifier.clickable(onClick=onDetails).padding(start=6.dp))
    }
}

@Composable private fun HistoryDialog(
    prefs:android.content.SharedPreferences,medicines:List<Medicine>,
    onDismiss:()->Unit,onSelect:(LocalDate)->Unit
){
    val expression=Regex("^(.+)_(\\d{4}-\\d{2}-\\d{2})_(\\d+)$")
    val records=prefs.all.entries.mapNotNull{(key,value)->
        val match=expression.matchEntire(key)?:return@mapNotNull null
        val date=runCatching{LocalDate.parse(match.groupValues[2])}.getOrNull()?:return@mapNotNull null
        Triple(date,match.groupValues[1] to match.groupValues[3].toIntOrNull(),value as? String)
    }.filter{it.third!=null}.sortedWith(compareByDescending<Triple<LocalDate,Pair<String,Int?>,String?>>{it.first}
        .thenBy{it.second.first}.thenBy{it.second.second})
    AlertDialog(onDismissRequest=onDismiss,title={Text("Historial de tomas")},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){
            if(records.isEmpty())Text("Todavía no hay tomas registradas.")
            records.forEach{(date,slot,time)->
                val name=medicines.firstOrNull{it.id==slot.first}?.name?:slot.first
                TextButton(onClick={onSelect(date)},modifier=Modifier.fillMaxWidth()){
                    Text("$date · $name · toma ${(slot.second?:0)+1} · $time",
                        modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Start)
                }
            }
        }},confirmButton={TextButton(onClick=onDismiss){Text("Cerrar")}})
}

@Composable private fun MedicineInfoDialog(m:Medicine,onDismiss:()->Unit,onEdit:()->Unit,onRemove:()->Unit){
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
        confirmButton={TextButton(onClick=onDismiss){Text("Cerrar")}},
        dismissButton={Row{
            TextButton(onClick=onEdit){Text("Editar")}
            TextButton(onClick=onRemove){Text("Quitar")}
        }}
    )
}
@Composable
private fun AddMedicineDialog(onDismiss: () -> Unit, initial:Medicine?=null, onSave: (String, Int, String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name?:"") }
    var doses by remember(initial) { mutableStateOf(initial?.doses?.toString()?:"1") }
    var info by remember(initial) { mutableStateOf(initial?.info?:"") }
    var selectedSuggestion by remember { mutableStateOf(false) }
    val suggestions = remember(name, selectedSuggestion) {
        if (selectedSuggestion) emptyList() else MedicineCatalog.search(name)
    }
    val n = doses.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(initial==null)"Agregar medicamento" else "Editar medicamento") },
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
            ) { Text(if(initial==null)"Agregar" else "Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
@Composable private fun TypedTimeDialog(current:String,onDismiss:()->Unit,onSave:(String)->Unit){var h by remember(current){mutableStateOf(current.substringBefore(":"))};var m by remember(current){mutableStateOf(current.substringAfter(":"))};val ph=h.toIntOrNull();val pm=m.toIntOrNull();val valid=ph!=null&&ph in 0..23&&pm!=null&&pm in 0..59;AlertDialog(onDismissRequest=onDismiss,title={Text("Cambiar hora")},text={Row(verticalAlignment=Alignment.CenterVertically){OutlinedTextField(h,{if(it.length<=2)h=it.filter(Char::isDigit)},label={Text("Hora")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp));Text(" : ");OutlinedTextField(m,{if(it.length<=2)m=it.filter(Char::isDigit)},label={Text("Min")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.width(100.dp))}},confirmButton={Button(enabled=valid,onClick={onSave(String.format("%02d:%02d",ph,pm))}){Text("Guardar")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})}
