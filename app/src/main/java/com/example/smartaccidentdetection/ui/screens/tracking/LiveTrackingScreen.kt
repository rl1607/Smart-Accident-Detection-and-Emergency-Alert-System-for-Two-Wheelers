package com.example.smartaccidentdetection.ui.screens.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

import com.example.smartaccidentdetection.ui.components.ChipVariant
import com.example.smartaccidentdetection.ui.components.StatusChip
import com.example.smartaccidentdetection.ui.theme.PrimaryBlue
import com.example.smartaccidentdetection.ui.theme.SafetyGreen
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import com.example.smartaccidentdetection.data.model.LocationPoint
import com.google.android.gms.maps.model.BitmapDescriptor
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

enum class RouteTimePreset(val label: String, val shortLabel: String) {
    ALL("All Time (Full Route)", "All Time"),
    LAST_15_MIN("Last 15 Minutes", "Last 15m"),
    LAST_1_HOUR("Last 1 Hour", "Last 1h"),
    LAST_3_HOURS("Last 3 Hours", "Last 3h"),
    TODAY("Today's Journey", "Today"),
    CUSTOM("Custom Time Window", "Custom")
}

@Composable
fun rememberWaypointDotIcon(): BitmapDescriptor {
    val context = LocalContext.current
    return remember {
        try {
            MapsInitializer.initialize(context)
            val sizePx = 28
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                isAntiAlias = true
            }

            // White border ring
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

            // Royal blue interior circle (GeoLinker CircuitDigest style)
            paint.color = android.graphics.Color.rgb(25, 118, 210)
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - 3.5f, paint)

            // Subtle white center dot
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, 3f, paint)

            BitmapDescriptorFactory.fromBitmap(bitmap)
        } catch (e: Exception) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }
    }
}

@Composable
fun LiveTrackingScreen(
    repository: AppRepository
) {
    val currentLocation by repository.currentLocation.collectAsState()
    val routePoints by repository.routePoints.collectAsState()
    val deviceStatus by repository.deviceStatus.collectAsState()

    val vehicleLatLng = remember(currentLocation.latitude, currentLocation.longitude) {
        LatLng(currentLocation.latitude, currentLocation.longitude)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vehicleLatLng, 16.5f)
    }

    val coroutineScope = rememberCoroutineScope()
    var isSatelliteView by remember { mutableStateOf(false) }
    var selectedWaypoint by remember { mutableStateOf<LocationPoint?>(null) }
    val focusedIncident by repository.focusedIncident.collectAsState()
    val waypointDotIcon = rememberWaypointDotIcon()

    // Route Path Visibility & Time Filter States
    var showPathTravelled by rememberSaveable { mutableStateOf(false) }
    var activePreset by rememberSaveable { mutableStateOf(RouteTimePreset.ALL) }
    var customStartHour by rememberSaveable { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) - 1) }
    var customStartMinute by rememberSaveable { mutableStateOf(0) }
    var customEndHour by rememberSaveable { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) }
    var customEndMinute by rememberSaveable { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Dynamic Filtered Route Points
    val filteredRoutePoints = remember(
        routePoints,
        showPathTravelled,
        activePreset,
        customStartHour,
        customStartMinute,
        customEndHour,
        customEndMinute
    ) {
        if (!showPathTravelled || routePoints.isEmpty()) emptyList()
        else {
            val now = System.currentTimeMillis()
            when (activePreset) {
                RouteTimePreset.ALL -> routePoints
                RouteTimePreset.LAST_15_MIN -> {
                    val threshold = now - (15 * 60 * 1000)
                    routePoints.filter { DateTimeUtils.parseToEpochMillis(it.createdAt, it.timestamp) >= threshold }
                }
                RouteTimePreset.LAST_1_HOUR -> {
                    val threshold = now - (60 * 60 * 1000)
                    routePoints.filter { DateTimeUtils.parseToEpochMillis(it.createdAt, it.timestamp) >= threshold }
                }
                RouteTimePreset.LAST_3_HOURS -> {
                    val threshold = now - (3 * 60 * 60 * 1000)
                    routePoints.filter { DateTimeUtils.parseToEpochMillis(it.createdAt, it.timestamp) >= threshold }
                }
                RouteTimePreset.TODAY -> {
                    val startOfToday = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    routePoints.filter { DateTimeUtils.parseToEpochMillis(it.createdAt, it.timestamp) >= startOfToday }
                }
                RouteTimePreset.CUSTOM -> {
                    val startCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, customStartHour.coerceIn(0, 23))
                        set(java.util.Calendar.MINUTE, customStartMinute.coerceIn(0, 59))
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val endCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, customEndHour.coerceIn(0, 23))
                        set(java.util.Calendar.MINUTE, customEndMinute.coerceIn(0, 59))
                        set(java.util.Calendar.SECOND, 59)
                        set(java.util.Calendar.MILLISECOND, 999)
                    }
                    val startMillis = startCal.timeInMillis
                    val endMillis = endCal.timeInMillis
                    routePoints.filter {
                        val ptTime = DateTimeUtils.parseToEpochMillis(it.createdAt, it.timestamp)
                        ptTime in startMillis..endMillis
                    }
                }
            }
        }
    }

    // Smoothly animate camera when new location arrives from Supabase or when focusing an incident
    LaunchedEffect(vehicleLatLng, focusedIncident) {
        if (focusedIncident != null) {
            val incidentLatLng = LatLng(focusedIncident!!.latitude, focusedIncident!!.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(incidentLatLng, 18f),
                durationMs = 900
            )
        } else {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(vehicleLatLng),
                durationMs = 800
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Official Google Maps View
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = if (isSatelliteView) MapType.HYBRID else MapType.NORMAL,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = true
            )
        ) {
            // Blue Route Line (only if showPathTravelled is true)
            if (showPathTravelled && filteredRoutePoints.size >= 2) {
                val pathCoords = filteredRoutePoints.map { LatLng(it.latitude, it.longitude) }
                Polyline(
                    points = pathCoords,
                    color = PrimaryBlue,
                    width = 10f
                )
            }

            // 10-Second Historical Waypoint Dots (only if showPathTravelled is true)
            if (showPathTravelled) {
                filteredRoutePoints.forEachIndexed { index, point ->
                    val pointLatLng = LatLng(point.latitude, point.longitude)
                    val timeLabel = if (!point.createdAt.isNullOrBlank()) {
                        DateTimeUtils.formatToLocalTime(point.createdAt)
                    } else {
                        "10s Fix #${index + 1}"
                    }
                    Marker(
                        state = remember(point.latitude, point.longitude) {
                            MarkerState(position = pointLatLng)
                        },
                        title = "10s GPS Fix: $timeLabel",
                        snippet = "Point #${index + 1} • Speed: ${String.format("%.1f", point.speedKmh)} km/h",
                        icon = waypointDotIcon,
                        anchor = Offset(0.5f, 0.5f),
                        onClick = { marker ->
                            selectedWaypoint = point
                            marker.showInfoWindow()
                            true
                        }
                    )
                }

                // Start Journey Origin Marker (Trip Start Location)
                if (filteredRoutePoints.isNotEmpty()) {
                    val originPoint = filteredRoutePoints.first()
                    Marker(
                        state = remember(originPoint.latitude, originPoint.longitude) {
                            MarkerState(position = LatLng(originPoint.latitude, originPoint.longitude))
                        },
                        title = "Trip Origin",
                        snippet = "Start: ${if (!originPoint.createdAt.isNullOrBlank()) DateTimeUtils.formatToLocalDateTime(originPoint.createdAt) else "Trip Start"}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }

            // Green Marker: Your live ESP32 vehicle location
            Marker(
                state = MarkerState(position = vehicleLatLng),
                title = if ((System.currentTimeMillis() - (com.example.smartaccidentdetection.util.DateTimeUtils.parseToDate(currentLocation.createdAt ?: "")?.time ?: 0)) < 60000) "Live ESP32 Vehicle Location" else "Last Known Vehicle Location (Database)",
                snippet = "${String.format("%.6f° N, %.6f° E", currentLocation.latitude, currentLocation.longitude)} • Speed: ${String.format("%.1f", currentLocation.speedKmh)} km/h",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                onClick = { marker ->
                    selectedWaypoint = null
                    marker.showInfoWindow()
                    true
                }
            )

            // Accident Incident Marker: ONLY shown when navigating from Alerts via "View Incident Location on Map"
            focusedIncident?.let { alert ->
                val isCancelled = alert.status?.contains("CANCEL") == true || alert.status?.contains("SAFE") == true
                Marker(
                    state = remember(alert.latitude, alert.longitude) {
                        MarkerState(position = LatLng(alert.latitude, alert.longitude))
                    },
                    title = if (isCancelled) "Rider Safe: ${alert.detectionType}" else "Accident Incident: ${alert.detectionType}",
                    snippet = "Lat: ${String.format("%.6f", alert.latitude)}, Lon: ${String.format("%.6f", alert.longitude)} | Impact: ${alert.impactG}g | Tilt: ${alert.tiltAngle}°",
                    icon = if (isCancelled) {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                    } else {
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    },
                    onClick = { marker ->
                        marker.showInfoWindow()
                        true
                    }
                )
            }
        }

        // Top Status Floating Chip: ONLY shown when viewing an incident from Alerts or when a custom time filter is active
        if (focusedIncident != null || activePreset != RouteTimePreset.ALL) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (focusedIncident != null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f) else PrimaryBlue.copy(alpha = 0.15f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (focusedIncident != null) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Viewing Incident Location from Database",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusChip(
                            text = "Close",
                            variant = ChipVariant.DANGER,
                            modifier = Modifier.clickable { repository.clearFocusedIncident() }
                        )
                    } else if (activePreset != RouteTimePreset.ALL) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Path: ${activePreset.shortLabel} (${filteredRoutePoints.size} pts)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.clickable { showFilterDialog = true }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusChip(
                            text = "Reset",
                            variant = ChipVariant.INFO,
                            modifier = Modifier.clickable { activePreset = RouteTimePreset.ALL }
                        )
                    }
                }
            }
        }

        // Map Control Floating Action Buttons (Right side - positioned safely below the top chip)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Toggle Roadmap vs Satellite Mode
            FloatingActionButton(
                onClick = {
                    isSatelliteView = !isSatelliteView
                },
                shape = CircleShape,
                containerColor = if (isSatelliteView) PrimaryBlue else MaterialTheme.colorScheme.surface,
                contentColor = if (isSatelliteView) Color.White else MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Toggle Google Maps View", modifier = Modifier.size(20.dp))
            }

            // 2. Toggle Path Travelled (Route Line & 10s Waypoint Dots) ON / OFF
            FloatingActionButton(
                onClick = {
                    showPathTravelled = !showPathTravelled
                },
                shape = CircleShape,
                containerColor = if (showPathTravelled) PrimaryBlue else MaterialTheme.colorScheme.surface,
                contentColor = if (showPathTravelled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (showPathTravelled) Icons.Default.Route else Icons.Default.AltRoute,
                    contentDescription = if (showPathTravelled) "Turn Off Path Travelled" else "Turn On Path Travelled",
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. Filter Path Travelled between Specific Times
            FloatingActionButton(
                onClick = {
                    showFilterDialog = true
                },
                shape = CircleShape,
                containerColor = if (activePreset != RouteTimePreset.ALL) SafetyGreen else MaterialTheme.colorScheme.surface,
                contentColor = if (activePreset != RouteTimePreset.ALL) Color.White else MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HistoryToggleOff,
                    contentDescription = "Filter Path by Time",
                    modifier = Modifier.size(20.dp)
                )
            }

            // 4. Zoom In
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }

            // 5. Zoom Out
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }

            // 6. Center on Vehicle
            FloatingActionButton(
                onClick = {
                    repository.clearFocusedIncident()
                    selectedWaypoint = null
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(vehicleLatLng, 17f),
                            durationMs = 600
                        )
                    }
                },
                shape = CircleShape,
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on Vehicle")
            }
        }

        // Bottom Telemetry Info Card (Symmetrically padded, never cut off)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                focusedIncident != null -> "Incident Location (Database)"
                                selectedWaypoint != null -> "Selected Waypoint"
                                !showPathTravelled -> "Live Telemetry (Path Hidden)"
                                activePreset != RouteTimePreset.ALL -> "Filtered Path (${filteredRoutePoints.size} pts)"
                                else -> "Live Telemetry"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        val formattedDateTime = when {
                            focusedIncident != null -> DateTimeUtils.formatToLocalDateTime(focusedIncident!!.createdAt)
                            selectedWaypoint != null -> DateTimeUtils.formatToLocalDateTime(selectedWaypoint!!.createdAt)
                            activePreset != RouteTimePreset.ALL -> "Distance: ${String.format("%.2f", GeoUtils.calculateTotalDistanceKm(filteredRoutePoints))} km"
                            else -> DateTimeUtils.formatToLocalDateTime(currentLocation.createdAt ?: deviceStatus.lastSeen)
                        }
                        Text(
                            text = when {
                                focusedIncident != null -> "Incident: $formattedDateTime"
                                selectedWaypoint != null -> "Recorded: $formattedDateTime"
                                activePreset != RouteTimePreset.ALL -> formattedDateTime
                                else -> "Updated: $formattedDateTime"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (focusedIncident != null) {
                            StatusChip(
                                text = "Live Vehicle",
                                variant = ChipVariant.SUCCESS,
                                icon = Icons.Default.MyLocation,
                                modifier = Modifier.clickable {
                                    repository.clearFocusedIncident()
                                    selectedWaypoint = null
                                }
                            )
                        } else if (selectedWaypoint != null) {
                            StatusChip(
                                text = "Live Vehicle",
                                variant = ChipVariant.SUCCESS,
                                icon = Icons.Default.MyLocation,
                                modifier = Modifier.clickable { selectedWaypoint = null }
                            )
                        } else {
                            StatusChip(
                                text = "${currentLocation.satellites} Sats",
                                variant = ChipVariant.INFO,
                                icon = Icons.Default.SatelliteAlt
                            )
                            StatusChip(
                                text = currentLocation.locationSource,
                                variant = if (currentLocation.locationSource == "GPS") ChipVariant.SUCCESS else ChipVariant.WARNING,
                                icon = Icons.Default.GpsFixed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayLat = when {
                    focusedIncident != null -> focusedIncident!!.latitude
                    selectedWaypoint != null -> selectedWaypoint!!.latitude
                    else -> currentLocation.latitude
                }
                val displayLon = when {
                    focusedIncident != null -> focusedIncident!!.longitude
                    selectedWaypoint != null -> selectedWaypoint!!.longitude
                    else -> currentLocation.longitude
                }
                val displayMetricLabel = if (focusedIncident != null) "Impact" else "Speed"
                val displayMetricValue = if (focusedIncident != null) {
                    "${focusedIncident!!.impactG} g"
                } else {
                    "${String.format("%.1f", selectedWaypoint?.speedKmh ?: currentLocation.speedKmh)} km/h"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "Latitude",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.6f° N", displayLat),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1.1f)) {
                        Text(
                            text = "Longitude",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.6f° E", displayLon),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = displayMetricLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = displayMetricValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (focusedIncident != null) MaterialTheme.colorScheme.error else PrimaryBlue,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Route Time Filter Dialog
        if (showFilterDialog) {
            RouteTimeFilterDialog(
                showPathTravelled = showPathTravelled,
                onToggleShowPath = { showPathTravelled = it },
                activePreset = activePreset,
                onSelectPreset = { activePreset = it },
                customStartHour = customStartHour,
                customStartMinute = customStartMinute,
                onStartHourMinuteChange = { h, m ->
                    customStartHour = h
                    customStartMinute = m
                },
                customEndHour = customEndHour,
                customEndMinute = customEndMinute,
                onEndHourMinuteChange = { h, m ->
                    customEndHour = h
                    customEndMinute = m
                },
                matchingPointsCount = filteredRoutePoints.size,
                matchingDistanceKm = GeoUtils.calculateTotalDistanceKm(filteredRoutePoints),
                onDismiss = { showFilterDialog = false },
                onReset = {
                    showPathTravelled = true
                    activePreset = RouteTimePreset.ALL
                }
            )
        }
    }
}

@Composable
fun RouteTimeFilterDialog(
    showPathTravelled: Boolean,
    onToggleShowPath: (Boolean) -> Unit,
    activePreset: RouteTimePreset,
    onSelectPreset: (RouteTimePreset) -> Unit,
    customStartHour: Int,
    customStartMinute: Int,
    onStartHourMinuteChange: (Int, Int) -> Unit,
    customEndHour: Int,
    customEndMinute: Int,
    onEndHourMinuteChange: (Int, Int) -> Unit,
    matchingPointsCount: Int,
    matchingDistanceKm: Double,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var tempPreset by remember { mutableStateOf(activePreset) }
    var tempShowPath by remember { mutableStateOf(showPathTravelled) }
    var tempStartHour by remember { mutableStateOf(customStartHour) }
    var tempStartMinute by remember { mutableStateOf(customStartMinute) }
    var tempEndHour by remember { mutableStateOf(customEndHour) }
    var tempEndMinute by remember { mutableStateOf(customEndMinute) }

    fun formatHourMinute(hour: Int, minute: Int): String {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
        }
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(cal.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.HistoryToggleOff,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Path Travelled Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Toggle Switch to Turn ON / OFF Path Travelled
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Path on Map",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (tempShowPath) "Route line & waypoint dots visible" else "Route hidden for clean view",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tempShowPath,
                            onCheckedChange = { tempShowPath = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryBlue
                            )
                        )
                    }
                }

                HorizontalDivider()

                // 2. Time Window Presets Header
                Text(
                    text = "Filter by Time Window",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Presets Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.ALL,
                            onClick = { tempPreset = RouteTimePreset.ALL },
                            label = { Text("All Time", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.LAST_15_MIN,
                            onClick = { tempPreset = RouteTimePreset.LAST_15_MIN },
                            label = { Text("Last 15m", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.LAST_1_HOUR,
                            onClick = { tempPreset = RouteTimePreset.LAST_1_HOUR },
                            label = { Text("Last 1h", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.LAST_3_HOURS,
                            onClick = { tempPreset = RouteTimePreset.LAST_3_HOURS },
                            label = { Text("Last 3 Hours", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.TODAY,
                            onClick = { tempPreset = RouteTimePreset.TODAY },
                            label = { Text("Today's Trip", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = tempPreset == RouteTimePreset.CUSTOM,
                            onClick = { tempPreset = RouteTimePreset.CUSTOM },
                            label = { Text("Custom...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 3. Custom Time Selection Pickers
                if (tempPreset == RouteTimePreset.CUSTOM) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Select Custom Time Range:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                tempStartHour = hour
                                                tempStartMinute = minute
                                            },
                                            tempStartHour,
                                            tempStartMinute,
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("From", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatHourMinute(tempStartHour, tempStartMinute), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        android.app.TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                tempEndHour = hour
                                                tempEndMinute = minute
                                            },
                                            tempEndHour,
                                            tempEndMinute,
                                            false
                                        ).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("To", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatHourMinute(tempEndHour, tempEndMinute), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Preview Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PrimaryBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Matching Waypoints:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (tempShowPath) "$matchingPointsCount fixes (${String.format("%.2f", matchingDistanceKm)} km)" else "Path Hidden",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (tempShowPath) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onToggleShowPath(tempShowPath)
                    onSelectPreset(tempPreset)
                    if (tempPreset == RouteTimePreset.CUSTOM) {
                        onStartHourMinuteChange(tempStartHour, tempStartMinute)
                        onEndHourMinuteChange(tempEndHour, tempEndMinute)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onReset()
                    onDismiss()
                }
            ) {
                Text("Reset to All")
            }
        }
    )
}
