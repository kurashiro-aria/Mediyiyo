package cl.kura.mediyiyo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class LauncherActivity : ComponentActivity() {
    private var requestedExactAlarmAccess = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        continueLaunch()
    }

    override fun onResume() {
        super.onResume()
        if (requestedExactAlarmAccess) {
            requestedExactAlarmAccess = false
            if (AlarmScheduler.canScheduleExact(this)) {
                AlarmScheduler.restoreAll(this)
            }
            openMain()
        }
    }

    private fun continueLaunch() {
        if (AlarmScheduler.canScheduleExact(this)) {
            AlarmScheduler.restoreAll(this)
            openMain()
            return
        }

        val settingsIntent = AlarmScheduler.exactAlarmSettingsIntent(this)
        if (settingsIntent != null) {
            requestedExactAlarmAccess = true
            startActivity(settingsIntent)
        } else {
            openMain()
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
