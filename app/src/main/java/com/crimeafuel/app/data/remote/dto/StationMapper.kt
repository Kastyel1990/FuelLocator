package com.crimeafuel.app.data.remote.dto

import com.crimeafuel.app.data.local.entity.StationEntity
import com.crimeafuel.app.domain.model.*
import org.json.JSONObject
import org.json.JSONArray

/**
 * Maps between Firestore documents, Room entities, and domain models.
 */
object StationMapper {

    fun entityToDomain(entity: StationEntity): Station {
        val fuelStatuses = parseFuelStatuses(entity.fuelStatusesJson)
        val paymentMethods = parsePaymentMethods(entity.paymentMethodsJson)

        return Station(
            id = entity.id,
            number = entity.number,
            network = entity.network,
            address = entity.address,
            region = Region.fromDisplayName(entity.region),
            latitude = entity.latitude,
            longitude = entity.longitude,
            fuelStatuses = fuelStatuses,
            paymentMethods = paymentMethods,
            lastUpdated = entity.lastUpdated,
            lastUpdatedBy = entity.lastUpdatedBy,
            comment = entity.comment,
            isVerified = entity.isVerified,
            isUserAdded = entity.isUserAdded
        )
    }

    fun domainToEntity(station: Station): StationEntity {
        return StationEntity(
            id = station.id,
            number = station.number,
            network = station.network,
            address = station.address,
            region = station.region.displayName,
            latitude = station.latitude,
            longitude = station.longitude,
            fuelStatusesJson = fuelStatusesToJson(station.fuelStatuses),
            paymentMethodsJson = paymentMethodsToJson(station.paymentMethods),
            lastUpdated = station.lastUpdated,
            lastUpdatedBy = station.lastUpdatedBy,
            comment = station.comment,
            isVerified = station.isVerified,
            isUserAdded = station.isUserAdded
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun firestoreToEntity(data: Map<String, Any>): StationEntity {
        val fuelStatuses = (data["fuelStatuses"] as? Map<String, String>) ?: emptyMap()
        val paymentMethods = (data["paymentMethods"] as? List<String>) ?: emptyList()

        return StationEntity(
            id = data["id"] as? String ?: "",
            number = data["number"] as? String ?: "",
            network = data["network"] as? String ?: "",
            address = data["address"] as? String ?: "",
            region = data["region"] as? String ?: "",
            latitude = (data["lat"] as? Number)?.toDouble() ?: 0.0,
            longitude = (data["lng"] as? Number)?.toDouble() ?: 0.0,
            fuelStatusesJson = JSONObject(fuelStatuses).toString(),
            paymentMethodsJson = JSONArray(paymentMethods).toString(),
            lastUpdated = (data["lastUpdated"] as? com.google.firebase.Timestamp)
                ?.toDate()?.time ?: (data["lastUpdated"] as? Long) ?: 0L,
            lastUpdatedBy = data["lastUpdatedBy"] as? String,
            comment = data["comment"] as? String,
            isVerified = data["isVerified"] as? Boolean ?: false,
            isUserAdded = data["isUserAdded"] as? Boolean ?: false
        )
    }

    fun domainToFirestore(station: Station): Map<String, Any> {
        val fuelMap = mutableMapOf<String, String>()
        station.fuelStatuses.forEach { status ->
            fuelMap[status.fuelType.name] = status.availability.name
        }

        val paymentList = station.paymentMethods.map { it.name }

        return mapOf(
            "number" to station.number,
            "network" to station.network,
            "address" to station.address,
            "region" to station.region.displayName,
            "lat" to station.latitude,
            "lng" to station.longitude,
            "fuelStatuses" to fuelMap,
            "paymentMethods" to paymentList,
            "lastUpdated" to com.google.firebase.Timestamp.now(),
            "lastUpdatedBy" to (station.lastUpdatedBy ?: ""),
            "comment" to (station.comment ?: ""),
            "isVerified" to station.isVerified,
            "isUserAdded" to station.isUserAdded
        )
    }

    private fun parseFuelStatuses(json: String): List<FuelStatus> {
        val result = mutableListOf<FuelStatus>()
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                val fuelType = try { FuelType.valueOf(key) } catch (_: Exception) { null }
                val availability = try {
                    Availability.valueOf(obj.getString(key))
                } catch (_: Exception) {
                    Availability.fromCode(obj.getString(key))
                }
                if (fuelType != null) {
                    result.add(FuelStatus(fuelType, availability))
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun parsePaymentMethods(json: String): List<PaymentMethod> {
        val result = mutableListOf<PaymentMethod>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                try {
                    result.add(PaymentMethod.valueOf(arr.getString(i)))
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return result
    }

    private fun fuelStatusesToJson(statuses: List<FuelStatus>): String {
        val map = mutableMapOf<String, String>()
        statuses.forEach { map[it.fuelType.name] = it.availability.name }
        return JSONObject(map as Map<*, *>).toString()
    }

    private fun paymentMethodsToJson(methods: List<PaymentMethod>): String {
        return JSONArray(methods.map { it.name }).toString()
    }
}
