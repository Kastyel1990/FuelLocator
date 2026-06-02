package com.crimeafuel.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.crimeafuel.app.domain.repository.StationRepository
import com.crimeafuel.app.domain.model.Availability
import com.crimeafuel.app.presentation.MainActivity
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class FuelMonitorWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val stationRepository: StationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("fuel_prefs", Context.MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("monitoring_enabled", false)
        if (!isMonitoringEnabled) return Result.success()

        val selectedFuels = prefs.getStringSet("selected_fuels", emptySet()) ?: emptySet()
        if (selectedFuels.isEmpty()) return Result.success()

        val lastNotified = prefs.getStringSet("last_notified", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        try {
            // Refresh from Firestore
            stationRepository.refresh()
            
            // Wait for DB to update and read the first emission
            val stations = stationRepository.getAllStations().firstOrNull() ?: return Result.success()

            val newAvailableFuels = mutableListOf<String>()
            val currentAvailable = mutableSetOf<String>()

            for (station in stations) {
                for (fuelStatus in station.fuelStatuses) {
                    val fuelType = fuelStatus.fuelType.name
                    if (selectedFuels.contains(fuelType) && fuelStatus.availability == Availability.FREE_SALE) {
                        val key = "${station.id}_$fuelType"
                        currentAvailable.add(key)
                        if (!lastNotified.contains(key)) {
                            newAvailableFuels.add("${station.network} (${station.address}): ${fuelStatus.fuelType.displayName}")
                            lastNotified.add(key)
                        }
                    }
                }
            }

            // Cleanup old notified that are no longer available
            lastNotified.retainAll(currentAvailable)
            prefs.edit().putStringSet("last_notified", lastNotified).apply()

            if (newAvailableFuels.isNotEmpty()) {
                showNotification(newAvailableFuels)
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun showNotification(messages: List<String>) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "fuel_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Уведомления о топливе", 
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val text = messages.take(3).joinToString("\n") + if (messages.size > 3) "\nи еще ${messages.size - 3}..." else ""

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon
            .setContentTitle("Появилось топливо!")
            .setContentText("На ${messages.size} заправках появилось отслеживаемое топливо")
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Find app icon if possible
        val appIconResId = appContext.applicationInfo.icon
        if (appIconResId != 0) {
            builder.setSmallIcon(appIconResId)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
