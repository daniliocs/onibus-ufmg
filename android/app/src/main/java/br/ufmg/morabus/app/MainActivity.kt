package br.ufmg.morabus.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.ufmg.morabus.app.ui.MorabusMainApp
import br.ufmg.morabus.app.ui.theme.MorabusTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_DIAG = "morabus_diagnostics"
        private const val KEY_LAST_CRASH = "last_crash_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installCrashReporter()
        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            Log.w("Morabus", "enableEdgeToEdge fallback", e)
        }
        super.onCreate(savedInstanceState)

        // Inicializa preferências locais e inicia o Radar de Comunicados UFMG / FUMP / Clima
        br.ufmg.morabus.app.data.UserPreferencesRepository.init(applicationContext)
        br.ufmg.morabus.app.data.FirebaseRealtimeRepository.init(applicationContext)
        br.ufmg.morabus.app.data.CampusRadarEngine.startPeriodicRadar(applicationContext)
        br.ufmg.morabus.app.data.CampusRadarEngine.triggerImmediateCheck(applicationContext)
        br.ufmg.morabus.app.data.AppUpdateManager.checkForUpdates(applicationContext)

        val shortcutDest = intent?.getStringExtra("shortcut_destination")
        val initialRoute = when (shortcutDest) {
            "schedules" -> br.ufmg.morabus.app.ui.navigation.NavItem.Schedules.route
            "notices" -> br.ufmg.morabus.app.ui.navigation.NavItem.Notices.route
            "more" -> br.ufmg.morabus.app.ui.navigation.NavItem.More.route
            "map" -> br.ufmg.morabus.app.ui.navigation.NavItem.Map.route
            else -> br.ufmg.morabus.app.ui.navigation.NavItem.Home.route
        }

        val prefs = getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE)
        val savedCrash = prefs.getString(KEY_LAST_CRASH, null)

        setContent {
            var crashTrace by remember { mutableStateOf(savedCrash) }

            MorabusMainApp(initialRoute = initialRoute)

            if (!crashTrace.isNullOrBlank()) {
                MorabusTheme(darkTheme = true) {
                    AlertDialog(
                        onDismissRequest = {
                            prefs.edit().remove(KEY_LAST_CRASH).apply()
                            crashTrace = null
                        },
                        title = { Text("Diagnóstico de Erro Anterior") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = crashTrace ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Morabus Crash", crashTrace))
                                    Toast.makeText(this@MainActivity, "Erro copiado!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Copiar Erro")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    prefs.edit().remove(KEY_LAST_CRASH).apply()
                                    crashTrace = null
                                }
                            ) {
                                Text("Fechar")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        br.ufmg.morabus.app.data.FirebaseRealtimeRepository.startPolling()
        br.ufmg.morabus.app.data.CampusRadarEngine.triggerImmediateCheck(this)
    }

    override fun onStop() {
        super.onStop()
        // Economia de dados móveis e bateria quando a interface está em segundo plano
        br.ufmg.morabus.app.data.FirebaseRealtimeRepository.stopPolling()
    }

    private fun installCrashReporter() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()
                Log.e("MorabusCrash", "Uncaught exception in thread ${thread.name}", throwable)
                getSharedPreferences(PREFS_DIAG, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, stackTrace.take(4000))
                    .commit()
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
