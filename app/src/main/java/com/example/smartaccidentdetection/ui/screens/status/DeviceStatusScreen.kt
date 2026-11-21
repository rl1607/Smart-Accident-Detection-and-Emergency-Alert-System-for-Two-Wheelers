package com.example.smartaccidentdetection.ui.screens.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartaccidentdetection.data.remote.RetrofitClient
import com.example.smartaccidentdetection.data.repository.AppRepository
import com.example.smartaccidentdetection.ui.components.ChipVariant
import com.example.smartaccidentdetection.ui.components.StatusChip
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue
import com.example.smartaccidentdetection.ui.theme.SafetyGreen
import com.example.smartaccidentdetection.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun DeviceStatusScreen(
    repository: AppRepository
) {
    val deviceStatus by repository.deviceStatus.collectAsState()
    val currentLocation by repository.currentLocation.collectAsState()
    val isDemoMode by repository.isDemoMode.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hardware & System Diagnostics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Live telemetry health of ESP32 and attached IoT modules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ESP32 DevKit V2 Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ESP32 DevKit V2 Microcontroller", fontWeight = FontWeight.Bold)
                    }
                    StatusChip(
                        text = if (deviceStatus.isOnline) "Connected" else "Offline",
                        variant = if (deviceStatus.isOnline) ChipVariant.SUCCESS else ChipVariant.DANGER
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Device ID: ${deviceStatus.deviceId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Last Telemetry Frame: ${deviceStatus.lastSeen}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Flash NVS Emergency Contact: ${deviceStatus.emergencySyncStatus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MPU6050 Sensor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = SafetyGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MPU6050 Accel & Gyroscope", fontWeight = FontWeight.Bold)
                    }
                    StatusChip(text = "Active", variant = ChipVariant.SUCCESS)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Impact Threshold: ~4.0g (Filtered against road vibrations)", style = MaterialTheme.typography.bodySmall)
                Text("Tilt Threshold: 45.0° Absolute X/Y Angle", style = MaterialTheme.typography.bodySmall)
                Text("I2C Bus: GPIO 21 (SDA), GPIO 22 (SCL)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // NEO-6M GPS Module Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NEO-6M GPS (Primary)", fontWeight = FontWeight.Bold)
                    }
                    StatusChip(
                        text = if (deviceStatus.gpsFix) "Fix Active" else "Acquiring...",
                        variant = if (deviceStatus.gpsFix) ChipVariant.SUCCESS else ChipVariant.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Active Satellites: ${if (currentLocation.satellites > 0) currentLocation.satellites else deviceStatus.satellites}", style = MaterialTheme.typography.bodySmall)
                Text("Current Location Source: ${currentLocation.locationSource}", style = MaterialTheme.typography.bodySmall)
                Text("UART2 Pins: GPIO 16 (RX), GPIO 17 (TX)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SIM800L GSM/GPRS Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NetworkCell, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIM800L GSM / GPRS Module", fontWeight = FontWeight.Bold)
                    }
                    StatusChip(text = "Connected", variant = ChipVariant.SUCCESS)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Signal Quality: ${deviceStatus.gsmSignalDbm} dBm", style = MaterialTheme.typography.bodySmall)
                Text("Network: ${deviceStatus.gsmNetworkStatus}", style = MaterialTheme.typography.bodySmall)
                Text("Location Fallback: Cellular / LBS Triangulation (Not true GPS)", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                Text("UART1 Pins: GPIO 27 (RX), GPIO 26 (TX) [External 3.7-4.2V Power]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hardware Pin Allocation Matrix Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cable, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pinout & Wiring Matrix", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                val pinTable = listOf(
                    "MPU6050 SDA" to "GPIO 21",
                    "MPU6050 SCL" to "GPIO 22",
                    "NEO-6M GPS TX" to "GPIO 16 (ESP32 RX)",
                    "NEO-6M GPS RX" to "GPIO 17 (ESP32 TX)",
                    "SIM800L TX" to "GPIO 27 (ESP32 RX)",
                    "SIM800L RX" to "GPIO 26 (ESP32 TX Divider)",
                    "Piezo Buzzer (+)" to "GPIO 25",
                    "Cancel Button" to "GPIO 33 (INPUT_PULLUP)"
                )

                pinTable.forEach { (component, pin) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(component, style = MaterialTheme.typography.bodySmall)
                        Text(pin, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = PrimaryBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    repository.refreshCloudData()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh System Diagnostics")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
