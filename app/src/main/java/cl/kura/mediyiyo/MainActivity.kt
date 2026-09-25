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

data class Medicine(
    val id: String,
    val name: String,
    val detail: String,
    val note: String? = null,
    val doses: Int = 1,
    val intervalHours: Int = 24
)

data class DoseState(val checked: Boolean = false, val time: String? = null)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private val ringtonePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                getSharedPreferences("mediyiyo_settings", MODE_PRIVATE)
                    .edit().putString("alarm_sound", it.toString()).apply()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { MediyiyoApp(this) { pickSound() } }
    }

    private fun pickSound() {
        val saved = getSharedPreferences("mediyiyo_settings", MODE_PRIVATE)
            .getString("alarm_sound", null)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonido de alarma Mediyiyo")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            saved?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
            }
        }
        ringtonePicker.launch(intent)
    }
}

private val meds = listOf(
    Medicine("omeprazol", "Omeprazol 20 mg", "1 cada 24 hrs.", "AYUNO"),
    Medicine("dapagliflozina", "Dapagliflozina 10 mg", "1 cada 24 hrs."),
    Medicine("aspirina", "Aspirina 100 mg", "1 cada 24 hrs.", "ALMUERZO"),
    Medicine("clopidogrel", "Clopidogrel 75 mg", "1 cada 24 hrs.", "ALMUERZO"),
    Medicine("atorvastatina", "Atorvastatina 80 mg", "1 cada 24 hrs."),
    Medicine("bisoprolol", "Bisoprolol 1,25 mg", "1 cada 12 hrs.", doses = 2, intervalHours = 12)
)

@Composable
fun MediyiyoApp(context: Context, onPickSound: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("mediyiyo", Context.MODE_PRIVATE) }
    var weekOffset by remember { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset.toLong())
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val names = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val states = remember { mutableStateMapOf<String, DoseState>() }

    fun key(med: Medicine, date: LocalDate, dose: Int) = "${med.id}_${date}_$dose"

    LaunchedEffect(weekOffset) {
        states.clear()
        meds.forEach { med ->
            days.forEach { date ->
                repeat(med.doses) { dose ->
                    val k = key(med, date, dose)
                    prefs.getString(k, null)?.let { states[k] = DoseState(true, it) }
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2DD4BF),
            background = Color(0xFF07131F),
            surface = Color(0xFF102235)
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF07131F))
                .verticalScroll(rememberScrollState()).padding(vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { weekOffset-- }) { Text("‹") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calendario de tomas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${days.first()}  —  ${days.last()}", color = Color(0xFF94A9BE), fontSize = 12.sp)
                }
                Button(onClick = { weekOffset++ }) { Text("›") }
            }

            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { weekOffset = 0 }) { Text("HOY") }
                Button(onClick = onPickSound) { Text("🔔 Elegir sonido") }
            }

            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                Column(Modifier.width(205.dp)) {
                    Header("Medicamento")
                    meds.forEach { MedicineCard(it) }
                }

                days.forEachIndexed { index, date ->
                    Column(Modifier.width(76.dp)) {
                        Header("${names[index]}\n${date.dayOfMonth}", date == today)
                        meds.forEach { med ->
                            Column(
                                Modifier.height(rowHeight(med)).fillMaxWidth().padding(2.dp)
                                    .background(Color(0xFF0D1D2D), RoundedCornerShape(10.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(med.doses) { dose ->
                                    val k = key(med, date, dose)
                                    val state = states[k] ?: DoseState()
                                    DoseCheck(state) {
                                        if (!state.checked) {
                                            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                            prefs.edit().putString(k, time).commit()
                                            states[k] = DoseState(true, time)
                                            if (date == today) {
                                                AlarmScheduler.schedule(
                                                    context,
                                                    med.id,
                                                    med.name,
                                                    med.intervalHours,
                                                    System.currentTimeMillis()
                                                )
                                            }
                                        } else {
                                            prefs.edit().remove(k).commit()
                                            states[k] = DoseState()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "‹ › cambia de semana. Los registros anteriores quedan guardados por fecha. La alarma usa el sonido seleccionado.",
                color = Color(0xFF91A6BA),
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun rowHeight(med: Medicine) = if (med.doses == 2) 154.dp else 112.dp

@Composable
private fun Header(text: String, today: Boolean = false) {
    Box(
        Modifier.height(64.dp).fillMaxWidth().padding(2.dp)
            .background(if (today) Color(0xFF173D43) else Color(0xFF172A3D), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (today) Color(0xFF5EEAD4) else Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MedicineCard(med: Medicine) {
    Column(
        Modifier.height(rowHeight(med)).fillMaxWidth().padding(2.dp)
            .background(Color(0xFF102235), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(med.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(med.detail, color = Color(0xFFA7B7C8), fontSize = 12.sp)
        med.note?.let {
            Text(
                it,
                color = if (it == "AYUNO") Color(0xFF5EEAD4) else Color(0xFFFACB5A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        if (med.doses == 2) Text("2 tomas diarias", color = Color(0xFFC4B5FD), fontSize = 11.sp)
    }
}

@Composable
private fun DoseCheck(state: DoseState, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(44.dp)
                .background(if (state.checked) Color(0xFF19B995) else Color.Transparent, RoundedCornerShape(9.dp))
                .border(2.dp, if (state.checked) Color(0xFF5EEAD4) else Color(0xFF526B80), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (state.checked) "✓" else "", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
        Text(state.time ?: "--:--", color = if (state.checked) Color.White else Color(0xFF526B80), fontSize = 11.sp)
    }
}
