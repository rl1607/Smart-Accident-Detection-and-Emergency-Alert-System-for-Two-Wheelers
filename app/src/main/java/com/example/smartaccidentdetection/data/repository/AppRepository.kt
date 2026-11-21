package com.example.smartaccidentdetection.data.repository

import com.example.smartaccidentdetection.data.model.AccidentAlert
import com.example.smartaccidentdetection.data.model.DeviceStatus
import com.example.smartaccidentdetection.data.model.EmergencyContact
import com.example.smartaccidentdetection.data.model.LocationPoint
import com.example.smartaccidentdetection.data.model.Trip
import com.example.smartaccidentdetection.data.model.UserProfile
import com.example.smartaccidentdetection.data.remote.RetrofitClient
import com.example.smartaccidentdetection.data.remote.SupabaseClient
import com.example.smartaccidentdetection.util.DateTimeUtils
import com.example.smartaccidentdetection.util.GeoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID

class AppRepository private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Live Cloud Mode active by default to fetch real data from Supabase
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _deviceStatus = MutableStateFlow(
        DeviceStatus(
            deviceId = "ACCIDENT-001",
            isOnline = false,
            gpsFix = false,
            satellites = 0,
            currentSpeedKmh = 0.0,
            distanceTravelledKm = 0.0,
            lastSeen = "Connecting to database..."
        )
    )
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    private val _userProfile = MutableStateFlow(MockDataProvider.defaultUserProfile)
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _currentLocation = MutableStateFlow(
        LocationPoint(
            deviceId = "ACCIDENT-001",
            latitude = 0.0,
            longitude = 0.0,
            speedKmh = 0.0,
            altitudeM = 0.0,
            satellites = 0,
            locationSource = "Connecting...",
            createdAt = null
        )
    )
    val currentLocation: StateFlow<LocationPoint> = _currentLocation.asStateFlow()

    private val _routePoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val routePoints: StateFlow<List<LocationPoint>> = _routePoints.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _alerts = MutableStateFlow<List<AccidentAlert>>(emptyList())
    val alerts: StateFlow<List<AccidentAlert>> = _alerts.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    // Accident Pending Countdown State for Rider Cancel UI simulation
    private val _accidentCountdown = MutableStateFlow<Int?>(null)
    val accidentCountdown: StateFlow<Int?> = _accidentCountdown.asStateFlow()

    // Selected Incident from Alerts tab to focus directly on Google Maps
    private val _focusedIncident = MutableStateFlow<AccidentAlert?>(null)
    val focusedIncident: StateFlow<AccidentAlert?> = _focusedIncident.asStateFlow()

    fun focusOnIncident(alert: AccidentAlert) {
        _focusedIncident.value = alert
    }

    fun clearFocusedIncident() {
        _focusedIncident.value = null
    }

    init {
        // Fetch live data from Supabase immediately on launch
        scope.launch {
            refreshCloudData()
        }
        // Start continuous 10-second live polling loop
        startLiveSimulationLoop()
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun setBackendUrl(url: String) {
        RetrofitClient.baseUrl = url
    }

    // User Profile Actions
    fun updateProfile(updated: UserProfile) {
        _userProfile.value = updated
        scope.launch {
            if (!_isDemoMode.value) {
                // Update directly in Supabase Cloud
                val uid = updated.id ?: "1"
                runCatching {
                    SupabaseClient.api.updateUser(
                        idFilter = "eq.$uid",
                        user = mapOf(
                            "name" to (updated.fullName ?: ""),
                            "phone" to (updated.phoneNumber ?: ""),
                            "email" to (updated.email ?: ""),
                            "vehicle_number" to (updated.vehicleNumber ?: ""),
                            "vehicle_model" to (updated.vehicleModel ?: "")
                        )
                    )
                }
                runCatching {
                    RetrofitClient.apiService.updateUser(uid, updated)
                }
            }
        }
    }

    // Emergency Contacts Actions
    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        val newContact = EmergencyContact(
            id = "CNT_" + UUID.randomUUID().toString().take(6).uppercase(),
            userId = _userProfile.value.id ?: "1",
            name = name,
            phoneNumber = phone,
            relationship = relationship,
            isPrimary = isPrimary,
            syncState = "Synced with ESP32"
        )
        val currentList = _contacts.value.toMutableList()
        if (isPrimary) {
            // Unmark other primary
            for (i in currentList.indices) {
                currentList[i] = currentList[i].copy(isPrimary = false)
            }
        }
        currentList.add(newContact)
        _contacts.value = currentList
        syncContactsToCloud(currentList)
    }

    fun editContact(contact: EmergencyContact) {
        val currentList = _contacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            if (contact.isPrimary) {
                for (i in currentList.indices) {
                    currentList[i] = currentList[i].copy(isPrimary = false)
                }
            }
            currentList[index] = contact.copy(syncState = "Synced with ESP32")
            _contacts.value = currentList
            syncContactsToCloud(currentList)
        }
    }

    fun deleteContact(id: String) {
        val currentList = _contacts.value.filter { it.id != id }
        _contacts.value = currentList
        syncContactsToCloud(currentList)
    }

    fun setPrimaryContact(id: String) {
        val currentList = _contacts.value.map {
            it.copy(isPrimary = it.id == id, syncState = "Synced with ESP32")
        }
        _contacts.value = currentList
        syncContactsToCloud(currentList)
    }

    private fun syncContactsToCloud(list: List<EmergencyContact>) {
        _deviceStatus.value = _deviceStatus.value.copy(
            emergencySyncStatus = "Synced",
            lastSyncTimestamp = "Just now"
        )
        scope.launch {
            if (!_isDemoMode.value) {
                // 1. Direct Cloud Sync to Supabase
                runCatching {
                    val primary = list.firstOrNull { it.isPrimary } ?: list.firstOrNull()
                    if (primary != null) {
                        SupabaseClient.api.updateEmergencyContact(
                            idFilter = "eq.1",
                            contact = mapOf(
                                "name" to primary.name,
                                "phone" to primary.phoneNumber,
                                "relationship" to primary.relationship
                            )
                        )
                    }
                }
                // 2. Also notify local backend if reachable
                runCatching {
                    RetrofitClient.apiService.updateEmergencyContacts(_deviceStatus.value.deviceId, list)
                }
            }
        }
    }

    // Accident State Machine Simulation and Cloud ingestion
    fun triggerSimulatedAccident(detectionType: String = "Impact (4.2g) + Tilt (49°)") {
        _accidentCountdown.value = 15
        _deviceStatus.value = _deviceStatus.value.copy(accidentState = "POSSIBLE_ACCIDENT")

        scope.launch {
            for (sec in 14 downTo 0) {
                delay(1000)
                if (_accidentCountdown.value == null) {
                    // Rider cancelled
                    return@launch
                }
                _accidentCountdown.value = sec
            }

            // Countdown expired without cancellation: Confirm Accident
            _accidentCountdown.value = null
            _deviceStatus.value = _deviceStatus.value.copy(accidentState = "CONFIRMED_ACCIDENT")

            val primaryContact = _contacts.value.firstOrNull { it.isPrimary } ?: _contacts.value.firstOrNull()
            val contactPhone = primaryContact?.phoneNumber ?: "+919876543210"

            val confirmedAlert = AccidentAlert(
                id = "ALT_" + UUID.randomUUID().toString().take(6).uppercase(),
                deviceId = _deviceStatus.value.deviceId,
                timestamp = System.currentTimeMillis(),
                latitude = _currentLocation.value.latitude,
                longitude = _currentLocation.value.longitude,
                locationName = "Live Incident: Near Current GPS Fix",
                impactG = 4.25,
                tiltAngle = 49.0,
                detectionType = detectionType,
                status = "CONFIRMED",
                smsStatus = "Sent to $contactPhone",
                callStatus = "Completed (25s)",
                locationSource = _currentLocation.value.locationSource
            )

            val updatedAlerts = listOf(confirmedAlert) + _alerts.value
            _alerts.value = updatedAlerts

            if (!_isDemoMode.value) {
                try {
                    RetrofitClient.apiService.postAccident(_deviceStatus.value.deviceId, confirmedAlert)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cancelSimulatedAccident() {
        if (_accidentCountdown.value != null) {
            _accidentCountdown.value = null
            _deviceStatus.value = _deviceStatus.value.copy(accidentState = "CANCELLED")

            val cancelledAlert = AccidentAlert(
                id = "ALT_" + UUID.randomUUID().toString().take(6).uppercase(),
                deviceId = _deviceStatus.value.deviceId,
                timestamp = System.currentTimeMillis(),
                latitude = _currentLocation.value.latitude,
                longitude = _currentLocation.value.longitude,
                locationName = "Incident Cancelled by Rider (Within 15s)",
                impactG = 4.05,
                tiltAngle = 46.0,
                detectionType = "Tilt Warning (46.0°)",
                status = "CANCELLED_BY_RIDER",
                smsStatus = "Cancelled",
                callStatus = "Cancelled",
                locationSource = _currentLocation.value.locationSource
            )
            _alerts.value = listOf(cancelledAlert) + _alerts.value

            scope.launch {
                runCatching {
                    SupabaseClient.api.postAccident(
                        mapOf(
                            "device_id" to _deviceStatus.value.deviceId,
                            "latitude" to _currentLocation.value.latitude,
                            "longitude" to _currentLocation.value.longitude,
                            "location_source" to _currentLocation.value.locationSource,
                            "status" to "CANCELLED_BY_RIDER",
                            "sms_status" to "CANCELLED_BY_RIDER",
                            "call_status" to "CANCELLED_BY_RIDER"
                        )
                    )
                }
                delay(3000)
                _deviceStatus.value = _deviceStatus.value.copy(accidentState = "SAFE")
            }
        }
    }

    // Refresh from live cloud backend if demo mode is off
    suspend fun refreshCloudData() {
        if (_isDemoMode.value) return
        val deviceId = _deviceStatus.value.deviceId

        // 1. Current Live Location directly from Supabase Cloud
        runCatching {
            val locResp = SupabaseClient.api.getLatestLocation(deviceIdFilter = "eq.$deviceId")
            if (locResp.isSuccessful && !locResp.body().isNullOrEmpty()) {
                val latest = locResp.body()!!.first()
                _currentLocation.value = latest
                _deviceStatus.value = _deviceStatus.value.copy(
                    currentSpeedKmh = latest.speedKmh,
                    lastSeen = latest.createdAt ?: "Just now",
                    locationSource = latest.locationSource
                )
                Log.d("AppRepository", "Live Supabase Location: ${latest.latitude}, ${latest.longitude} (${latest.speedKmh} km/h)")
            } else {
                // Fallback to local server if available
                val fallback = RetrofitClient.apiService.getCurrentLocation(deviceId)
                if (fallback.isSuccessful && fallback.body() != null) {
                    val latest = fallback.body()!!
                    _currentLocation.value = latest
                    _deviceStatus.value = _deviceStatus.value.copy(
                        currentSpeedKmh = latest.speedKmh,
                        lastSeen = latest.createdAt ?: "Just now",
                        locationSource = latest.locationSource
                    )
                }
            }
        }.onFailure { Log.e("AppRepository", "Location fetch error: ${it.message}") }

        // 2. Historical Route Polyline Points directly from Supabase Cloud
        runCatching {
            val locsResp = SupabaseClient.api.getRoutePoints(deviceIdFilter = "eq.$deviceId")
            if (locsResp.isSuccessful && !locsResp.body().isNullOrEmpty()) {
                val points = locsResp.body()!!.reversed()
                _routePoints.value = points
                val realDistance = GeoUtils.calculateTotalDistanceKm(points)
                _deviceStatus.value = _deviceStatus.value.copy(
                    distanceTravelledKm = realDistance
                )
                Log.d("AppRepository", "Live Supabase Route: ${points.size} points (${String.format("%.2f", realDistance)} km)")

                // Dynamically build real trip record from actual trackpoints
                val startPoint = points.first()
                val endPoint = points.last()
                val maxSpeed = points.maxOfOrNull { it.speedKmh } ?: 0.0
                val avgSpeed = points.map { it.speedKmh }.average()
                _trips.value = listOf(
                    Trip(
                        id = "TRIP_LIVE",
                        deviceId = deviceId,
                        date = DateTimeUtils.formatToLocalDateTime(endPoint.createdAt),
                        startTime = DateTimeUtils.formatToLocalTime(startPoint.createdAt),
                        endTime = DateTimeUtils.formatToLocalTime(endPoint.createdAt),
                        distanceKm = realDistance,
                        durationMinutes = ((points.size * 10) / 60).coerceAtLeast(1),
                        maxSpeedKmh = maxSpeed,
                        avgSpeedKmh = avgSpeed,
                        startLocationName = "Origin (${String.format("%.4f, %.4f", startPoint.latitude, startPoint.longitude)})",
                        endLocationName = "Current (${String.format("%.4f, %.4f", endPoint.latitude, endPoint.longitude)})",
                        routePoints = points
                    )
                )
            } else {
                val fallback = RetrofitClient.apiService.getRouteLocations(deviceId)
                if (fallback.isSuccessful && fallback.body() != null) {
                    _routePoints.value = fallback.body()!!
                }
            }
        }.onFailure { Log.e("AppRepository", "Route fetch error: ${it.message}") }

        // 3. Alerts & Accidents directly from Supabase Cloud
        runCatching {
            val alertsResp = SupabaseClient.api.getAccidents(deviceIdFilter = "eq.$deviceId")
            if (alertsResp.isSuccessful && !alertsResp.body().isNullOrEmpty()) {
                val alertList = alertsResp.body()!!
                _alerts.value = alertList
                Log.d("AppRepository", "Live Supabase Accidents: ${alertList.size} alerts")
            } else {
                val fallback = RetrofitClient.apiService.getAlerts(deviceId)
                if (fallback.isSuccessful && fallback.body() != null) {
                    _alerts.value = fallback.body()!!
                }
            }
        }.onFailure { Log.e("AppRepository", "Alerts fetch error: ${it.message}") }

        // 4. Primary Emergency Contact directly from Supabase Cloud
        runCatching {
            val contactResp = SupabaseClient.api.getEmergencyContacts()
            if (contactResp.isSuccessful && !contactResp.body().isNullOrEmpty()) {
                val contactsList = contactResp.body()!!
                _contacts.value = contactsList
                val primary = contactsList.firstOrNull { it.isPrimary } ?: contactsList.first()
                Log.d("AppRepository", "Live Supabase Emergency Contact: ${primary.name} (${primary.phoneNumber})")
            } else {
                val fallback = RetrofitClient.apiService.getEmergencyContact(deviceId)
                if (fallback.isSuccessful && fallback.body() != null) {
                    val raw = fallback.body()!!
                    val primaryContact = EmergencyContact(
                        id = (raw.id as? String) ?: "CNT_01",
                        userId = raw.userId ?: "USER_001",
                        name = raw.name ?: "Emergency Contact",
                        phoneNumber = raw.phoneNumber ?: "+919353035530",
                        relationship = raw.relationship ?: "Family",
                        isPrimary = true,
                        syncState = "Synced with ESP32"
                    )
                    _contacts.value = listOf(primaryContact)
                }
            }
        }.onFailure { Log.e("AppRepository", "Contact fetch error: ${it.message}") }

        // 5. Device Status directly from Supabase Cloud
        runCatching {
            val statusResp = SupabaseClient.api.getDeviceStatus(deviceIdFilter = "eq.$deviceId")
            if (statusResp.isSuccessful && !statusResp.body().isNullOrEmpty()) {
                val rawStatus = statusResp.body()!!.first()
                val liveSeen = _currentLocation.value.createdAt ?: rawStatus.lastSeen
                _deviceStatus.value = rawStatus.copy(
                    currentSpeedKmh = _currentLocation.value.speedKmh,
                    lastSeen = liveSeen
                )
                Log.d("AppRepository", "Live Supabase Device Status: ${rawStatus.accidentState}")
            } else {
                val fallback = RetrofitClient.apiService.getDeviceStatus(deviceId)
                if (fallback.isSuccessful && fallback.body() != null) {
                    _deviceStatus.value = fallback.body()!!
                }
            }
        }.onFailure { Log.e("AppRepository", "Status fetch error: ${it.message}") }

        // 6. User Profile directly from Supabase Cloud
        runCatching {
            val userResp = SupabaseClient.api.getUsers()
            if (userResp.isSuccessful && !userResp.body().isNullOrEmpty()) {
                val u = userResp.body()!!.first()
                _userProfile.value = u
                Log.d("AppRepository", "Live Supabase User: ${u.fullName} (${u.phoneNumber})")
            }
        }.onFailure { Log.e("AppRepository", "User fetch error: ${it.message}") }
    }

    // 10-second periodic movement update (Simulation in Demo Mode, Live Polling in Cloud Mode)
    private fun startLiveSimulationLoop() {
        scope.launch {
            var stepIndex = 0
            val mockPoints = MockDataProvider.sampleRoutePoints
            while (true) {
                delay(10000) // 10 second update interval
                if (_isDemoMode.value && _accidentCountdown.value == null) {
                    stepIndex = (stepIndex + 1) % mockPoints.size
                    val basePoint = mockPoints[stepIndex]
                    val updatedPoint = basePoint.copy(
                        timestamp = System.currentTimeMillis()
                    )
                    _currentLocation.value = updatedPoint

                    val newRoute = _routePoints.value.toMutableList()
                    if (newRoute.size > 50) newRoute.removeAt(0)
                    newRoute.add(updatedPoint)
                    _routePoints.value = newRoute

                    _deviceStatus.value = _deviceStatus.value.copy(
                        currentSpeedKmh = updatedPoint.speedKmh,
                        distanceTravelledKm = _deviceStatus.value.distanceTravelledKm + (updatedPoint.speedKmh * (10.0 / 3600.0)),
                        lastSeen = "Just now"
                    )
                } else if (!_isDemoMode.value) {
                    // Live Cloud auto-refresh every 10 seconds
                    refreshCloudData()
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AppRepository? = null

        fun getInstance(): AppRepository {
            return instance ?: synchronized(this) {
                instance ?: AppRepository().also { instance = it }
            }
        }
    }
}
