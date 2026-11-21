package com.example.smartaccidentdetection.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartaccidentdetection.util.DateTimeUtils
import com.example.smartaccidentdetection.util.GeoUtils
import com.example.smartaccidentdetection.data.repository.AppRepository
import com.example.smartaccidentdetection.ui.components.AccidentCountdownBanner
import com.example.smartaccidentdetection.ui.components.ChipVariant
import com.example.smartaccidentdetection.ui.components.MetricCard
import com.example.smartaccidentdetection.ui.components.StatusChip
import com.example.smartaccidentdetection.ui.theme.DangerRed
import com.example.smartaccidentdetection.ui.theme.OnDangerRedContainer
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue
import com.example.smartaccidentdetection.ui.theme.SafetyGreen
import com.example.smartaccidentdetection.ui.theme.WarningAmber

@Composable
fun DashboardScreen(
    repository: AppRepository,
    onNavigateToTracking: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val deviceStatus by repository.deviceStatus.collectAsState()
    val currentLocation by repository.currentLocation.collectAsState()
    val userProfile by repository.userProfile.collectAsState()
    val contacts by repository.contacts.collectAsState()
    val alerts by repository.alerts.collectAsState()
    val routePoints by repository.routePoints.collectAsState()
    val countdown by repository.accidentCountdown.collectAsState()
    val isDemoMode by repository.isDemoMode.collectAsState()

    val primaryContact = contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Smart Accident Detection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val vModel = if (!userProfile.vehicleModel.isNullOrBlank() && userProfile.vehicleModel != "null") userProfile.vehicleModel else "Two-Wheeler (ESP32)"
                val vNumber = if (!userProfile.vehicleNumber.isNullOrBlank() && userProfile.vehicleNumber != "null") userProfile.vehicleNumber else "ACCIDENT-001"
                Text(
                    text = "$vModel • $vNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusChip(
                text = if (deviceStatus.isOnline) "ONLINE" else "OFFLINE",
                variant = if (deviceStatus.isOnline) ChipVariant.SUCCESS else ChipVariant.DANGER
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Priority Accident Countdown Banner (if accident detected)
        if (countdown != null) {
            AccidentCountdownBanner(
                secondsRemaining = countdown!!,
                onCancelClick = { repository.cancelSimulatedAccident() }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Real-Time Hardware & Cloud Connectivity Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (deviceStatus.isOnline) SafetyGreen else WarningAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Live Supabase Cloud Database",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hardware: ${deviceStatus.deviceId} • Fix: ${currentLocation.locationSource}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusChip(
                    text = if (deviceStatus.isOnline) "ONLINE" else "STANDBY",
                    variant = if (deviceStatus.isOnline) ChipVariant.SUCCESS else ChipVariant.WARNING
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real Hardware Metric Cards Grid (2x2) with Equal Height
        val realDistanceKm = remember(routePoints) { GeoUtils.calculateTotalDistanceKm(routePoints) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Speed",
                value = String.format("%.1f", currentLocation.speedKmh),
                unit = "km/h",
                subtitle = "Hardware GPS Speed",
                icon = Icons.Default.Speed,
                accentColor = PrimaryBlue,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            MetricCard(
                title = "Distance",
                value = String.format("%.2f", realDistanceKm),
                unit = "km",
                subtitle = "Route Path (${routePoints.size} pts)",
                icon = Icons.Default.Navigation,
                accentColor = SafetyGreen,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Satellites",
                value = "${currentLocation.satellites}",
                unit = "Sats",
                subtitle = if (currentLocation.satellites >= 3) "NEO-6M Satellite Lock" else "Searching Satellite Fix",
                icon = Icons.Default.SatelliteAlt,
                accentColor = PrimaryBlue,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            MetricCard(
                title = "Cloud Sync",
                value = if (deviceStatus.isOnline) "Active" else "Standby",
                unit = "10s",
                subtitle = "Real-Time Polling",
                icon = Icons.Default.Cloud,
                accentColor = if (deviceStatus.isOnline) SafetyGreen else WarningAmber,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Vehicle Location Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToTracking() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Vehicle Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isFixRecent = remember(currentLocation.createdAt) {
                        val parsedDate = com.example.smartaccidentdetection.util.DateTimeUtils.parseToDate(currentLocation.createdAt ?: "")
                        if (parsedDate != null) {
                            (System.currentTimeMillis() - parsedDate.time) < 60000
                        } else false
                    }

                    StatusChip(
                        text = if (isFixRecent) "LIVE GPS" else "LAST KNOWN FIX",
                        variant = if (isFixRecent) ChipVariant.SUCCESS else ChipVariant.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Coordinates: ${String.format("%.6f", currentLocation.latitude)}, ${String.format("%.6f", currentLocation.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val liveLastUpdated = remember(currentLocation.createdAt, currentLocation.timestamp) {
                    if (!currentLocation.createdAt.isNullOrBlank()) {
                        DateTimeUtils.formatToLocalDateTime(currentLocation.createdAt)
                    } else if (!deviceStatus.lastSeen.isNullOrBlank() && deviceStatus.lastSeen != "Just now") {
                        DateTimeUtils.formatToLocalDateTime(deviceStatus.lastSeen)
                    } else {
                        DateTimeUtils.formatToLocalDateTime(currentLocation.timestamp.toString())
                    }
                }

                Text(
                    text = "Last updated: $liveLastUpdated • Altitude: ${currentLocation.altitudeM.toInt()} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToTracking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accident & Alerts Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAlerts() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (alerts.isNotEmpty()) DangerRed else SafetyGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Accident & Alert Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    StatusChip(
                        text = "${alerts.size} Alerts",
                        variant = if (alerts.isNotEmpty()) ChipVariant.DANGER else ChipVariant.SUCCESS
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (alerts.isNotEmpty()) {
                    val latestAlert = alerts.first()
                    Text(
                        text = "Latest: ${latestAlert.detectionType} (${latestAlert.locationName})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed
                    )
                    Text(
                        text = "Impact: ${latestAlert.impactG}g • Tilt: ${latestAlert.tiltAngle}° • SMS: ${latestAlert.smsStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No accident events recorded. System active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToAlerts,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Incident Alerts Log (${alerts.size})")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Contact Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToContacts() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = SafetyGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Primary Emergency Contact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    StatusChip(
                        text = deviceStatus.emergencySyncStatus,
                        variant = if (deviceStatus.emergencySyncStatus == "Synced") ChipVariant.SUCCESS else ChipVariant.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (primaryContact != null) {
                    Text(
                        text = "${primaryContact.name} (${primaryContact.relationship})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = primaryContact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stored locally in ESP32 NVS Flash • Dispatched upon 15s timeout",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No emergency contact configured!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Diagnostics Quick Summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDiagnostics() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hardware & Telemetry Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ESP32 DevKit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(text = "GPIO 21,22,16,17,26,27,25,33", variant = ChipVariant.INFO)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MPU6050 Tilt/Acc", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(text = "45° / 4.0g Calibrated", variant = ChipVariant.SUCCESS)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("NEO-6M GPS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(text = if (deviceStatus.gpsFix && currentLocation.satellites > 0) "Fix Active (" + currentLocation.satellites + " Sats)" else if (deviceStatus.gpsFix) "Fix Active" else "Searching Fix", variant = if (deviceStatus.gpsFix) ChipVariant.SUCCESS else ChipVariant.WARNING)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SIM800L GSM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusChip(text = "${deviceStatus.gsmSignalDbm} dBm (Good)", variant = ChipVariant.SUCCESS)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
