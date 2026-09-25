package cl.kura.mediyiyo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class Medicine(val id:String, val name:String, val detail:String, val note:String?=null, val doses:Int=1)
data class DoseState(val checked:Boolean=false, val time:String?=null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MediyiyoApp(this) }
    }
}

private val meds = listOf(
    Medicine("omeprazol", "Omeprazol 20 mg", "1 cada 24 hrs.", "AYUNO"),
    Medicine("dapagliflozina", "Dapagliflozina 10 mg", "1 cada 24 hrs."),
    Medicine("aspirina", "Aspirina 100 mg", "1 cada 24 hrs.", "ALMUERZO"),
    Medicine("clopidogrel", "Clopidogrel 75 mg", "1 cada 24 hrs.", "ALMUERZO"),
    Medicine("atorvastatina", "Atorvastatina 80 mg", "1 cada 24 hrs."),
    Medicine("bisoprolol", "Bisoprolol 1,25 mg", "1 cada 12 hrs.", doses=2)
)

@Composable
fun MediyiyoApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("mediyiyo", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now() }
    val monday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val days = remember { (0..6).map { monday.plusDays(it.toLong()) } }
    val names = listOf("Lun","Mar","Mié","Jue","Vie","Sáb","Dom")
    val states = remember { mutableStateMapOf<String, DoseState>() }

    fun key(med: Medicine, date: LocalDate, dose:Int) = "${med.id}_${date}_$dose"

    LaunchedEffect(Unit) {
        meds.forEach { med -> days.forEach { date -> repeat(med.doses) { dose ->
            val k = key(med,date,dose)
            prefs.getString(k,null)?.let { states[k] = DoseState(true,it) }
        }}}
    }

    MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF2DD4BF),background=Color(0xFF07131F),surface=Color(0xFF102235))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF07131F)).verticalScroll(rememberScrollState()).padding(vertical=16.dp)) {
            Text("Semana actual", color=Color.White, fontWeight=FontWeight.Bold, fontSize=21.sp, modifier=Modifier.padding(horizontal=14.dp))
            Text("${days.first().dayOfMonth} – ${days.last().dayOfMonth}", color=Color(0xFF94A9BE), fontSize=14.sp, modifier=Modifier.padding(horizontal=14.dp, vertical=4.dp))
            Spacer(Modifier.height(8.dp))

            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal=8.dp)) {
                Column(Modifier.width(205.dp)) {
                    Header("Medicamento")
                    meds.forEach { MedicineCard(it) }
                }
                days.forEachIndexed { index,date ->
                    Column(Modifier.width(76.dp)) {
                        Header("${names[index]}\n${date.dayOfMonth}", date==today)
                        meds.forEach { med ->
                            Column(
                                Modifier.height(rowHeight(med)).fillMaxWidth().padding(2.dp)
                                    .background(Color(0xFF0D1D2D),RoundedCornerShape(10.dp)),
                                horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.SpaceEvenly
                            ) {
                                repeat(med.doses) { dose ->
                                    val k=key(med,date,dose)
                                    val state=states[k] ?: DoseState()
                                    DoseCheck(state) {
                                        if(!state.checked) {
                                            val time=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                            prefs.edit().putString(k,time).commit()
                                            states[k]=DoseState(true,time)
                                        } else {
                                            prefs.edit().remove(k).commit()
                                            states[k]=DoseState(false,null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Text("Toca el cuadro cuando tomes el medicamento. El ✓ y la hora quedan guardados automáticamente.",color=Color(0xFF91A6BA),fontSize=13.sp,modifier=Modifier.padding(16.dp))
        }
    }
}

private fun rowHeight(med:Medicine)=if(med.doses==2) 154.dp else 112.dp

@Composable private fun Header(text:String,today:Boolean=false) {
    Box(Modifier.height(64.dp).fillMaxWidth().padding(2.dp).background(if(today) Color(0xFF173D43) else Color(0xFF172A3D),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center) {
        Text(text,color=if(today) Color(0xFF5EEAD4) else Color.White,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center,fontSize=13.sp)
    }
}

@Composable private fun MedicineCard(med:Medicine) {
    Column(Modifier.height(rowHeight(med)).fillMaxWidth().padding(2.dp).background(Color(0xFF102235),RoundedCornerShape(10.dp)).padding(horizontal=12.dp),verticalArrangement=Arrangement.Center) {
        Text(med.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=15.sp)
        Text(med.detail,color=Color(0xFFA7B7C8),fontSize=12.sp)
        med.note?.let { Text(it,color=if(it=="AYUNO") Color(0xFF5EEAD4) else Color(0xFFFACB5A),fontWeight=FontWeight.Bold,fontSize=12.sp) }
        if(med.doses==2) Text("2 tomas diarias",color=Color(0xFFC4B5FD),fontSize=11.sp)
    }
}

@Composable private fun DoseCheck(state:DoseState,onClick:()->Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=3.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp)
                .background(if(state.checked) Color(0xFF19B995) else Color.Transparent,RoundedCornerShape(9.dp))
                .border(2.dp,if(state.checked) Color(0xFF5EEAD4) else Color(0xFF526B80),RoundedCornerShape(9.dp)),
            contentAlignment=Alignment.Center
        ) { Text(if(state.checked) "✓" else "",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.Bold) }
        Spacer(Modifier.height(2.dp))
        Text(state.time ?: "--:--",color=if(state.checked) Color.White else Color(0xFF526B80),fontSize=11.sp,fontWeight=if(state.checked) FontWeight.Bold else FontWeight.Normal)
    }
}
