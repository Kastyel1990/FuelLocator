package com.crimeafuel.app.data.repository

import com.crimeafuel.app.data.local.dao.StationDao
import com.crimeafuel.app.data.remote.FirestoreDataSource
import com.crimeafuel.app.data.remote.dto.StationMapper
import com.crimeafuel.app.data.seed.InitialStationsData
import com.crimeafuel.app.domain.model.FuelStatus
import com.crimeafuel.app.domain.model.PaymentMethod
import com.crimeafuel.app.domain.model.Station
import com.crimeafuel.app.domain.repository.StationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import java.util.UUID
import com.crimeafuel.app.domain.model.*

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val stationDao: StationDao,
    private val firestoreDataSource: FirestoreDataSource,
    @ApplicationContext private val context: Context
) : StationRepository {

    override fun getAllStations(): Flow<List<Station>> {
        return stationDao.getAllStations()
            .map { entities ->
                entities.map { StationMapper.entityToDomain(it) }
            }
            .onStart {
                // Always ensure local seed data is present in Room first
                ensureLocalData()
                // Try to sync from Firestore on first collection
                try {
                    syncFromFirestore()
                } catch (_: Exception) {
                    // Ignore sync errors, we already have local/cached data
                }
            }
    }

    override suspend fun getStation(stationId: String): Station? {
        val entity = stationDao.getStation(stationId)
        return entity?.let { StationMapper.entityToDomain(it) }
    }

    override suspend fun updateFuelStatus(
        stationId: String,
        fuelStatuses: List<FuelStatus>,
        paymentMethods: List<PaymentMethod>,
        comment: String?,
        userId: String
    ) {
        // Get current station data
        val currentStation = stationDao.getStation(stationId)
            ?: throw IllegalArgumentException("Station not found: $stationId")

        val currentDomain = StationMapper.entityToDomain(currentStation)

        // Build update data for Firestore
        val fuelMap = mutableMapOf<String, String>()
        fuelStatuses.forEach { fuelMap[it.fuelType.name] = it.availability.name }
        val paymentList = paymentMethods.map { it.name }

        val updateData = mapOf(
            "fuelStatuses" to fuelMap,
            "paymentMethods" to paymentList,
            "lastUpdated" to Timestamp.now(),
            "lastUpdatedBy" to userId,
            "comment" to (comment ?: ""),
            "isVerified" to false
        )

        // Save update record for history
        val previousFuelMap = mutableMapOf<String, String>()
        currentDomain.fuelStatuses.forEach {
            previousFuelMap[it.fuelType.name] = it.availability.name
        }

        val updateRecord = mapOf(
            "stationId" to stationId,
            "userId" to userId,
            "timestamp" to Timestamp.now(),
            "previousStatus" to previousFuelMap,
            "newStatus" to fuelMap,
            "comment" to (comment ?: "")
        )

        // Update Firestore
        try {
            kotlinx.coroutines.withTimeout(2000L) {
                firestoreDataSource.updateStation(stationId, updateData)
                firestoreDataSource.addUpdateRecord(updateRecord)
            }
        } catch (_: Exception) {
            // If Firestore fails or times out, still update locally
        }

        // Update local cache
        val updatedStation = currentDomain.copy(
            fuelStatuses = fuelStatuses,
            paymentMethods = paymentMethods,
            lastUpdated = System.currentTimeMillis(),
            lastUpdatedBy = userId,
            comment = comment,
            isVerified = false
        )
        stationDao.update(StationMapper.domainToEntity(updatedStation))
    }

    override suspend fun seedInitialData() {
        try {
            val stations = InitialStationsData.getAllStations()
            // Fetch existing Firestore IDs to avoid overwriting crowd-sourced edits
            val snapshot = kotlinx.coroutines.withTimeout(5000L) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(FirestoreDataSource.STATIONS_COLLECTION)
                    .get()
                    .await()
            }
            val existingIds = snapshot.documents.map { it.id }.toSet()
            val missingStations = stations.filter { it.id !in existingIds }

            if (missingStations.isNotEmpty()) {
                // Seed Firestore in chunks of 50 in parallel to prevent timeout/exhaustion
                missingStations.chunked(50).forEach { chunk ->
                    coroutineScope {
                        chunk.map { station ->
                            async {
                                try {
                                    kotlinx.coroutines.withTimeout(3000L) {
                                        firestoreDataSource.setStation(station.id, StationMapper.domainToFirestore(station))
                                    }
                                } catch (_: Exception) {
                                    // Ignore individual write failures
                                }
                            }
                        }.awaitAll()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore overall seeding/network failures
        }
        ensureLocalData()
    }

    override suspend fun refresh() {
        syncFromFirestore()
    }

    private suspend fun syncFromFirestore() {
        try {
            // One-shot fetch from Firestore with timeout
            val snapshot = kotlinx.coroutines.withTimeout(3000L) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(FirestoreDataSource.STATIONS_COLLECTION)
                    .get()
                    .await()
            }

            val entities = snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.plus("id" to doc.id) ?: return@mapNotNull null
                StationMapper.firestoreToEntity(data)
            }

            if (entities.isNotEmpty()) {
                stationDao.insertAll(entities)
            }
        } catch (_: Exception) {
            // Firestore unavailable, use local data
        }
    }

    private suspend fun ensureLocalData() {
        val stations = InitialStationsData.getAllStations()
        val localCount = stationDao.getCount()
        if (localCount < stations.size) {
            // Seed local DB from built-in data, inserting only missing ones to protect local edits
            val existingEntities = try {
                stationDao.getAllStations().first()
            } catch (_: Exception) {
                emptyList()
            }
            val existingIds = existingEntities.map { it.id }.toSet()
            val missingStations = stations.filter { it.id !in existingIds }
            
            if (missingStations.isNotEmpty()) {
                val entities = missingStations.map { StationMapper.domainToEntity(it) }
                stationDao.insertAll(entities)
            }
        }
    }

    override suspend fun addStation(station: Station) {
        // Save to local cache
        val entity = StationMapper.domainToEntity(station)
        stationDao.insert(entity)

        // Try to sync with Firestore
        try {
            kotlinx.coroutines.withTimeout(2000L) {
                firestoreDataSource.setStation(station.id, StationMapper.domainToFirestore(station))
            }
        } catch (_: Exception) {
            // Ignore if offline, mocked, or timeout
        }
    }

    override suspend fun deleteStation(stationId: String, reason: String, userId: String) {
        val deletionRecord = mapOf(
            "stationId" to stationId,
            "reason" to reason,
            "userId" to userId,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        try {
            kotlinx.coroutines.withTimeout(3000L) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("deletions")
                    .add(deletionRecord)
                    .await()
                    
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection(FirestoreDataSource.STATIONS_COLLECTION)
                    .document(stationId)
                    .delete()
                    .await()
            }
        } catch (_: Exception) {
            // Proceed to local deletion even if offline
        }
        stationDao.deleteStation(stationId)
    }

    private fun getInitialStationsFromJson(): List<Station> {
        val stations = mutableListOf<Station>()
        try {
            val jsonString = context.assets.open("initial_stations.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val network = obj.getString("network")
                val address = obj.getString("address")
                val lat = obj.getDouble("latitude")
                val lng = obj.getDouble("longitude")
                
                val fuelStatusObj = obj.getJSONObject("fuelStatus")
                val ai92 = Availability.valueOf(fuelStatusObj.optString("ai_92", "NOT_AVAILABLE"))
                val ai95 = Availability.valueOf(fuelStatusObj.optString("ai_95", "NOT_AVAILABLE"))
                val ai95p = Availability.valueOf(fuelStatusObj.optString("ai_95_plus", "NOT_AVAILABLE"))
                val ai100 = Availability.valueOf(fuelStatusObj.optString("ai_100", "NOT_AVAILABLE"))
                val dt = Availability.valueOf(fuelStatusObj.optString("dt", "NOT_AVAILABLE"))
                val dtp = Availability.valueOf(fuelStatusObj.optString("dt_plus", "NOT_AVAILABLE"))
                val gas = Availability.valueOf(fuelStatusObj.optString("gas", "NOT_AVAILABLE"))
                
                val fuelStatuses = listOf(
                    FuelStatus(FuelType.AI_92, ai92),
                    FuelStatus(FuelType.AI_95, ai95),
                    FuelStatus(FuelType.AI_95_PLUS, ai95p),
                    FuelStatus(FuelType.AI_100, ai100),
                    FuelStatus(FuelType.DT, dt),
                    FuelStatus(FuelType.DT_PLUS, dtp),
                    FuelStatus(FuelType.GAS, gas)
                )

                // paymentMethods default
                val paymentMethods = mutableListOf<PaymentMethod>()
                if (fuelStatuses.any { it.availability == Availability.CARDS_ONLY }) {
                    paymentMethods.add(PaymentMethod.CARDS)
                }
                if (fuelStatuses.any { it.availability == Availability.FREE_SALE }) {
                    paymentMethods.add(PaymentMethod.CASH)
                    paymentMethods.add(PaymentMethod.CASHLESS)
                }
                if (paymentMethods.isEmpty()) {
                    paymentMethods.addAll(PaymentMethod.entries)
                }

                val station = Station(
                    id = UUID.randomUUID().toString(),
                    number = "-",
                    network = network,
                    address = address,
                    region = Region.SIMFEROPOL, // arbitrary
                    latitude = lat,
                    longitude = lng,
                    fuelStatuses = fuelStatuses,
                    paymentMethods = paymentMethods,
                    lastUpdated = System.currentTimeMillis(),
                    lastUpdatedBy = "system",
                    comment = null,
                    isVerified = true
                )
                stations.add(station)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // fallback to InitialStationsData
            return InitialStationsData.getAllStations()
        }
        return stations
    }
}
