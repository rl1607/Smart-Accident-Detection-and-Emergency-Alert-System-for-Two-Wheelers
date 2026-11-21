package com.example.smartaccidentdetection.util

import com.example.smartaccidentdetection.data.model.LocationPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {

    /**
     * Calculates the true cumulative distance in kilometers along a list of real GPS trackpoints
     * using the Haversine formula. Filters stationary duplicates, GPS multipath jumps,
     * and session gaps (> 20 mins between sequential telemetry updates).
     */
    fun calculateTotalDistanceKm(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            // Skip invalid coordinates
            if (p1.latitude == 0.0 || p1.longitude == 0.0 || p2.latitude == 0.0 || p2.longitude == 0.0) continue
            // Skip duplicate stationary points
            if (p1.latitude == p2.latitude && p1.longitude == p2.longitude) continue
            // Skip disconnected sessions where time gap between GPS updates is > 20 minutes
            val t1 = DateTimeUtils.parseToEpochMillis(p1.createdAt, p1.timestamp)
            val t2 = DateTimeUtils.parseToEpochMillis(p2.createdAt, p2.timestamp)
            if (t1 > 0 && t2 > 0 && Math.abs(t2 - t1) > 20 * 60 * 1000) {
                continue
            }
            val dist = haversineKm(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            // Filter GPS multipath jumps (> 20 km between sequential packets is erroneous)
            if (dist < 20.0) {
                total += dist
            }
        }
        return total
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
