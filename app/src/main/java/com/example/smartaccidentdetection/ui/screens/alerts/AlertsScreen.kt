package com.example.smartaccidentdetection.ui.screens.alerts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartaccidentdetection.data.repository.AppRepository
import com.example.smartaccidentdetection.ui.components.ChipVariant
import com.example.smartaccidentdetection.ui.components.StatusChip
import com.example.smartaccidentdetection.ui.theme.DangerRed
import com.example.smartaccidentdetection.ui.theme.DangerRedContainer
import com.example.smartaccidentdetection.ui.theme.OnDangerRedContainer
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue
import com.example.smartaccidentdetection.ui.theme.SafetyGreen
import com.example.smartaccidentdetection.ui.theme.WarningAmber
import com.example.smartaccidentdetection.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AlertsScreen(
    repository: AppRepository,
    onNavigateToTracking: () -> Unit
) {
    val alerts by repository.alerts.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Accident & Alert History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Log of crash sensor triggers (4.0g impact / 45° tilt) and emergency dispatches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = SafetyGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Accident Events Recorded",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your vehicle sensor logs and alerts will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(alerts, key = { it.id ?: java.util.UUID.randomUUID().toString() }) { alert ->
                val statusStr = (alert.status ?: "CONFIRMED").uppercase()
                val isCancelled = statusStr.contains("CANCEL") || statusStr.contains("SAFE") || statusStr.contains("ABORT")
                val isConfirmed = !isCancelled

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCancelled) SafetyGreen.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isCancelled) SafetyGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCancelled) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isCancelled) SafetyGreen else DangerRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "ACCIDENT DETECTED",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCancelled) SafetyGreen else DangerRed
                                    )
                                    val formattedAlertTime = if (!alert.createdAt.isNullOrBlank()) {
                                        DateTimeUtils.formatToLocalDateTime(alert.createdAt)
                                    } else {
                                        dateFormat.format(Date(alert.timestamp))
                                    }
                                    Text(
                                        text = formattedAlertTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            StatusChip(
                                text = if (isCancelled) "RIDER SAFE" else (alert.locationSource ?: "GPS"),
                                variant = if (isCancelled) ChipVariant.SUCCESS else if ((alert.locationSource ?: "GPS") == "GPS") ChipVariant.DANGER else ChipVariant.WARNING
                            )
                        }

                        // Specific Rider Safe / Push Button Cancelled Banner
                        if (isCancelled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = SafetyGreen.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafetyGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Accident was detected by hardware sensors, but the rider pressed the push button. Rider is safe — Emergency dispatch cancelled.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SafetyGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Incident location
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val displayLocation = alert.locationName ?: "Incident (${String.format("%.4f, %.4f", alert.latitude, alert.longitude)})"
                            Text(
                                text = displayLocation,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Telemetry row (Impact G & Tilt Angle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Impact Force", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${alert.impactG} g", fontWeight = FontWeight.Bold, color = if (alert.impactG >= 4.0) (if (isCancelled) SafetyGreen else DangerRed) else MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tilt Angle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${alert.tiltAngle}°", fontWeight = FontWeight.Bold, color = if (alert.tiltAngle >= 45.0) (if (isCancelled) SafetyGreen else DangerRed) else MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val dText = if (isCancelled) "Rider Safe" else "Triggered"
                                Text(dText, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isCancelled) SafetyGreen else DangerRed)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SMS and Call status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Message,
                                    contentDescription = null,
                                    tint = if (isCancelled) SafetyGreen else PrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val smsDisplay = if (isCancelled) "SMS: Cancelled (Safe)" else "SMS: ${alert.smsStatus ?: "SENT"}"
                                Text(
                                    text = smsDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCancelled) SafetyGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = null,
                                    tint = if (isCancelled) SafetyGreen else SafetyGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val callDisplay = if (isCancelled) "Call: Aborted (Safe)" else "Call: ${alert.callStatus ?: "INITIATED"}"
                                Text(
                                    text = callDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCancelled) SafetyGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                repository.focusOnIncident(alert)
                                onNavigateToTracking()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Incident Location on Map")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
