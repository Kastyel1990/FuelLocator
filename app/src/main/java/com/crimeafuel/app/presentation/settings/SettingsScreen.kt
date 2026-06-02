package com.crimeafuel.app.presentation.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.crimeafuel.app.domain.model.FuelType
import com.crimeafuel.app.worker.FuelMonitorWorker
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fuel_prefs", Context.MODE_PRIVATE) }
    
    var isMonitoringEnabled by remember {
        mutableStateOf(prefs.getBoolean("monitoring_enabled", false))
    }
    
    var selectedFuels by remember {
        mutableStateOf(prefs.getStringSet("selected_fuels", emptySet()) ?: emptySet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки уведомлений") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Уведомления о появлении топлива",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isMonitoringEnabled,
                    onCheckedChange = { checked ->
                        isMonitoringEnabled = checked
                        prefs.edit().putBoolean("monitoring_enabled", checked).apply()
                        if (checked) {
                            val workRequest = PeriodicWorkRequestBuilder<FuelMonitorWorker>(30, TimeUnit.MINUTES)
                                .build()
                            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                "FuelMonitorWorker",
                                ExistingPeriodicWorkPolicy.UPDATE,
                                workRequest
                            )
                        } else {
                            WorkManager.getInstance(context).cancelUniqueWork("FuelMonitorWorker")
                        }
                    }
                )
            }
            
            Text(
                text = "Раз в 30 минут приложение будет в фоновом режиме проверять появление выбранного топлива и пришлет пуш-уведомление.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            if (isMonitoringEnabled) {
                Text(
                    text = "Виды топлива для отслеживания:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn {
                    items(FuelType.entries) { fuelType ->
                        val isSelected = selectedFuels.contains(fuelType.name)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newSet = selectedFuels.toMutableSet()
                                    if (checked) newSet.add(fuelType.name)
                                    else newSet.remove(fuelType.name)
                                    
                                    selectedFuels = newSet
                                    prefs.edit().putStringSet("selected_fuels", newSet).apply()
                                }
                            )
                            Text(text = fuelType.displayName)
                        }
                    }
                }
            }
        }
    }
}
