package com.crimeafuel.app.domain.usecase

import com.crimeafuel.app.domain.model.Station
import javax.inject.Inject
import kotlin.math.*

class GetNearestStationsUseCase @Inject constructor() {
    /**
     * Returns stations sorted by distance from user location.
     * @param userLat user latitude
     * @param userLng user longitude
     * @param limit max number of stations to return (0 = all)
     */
    operator fun invoke(
        stations: List<Station>,
        userLat: Double,
        userLng: Double,
        limit: Int = 0
    ): List<Pair<Station, Double>> {
        val sorted = stations.map { station ->
            station to distanceKm(userLat, userLng, station.latitude, station.longitude)
        }.sortedBy { it.second }

        return if (limit > 0) sorted.take(limit) else sorted
    }

    companion object {
        /**
         * Haversine formula - distance between two points on Earth in km
         */
        fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        /**
         * Format distance for display
         */
        fun formatDistance(distanceKm: Double): String {
            return if (distanceKm < 1.0) {
                "${(distanceKm * 1000).toInt()} м"
            } else {
                String.format("%.1f км", distanceKm)
            }
        }
    }
}
