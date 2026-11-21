package com.example.smartaccidentdetection.data.repository

import com.example.smartaccidentdetection.data.model.AccidentAlert
import com.example.smartaccidentdetection.data.model.DeviceStatus
import com.example.smartaccidentdetection.data.model.EmergencyContact
import com.example.smartaccidentdetection.data.model.LocationPoint
import com.example.smartaccidentdetection.data.model.Trip
import com.example.smartaccidentdetection.data.model.UserProfile

object MockDataProvider {

    val defaultUserProfile = UserProfile(
        id = "USER_001",
        fullName = "Rahul Sharma",
        phoneNumber = "+919876543210",
        email = "rahul.sharma@example.com",
        vehicleNumber = "TS 09 EA 4589",
        vehicleModel = "Yamaha MT-15 (Two-Wheeler)",
        bloodGroup = "B+ve",
        medicalNotes = "No existing chronic conditions. Wears spectacles."
    )

    val defaultDeviceStatus = DeviceStatus(
        deviceId = "ACCIDENT-001",
        isOnline = true,
        batteryPercent = 88,
        gsmSignalDbm = -68,
        gsmNetworkStatus = "Registered (4G / 2G GPRS Connected)",
        gpsFix = true,
        satellites = 9,
        mpuStatus = "Active (Calibrated: 45° Tilt, 4.0g Impact)",
        lastSeen = "2 seconds ago",
        locationSource = "GPS",
        emergencySyncStatus = "Synced",
        lastSyncTimestamp = "Today, 09:15 AM",
        currentSpeedKmh = 42.5,
        distanceTravelledKm = 18.3,
        accidentState = "SAFE"
    )

    val sampleContacts = mutableListOf(
        EmergencyContact(
            id = "CNT_01",
            userId = "USER_001",
            name = "Anjali Sharma",
            phoneNumber = "+917760407737",
            relationship = "Spouse",
            isPrimary = true,
            syncState = "Synced with ESP32"
        ),
        EmergencyContact(
            id = "CNT_02",
            userId = "USER_001",
            name = "Vikram Sharma",
            phoneNumber = "+919876543212",
            relationship = "Brother",
            isPrimary = false,
            syncState = "Synced with ESP32"
        ),
        EmergencyContact(
            id = "CNT_03",
            userId = "USER_001",
            name = "Dr. K. S. Rao",
            phoneNumber = "+919876543213",
            relationship = "Family Doctor",
            isPrimary = false,
            syncState = "Synced with ESP32"
        )
    )

    // Realistic route points representing the vehicle in Bellary
    val sampleRoutePoints = listOf(
        LocationPoint(id = "LOC_01", deviceId = "ACCIDENT-001", latitude = 15.13822, longitude = 76.90379, speedKmh = 0.0, bearing = 45f, locationSource = "GPS", timestamp = System.currentTimeMillis() - 60000),
        LocationPoint(id = "LOC_02", deviceId = "ACCIDENT-001", latitude = 15.13828, longitude = 76.90377, speedKmh = 12.0, bearing = 50f, locationSource = "GPS", timestamp = System.currentTimeMillis() - 40000),
        LocationPoint(id = "LOC_03", deviceId = "ACCIDENT-001", latitude = 15.13831, longitude = 76.90381, speedKmh = 18.5, bearing = 55f, locationSource = "GPS", timestamp = System.currentTimeMillis() - 20000),
        LocationPoint(id = "LOC_04", deviceId = "ACCIDENT-001", latitude = 15.13833, longitude = 76.90382, speedKmh = 15.0, bearing = 60f, locationSource = "GPS", timestamp = System.currentTimeMillis())
    )

    val sampleAlerts = mutableListOf(
        AccidentAlert(
            id = "ALT_101",
            deviceId = "ACCIDENT-001",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 2),
            latitude = 15.138220,
            longitude = 76.903790,
            locationName = "Station Road, Bellary",
            impactG = 4.25,
            tiltAngle = 49.0,
            detectionType = "High Impact (4.25g) + Tilt (49°)",
            status = "CANCELLED_BY_RIDER",
            smsStatus = "Cancelled (Safe)",
            callStatus = "Aborted (Safe)",
            locationSource = "GPS"
        ),
        AccidentAlert(
            id = "ALT_102",
            deviceId = "ACCIDENT-001",
            timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24),
            latitude = 15.138304,
            longitude = 76.903786,
            locationName = "Jawans Road, Bellary",
            impactG = 4.35,
            tiltAngle = 52.0,
            detectionType = "Severe Impact (4.35g)",
            status = "CONFIRMED",
            smsStatus = "Sent to Emergency Contacts",
            callStatus = "Completed",
            locationSource = "GPS"
        )
    )

    val sampleTrips = listOf(
        Trip(
            id = "TRIP_01",
            deviceId = "ACCIDENT-001",
            date = "Today",
            startTime = "08:30 AM",
            endTime = "09:12 AM",
            distanceKm = 18.3,
            durationMinutes = 42,
            avgSpeedKmh = 26.1,
            maxSpeedKmh = 58.0,
            startLocationName = "Kondapur, Hyderabad",
            endLocationName = "Banjara Hills Road 12",
            routePoints = sampleRoutePoints
        ),
        Trip(
            id = "TRIP_02",
            deviceId = "ACCIDENT-001",
            date = "Yesterday",
            startTime = "06:15 PM",
            endTime = "07:05 PM",
            distanceKm = 21.7,
            durationMinutes = 50,
            avgSpeedKmh = 28.4,
            maxSpeedKmh = 62.5,
            startLocationName = "Gachibowli Financial District",
            endLocationName = "Madhapur Metro",
            routePoints = sampleRoutePoints.reversed()
        ),
        Trip(
            id = "TRIP_03",
            deviceId = "ACCIDENT-001",
            date = "28 Aug 2026",
            startTime = "10:00 AM",
            endTime = "10:35 AM",
            distanceKm = 14.2,
            durationMinutes = 35,
            avgSpeedKmh = 24.3,
            maxSpeedKmh = 52.0,
            startLocationName = "Kukatpally Housing Board",
            endLocationName = "Inorbit Mall, Cyberabad",
            routePoints = sampleRoutePoints
        )
    )
}
