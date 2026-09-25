package cl.kura.mediyiyo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MediyiyoApp(this) }
    }
}

data class Medicine(val id:String, val name:String, val detail:String, val note:String?=null, val dose:Int=1)

private val meds = listOf(
    Medicine("omeprazol", "Omeprazol 20 mg", "1 cada 24 hrs. durante 3 meses", "Ayuno"),
    Medicine("dapagliflozina", "Dapagliflozina 10 mg", "1 cada 24 hrs. durante 3 meses"),
    Medicine("aspirina", "Aspirina 100 mg", "1 cada 24 hrs. durante 3 meses", "Almuerzo"),
    Medicine("clopidogrel", "Clopidogrel 75 mg", "1 cada 24 hrs. durante 3 meses", "Almuerzo"),
    Medicine("atorvastatina", "Atorvastatina 80 mg", "1 cada 24 hrs. durante 3 meses"),
    Medicine("bisoprolol", "Bisoprolol 1,25 mg", "1 cada 12 hrs. durante 3 meses", dose=2)
)

@Composable
fun MediyiyoApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("mediyiyo", Context.MODE_PRIVATE) }
    var refresh by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val dayNames = listOf("Lun","Mar","Mié","Jue","Vie","Sáb","Dom")

    MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF20D6A4), background=Color(0xFF081522), surface=Color(0xFF132438))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF081522)).padding(12.dp).verticalScroll(rememberScrollState())) {
            Text("${days.first().dayOfMonth} - ${days.last().dayOfMonth}  •  Semana actual", color=Color.White, fontSize=20.sp, fontWeight=FontWeight.Bold, modifier=Modifier.padding(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(220.dp)) {
                    HeaderCell("Medicamento", 70)
                    meds.forEach { med -> MedicineInfo(med) }
                }
                days.forEachIndexed { index, date ->
                    Column(Modifier.width(78.dp)) {
                        HeaderCell("${dayNames[index]}\n${date.dayOfMonth}",70)
                        meds.forEach { med ->
                            Column(Modifier.height(if(med.dose==2) 154.dp else 112.dp).fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.SpaceEvenly) {
                                repeat(med.dose) { doseIndex ->
                                    val key = "${med.id}_${date}_${doseIndex}"
                                    val saved = prefs.getString(key, null)
                                    DoseCheck(saved) {
                                        if(saved == null) {
                                            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                            prefs.edit().putString(key,time).apply()
                                        } else prefs.edit().remove(key).apply()
                                        refresh++
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("✓ Toca una casilla al tomar el medicamento. La hora se guarda automáticamente en este dispositivo.", color=Color(0xFF9FB2C8), fontSize=13.sp, modifier=Modifier.padding(8.dp))
        }
    }
}

@Composable private fun HeaderCell(text:String, height:Int) { Box(Modifier.height(height.dp).fillMaxWidth().padding(3.dp).background(Color(0xFF1A2D46), RoundedCornerShape(14.dp)), contentAlignment=Alignment.Center) { Text(text, color=Color.White, fontWeight=FontWeight.Bold) } }

@Composable private fun MedicineInfo(med:Medicine) {
    Column(Modifier.height(if(med.dose==2)154.dp else 112.dp).fillMaxWidth().padding(3.dp).background(Color(0xFF132438),RoundedCornerShape(14.dp)).padding(12.dp), verticalArrangement=Arrangement.Center) {
        Text(med.name,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)
        Text(med.detail,color=Color(0xFFAFBED0),fontSize=12.sp)
        med.note?.let { Spacer(Modifier.height(5.dp)); Text(it,color=if(it=="Ayuno") Color(0xFF64E0AD) else Color(0xFFFFC94A),fontWeight=FontWeight.Bold) }
        if(med.dose==2) Text("Toma 1 / Toma 2",color=Color(0xFFC4B5FD),fontSize=12.sp)
    }
}

@Composable private fun DoseCheck(time:String?, onClick:()->Unit) {
    Column(horizontalAlignment=Alignment.CenterHorizontally, modifier=Modifier.clickable { onClick() }.padding(2.dp)) {
        Box(Modifier.size(48.dp).background(if(time!=null) Color(0xFF20D6A4) else Color(0xFF0C1928),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) { Text(if(time!=null) "✓" else "□",color=if(time!=null) Color.White else Color(0xFF8FA5BF),fontSize=28.sp,fontWeight=FontWeight.Bold) }
        if(time!=null) Text(time,color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold)
    }
}
