package com.example.smartaccidentdetection.data.model

import com.google.gson.annotations.SerializedName

/**
 * Real-time Device and Telemetry Status
 */
data class DeviceStatus(
    @SerializedName("deviceId", alternate = ["device_id"]) val deviceId: String = "ACCIDENT-001",
    @SerializedName("isOnline", alternate = ["is_online"]) val isOnline: Boolean = false,
    @SerializedName("batteryPercent", alternate = ["battery_percent"]) val batteryPercent: Int = 100,
    @SerializedName("gsmSignalDbm", alternate = ["gsm_signal_dbm"]) val gsmSignalDbm: Int = -70,
    @SerializedName("gsmNetworkStatus", alternate = ["gsm_network_status"]) val gsmNetworkStatus: String = "4G / 2G GPRS Active",
    @SerializedName("gpsFix", alternate = ["gps_fix"]) val gpsFix: Boolean = false,
    @SerializedName("satellites") val satellites: Int = 0,
    @SerializedName("mpuStatus", alternate = ["mpu_status"]) val mpuStatus: String = "Active (Calibrated 45° / 4.0g)",
    @SerializedName("lastSeen", alternate = ["last_seen"]) val lastSeen: String = "Connecting to database...",
    @SerializedName("locationSource", alternate = ["location_source"]) val locationSource: String = "GPS",
    @SerializedName("emergencySyncStatus", alternate = ["emergency_sync_status"]) val emergencySyncStatus: String = "Synced",
    @SerializedName("lastSyncTimestamp", alternate = ["last_sync_timestamp"]) val lastSyncTimestamp: String = "Just now",
    @SerializedName("currentSpeedKmh", alternate = ["current_speed_kmh", "speed"]) val currentSpeedKmh: Double = 0.0,
    @SerializedName("distanceTravelledKm", alternate = ["distance_travelled_km"]) val distanceTravelledKm: Double = 0.0,
    @SerializedName("accidentState", alternate = ["accident_state", "status"]) val accidentState: String = "SAFE"
)

/**
 * GPS / Cellular Location Coordinates & Metadata (Maps to Supabase locations table)
 */
data class LocationPoint(
    @SerializedName("id") val id: String? = null,
    @SerializedName("deviceId", alternate = ["device_id"]) val deviceId: String = "ACCIDENT-001",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("speedKmh", alternate = ["speed"]) val speedKmh: Double = 0.0,
    @SerializedName("altitudeM", alternate = ["altitude"]) val altitudeM: Double = 0.0,
    @SerializedName("bearing") val bearing: Float = 0f,
    @SerializedName("satellites") val satellites: Int = 0,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("locationSource", alternate = ["location_source"]) val locationSource: String = "GPS",
    @SerializedName("createdAt", alternate = ["created_at"]) val createdAt: String? = null
)

/**
 * Accident Incident Log & Alert Payload (Maps to Supabase accidents table)
 */
data class AccidentAlert(
    @SerializedName("id") val id: String? = "1",
    @SerializedName("deviceId", alternate = ["device_id"]) val deviceId: String? = "ACCIDENT-001",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("locationName", alternate = ["location_name"]) val locationName: String? = "Incident Location",
    @SerializedName("impactG", alternate = ["impact_g"]) val impactG: Double = 4.25,
    @SerializedName("tiltAngle", alternate = ["tilt_angle"]) val tiltAngle: Double = 49.0,
    @SerializedName("detectionType", alternate = ["detection_type"]) val detectionType: String? = "Impact + Tilt",
    @SerializedName("status") val status: String? = "CONFIRMED",
    @SerializedName("smsStatus", alternate = ["sms_status"]) val smsStatus: String? = "SENT",
    @SerializedName("callStatus", alternate = ["call_status"]) val callStatus: String? = "INITIATED",
    @SerializedName("locationSource", alternate = ["location_source"]) val locationSource: String? = "GPS",
    @SerializedName("createdAt", alternate = ["created_at"]) val createdAt: String? = null
)

/**
 * Emergency Contact details (Maps to Supabase emergency_contacts table)
 */
data class EmergencyContact(
    @SerializedName("id") val id: String = "CNT_01",
    @SerializedName("userId", alternate = ["user_id"]) val userId: String = "USER_001",
    @SerializedName("name") val name: String = "Emergency Contact",
    @SerializedName("phoneNumber", alternate = ["phone"]) val phoneNumber: String = "+919353035530",
    @SerializedName("relationship") val relationship: String = "Family",
    @SerializedName("isPrimary", alternate = ["is_primary"]) val isPrimary: Boolean = false,
    @SerializedName("syncState", alternate = ["sync_state"]) val syncState: String = "Synced with ESP32"
)

/**
 * Registered User / Rider Profile (Maps to Supabase users table)
 */
data class UserProfile(
    @SerializedName("id") val id: String? = "1",
    @SerializedName("fullName", alternate = ["name"]) val fullName: String? = "Rahul",
    @SerializedName("phoneNumber", alternate = ["phone"]) val phoneNumber: String? = "+919876543210",
    @SerializedName("email") val email: String? = "rahul.rider@gmail.com",
    @SerializedName("vehicleNumber", alternate = ["vehicle_number"]) val vehicleNumber: String? = "ACCIDENT-001",
    @SerializedName("vehicleModel", alternate = ["vehicle_model"]) val vehicleModel: String? = "Two-Wheeler (ESP32)",
    @SerializedName("bloodGroup", alternate = ["blood_group"]) val bloodGroup: String? = "O+ve",
    @SerializedName("medicalNotes", alternate = ["medical_notes"]) val medicalNotes: String? = "None"
)

/**
 * Historical Journey / Trip Data
 */
data class Trip(
    @SerializedName("id") val id: String,
    @SerializedName("deviceId", alternate = ["device_id"]) val deviceId: String = "ACCIDENT-001",
    @SerializedName("date") val date: String,
    @SerializedName("startTime", alternate = ["start_time"]) val startTime: String,
    @SerializedName("endTime", alternate = ["end_time"]) val endTime: String,
    @SerializedName("distanceKm", alternate = ["distance_km"]) val distanceKm: Double,
    @SerializedName("durationMinutes", alternate = ["duration_minutes"]) val durationMinutes: Int,
    @SerializedName("avgSpeedKmh", alternate = ["avg_speed_kmh"]) val avgSpeedKmh: Double,
    @SerializedName("maxSpeedKmh", alternate = ["max_speed_kmh"]) val maxSpeedKmh: Double,
    @SerializedName("startLocationName", alternate = ["start_location_name"]) val startLocationName: String,
    @SerializedName("endLocationName", alternate = ["end_location_name"]) val endLocationName: String,
    @SerializedName("routePoints", alternate = ["route_points"]) val routePoints: List<LocationPoint> = emptyList()
)

/**
 * Generic API Response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)
