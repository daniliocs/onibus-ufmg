package br.ufmg.morabus.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "MorabusLiveActivity")
class MorabusLiveActivityPlugin : Plugin() {

    companion object {
        const val CHANNEL_ID = "morabus_live_nowbar"
        const val CHANNEL_NAME = "Morabus Ao Vivo (Now Bar / Live Updates)"
        const val NOTIFICATION_ID = 999100
    }

    private var channelCreated = false

    private fun ensureChannel(context: Context) {
        if (channelCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Canal IMPORTANCE_LOW garante que atualizações contínuas em tempo real
        // NÃO vibrem e NÃO toquem som no celular, permitindo animação fluida no Now Bar / status bar
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Atividades ao vivo contínuas (Samsung Now Bar, Motorola e Xiaomi - Android 15/16/17)"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            vibrationPattern = null
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
        channelCreated = true
    }

    @PluginMethod
    fun updateLiveActivity(call: PluginCall) {
        val title = call.getString("title") ?: "🚌 Morabus em Trânsito"
        val message = call.getString("message") ?: "Acompanhando trajeto em tempo real"
        val subtext = call.getString("subtext") ?: "Morabus UFMG"
        val progress = call.getInt("progress") ?: -1

        val context = context ?: run {
            call.reject("Contexto Android indisponível")
            return
        }
        ensureChannel(context)

        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            } else null

            val smallIcon = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName).let {
                if (it != 0) it else android.R.drawable.ic_dialog_info
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subtext)
                .setOngoing(true) // Fundamental para permanecer no Now Bar / Live Activities
                .setOnlyAlertOnce(true) // Crucial: nunca vibra ou apita ao atualizar
                .setCategory(NotificationCompat.CATEGORY_PROGRESS) // Categoria exigida pelo Now Bar da Samsung e Android Live Updates
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .setAutoCancel(false)

            if (pendingIntent != null) {
                builder.setContentIntent(pendingIntent)
            }

            if (progress in 0..100) {
                builder.setProgress(100, progress, false)
            } else {
                builder.setProgress(0, 0, false)
            }

            val notification = builder.build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)

            val ret = JSObject().apply {
                put("success", true)
                put("active", true)
            }
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("Falha ao atualizar Live Activity: ${e.message}", e)
        }
    }

    @PluginMethod
    fun stopLiveActivity(call: PluginCall) {
        val context = context ?: run {
            call.reject("Contexto Android indisponível")
            return
        }
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            val ret = JSObject().apply {
                put("success", true)
                put("stopped", true)
            }
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("Falha ao parar Live Activity: ${e.message}", e)
        }
    }

    @PluginMethod
    fun isSupported(call: PluginCall) {
        val ret = JSObject().apply {
            put("supported", true)
            put("sdkInt", Build.VERSION.SDK_INT)
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            val mfgLower = Build.MANUFACTURER.lowercase()
            put("isSamsung", mfgLower.contains("samsung"))
            put("isXiaomi", mfgLower.contains("xiaomi") || mfgLower.contains("redmi") || mfgLower.contains("poco"))
            put("isMotorola", mfgLower.contains("motorola"))
        }
        call.resolve(ret)
    }
}
