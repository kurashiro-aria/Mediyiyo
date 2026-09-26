package cl.kura.mediyiyo

import android.Manifest
import android.app.TimePickerDialog
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
import java.time.ZoneId
import java.time.ZonedDateTime
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
data class PendingDelete(val key: String, val medicine: Medicine)

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
        ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sonido de alarma Mediyiyo")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            saved?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
        })
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
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    val states = remember { mutableStateMapOf<String, DoseState>() }
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .plusWeeks(weekOffset.toLong())
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    fun doseKey(medicine: Medicine, date: LocalDate, dose: Int): String =
        "${medicine.id}_${date}_$dose"

    fun scheduleFromRecordedTime(medicine: Medicine, date: LocalDate, time: String) {
        if (date != today) return
        val localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        val recordedAt = ZonedDateTime.of(date, localTime, ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        AlarmScheduler.schedule(
            context,
            medicine.id,
            medicine.name,
            medicine.intervalHours,
            recordedAt
        )
    }

    fun editTime(key: String, medicine: Medicine, date: LocalDate, currentTime: String) {
        val old = LocalTime.parse(currentTime, DateTimeFormatter.ofPattern("HH:mm"))
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val newTime = String.format("%02d:%02d", hour, minute)
                prefs.edit().putString(key, newTime).apply()
                states[key] = DoseState(true, newTime)
                scheduleFromRecordedTime(medicine, date, newTime)
            },
            old.hour,
            old.minute,
            true
        ).show()
    }

    LaunchedEffect(weekOffset) {
        states.clear()
        meds.forEach { medicine ->
            days.forEach { date ->
                repeat(medicine.doses) { dose ->
                    val key = doseKey(medicine, date, dose)
                    prefs.getString(key, null)?.let { savedTime ->
                        states[key] = DoseState(true, savedTime)
                    }
                }
            }
        }
    }

    pendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("¿Desmarcar esta toma?") },
            text = {
                Text("Este registro indica que tomaste ${pending.medicine.name}. Si lo desmarcas, se eliminarán la confirmación y la hora guardada.")
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().remove(pending.key).apply()
                    states[pending.key] = DoseState()
                    pendingDelete = null
                }) { Text("Sí, desmarcar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") }
            }
        )
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2DD4BF),
            background = Color(0xFF07131F),
            surface = Color(0xFF102235)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07131F))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { weekOffset-- }) { Text("‹") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Calendario de tomas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${days.first()} — ${days.last()}", color = Color(0xFF94A9BE), fontSize = 12.sp)
                }
                Button(onClick = { weekOffset++ }) { Text("›") }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { weekOffset = 0 }) { Text("HOY") }
                Button(onClick = onPickSound) { Text("🔔 Elegir sonido") }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                Column(Modifier.width(205.dp)) {
                    Header("Medicamento")
                    meds.forEach { medicine -> MedicineCard(medicine) }
                }

                days.forEachIndexed { index, date ->
                    Column(Modifier.width(76.dp)) {
                        Header("${dayNames[index]}\n${date.dayOfMonth}", date == today)
                        meds.forEach { medicine ->
                            Column(
                                modifier = Modifier
                                    .height(rowHeight(medicine))
                                    .fillMaxWidth()
                                    .padding(2.dp)
                                    .background(Color(0xFF0D1D2D), RoundedCornerShape(10.dp)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                repeat(medicine.doses) { dose ->
                                    val key = doseKey(medicine, date, dose)
                                    val state = states[key] ?: DoseState()
                                    DoseCheck(
                                        state = state,
                                        onCheck = {
                                            if (!state.checked) {
                                                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                                                prefs.edit().putString(key, time).apply()
                                                states[key] = DoseState(true, time)
                                                scheduleFromRecordedTime(medicine, date, time)
                                            } else {
                                                pendingDelete = PendingDelete(key, medicine)
                                            }
                                        },
                                        onEditTime = {
                                            state.time?.let { editTime(key, medicine, date, it) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "Toca ✓ para confirmar. La hora queda editable en cualquier momento mientras la toma esté marcada. Al desmarcar se pedirá confirmación.",
                color = Color(0xFF91A6BA),
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun rowHeight(medicine: Medicine): androidx.compose.ui.unit.Dp =
    if (medicine.doses == 2) 154.dp else 112.dp

@Composable
private fun Header(text: String, isToday: Boolean = false) {
    Box(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .padding(2.dp)
            .background(if (isToday) Color(0xFF173D43) else Color(0xFF172A3D), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isToday) Color(0xFF5EEAD4) else Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun MedicineCard(medicine: Medicine) {
    Column(
        modifier = Modifier
            .height(rowHeight(medicine))
            .fillMaxWidth()
            .padding(2.dp)
            .background(Color(0xFF102235), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(medicine.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(medicine.detail, color = Color(0xFFA7B7C8), fontSize = 12.sp)
        medicine.note?.let { note ->
            Text(
                note,
                color = if (note == "AYUNO") Color(0xFF5EEAD4) else Color(0xFFFACB5A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        if (medicine.doses == 2) {
            Text("2 tomas diarias", color = Color(0xFFC4B5FD), fontSize = 11.sp)
        }
    }
}

@Composable
private fun DoseCheck(
    state: DoseState,
    onCheck: () -> Unit,
    onEditTime: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onCheck)
                .background(
                    if (state.checked) Color(0xFF19B995) else Color.Transparent,
                    RoundedCornerShape(9.dp)
                )
                .border(
                    2.dp,
                    if (state.checked) Color(0xFF5EEAD4) else Color(0xFF526B80),
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (state.checked) "✓" else "",
                color = Color.White,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = state.time ?: "--:--",
            color = if (state.checked) Color.White else Color(0xFF526B80),
            fontSize = 11.sp,
            modifier = if (state.checked) {
                Modifier.clickable(onClick = onEditTime).padding(3.dp)
            } else {
                Modifier.padding(3.dp)
            }
        )
    }
}
