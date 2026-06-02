package com.crimeafuel.app.domain.repository

import com.crimeafuel.app.domain.model.FuelStatus
import com.crimeafuel.app.domain.model.PaymentMethod
import com.crimeafuel.app.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    /**
     * Get all stations as a Flow (real-time updates from Firestore)
     */
    fun getAllStations(): Flow<List<Station>>

    /**
     * Get a single station by ID
     */
    suspend fun getStation(stationId: String): Station?

    /**
     * Update fuel statuses for a station
     */
    suspend fun updateFuelStatus(
        stationId: String,
        fuelStatuses: List<FuelStatus>,
        paymentMethods: List<PaymentMethod>,
        comment: String?,
        userId: String
    )

    /**
     * Seed initial data (only if Firestore collection is empty)
     */
    suspend fun seedInitialData()

    suspend fun refresh()

    /**
     * Add a user created station
     */
    suspend fun addStation(station: Station)

    /**
     * Delete a station with a reason
     */
    suspend fun deleteStation(stationId: String, reason: String, userId: String)
}
