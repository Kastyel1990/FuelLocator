package com.crimeafuel.app.data.remote

import com.crimeafuel.app.data.local.entity.StationEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val STATIONS_COLLECTION = "stations"
        const val UPDATES_COLLECTION = "updates"
    }

    fun getAllStations(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listener = firestore.collection(STATIONS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val stations = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)
                } ?: emptyList()
                trySend(stations)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getStation(stationId: String): Map<String, Any>? {
        val doc = firestore.collection(STATIONS_COLLECTION)
            .document(stationId)
            .get()
            .await()
        return doc.data?.plus("id" to doc.id)
    }

    suspend fun updateStation(stationId: String, data: Map<String, Any>) {
        firestore.collection(STATIONS_COLLECTION)
            .document(stationId)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun addUpdateRecord(record: Map<String, Any>) {
        firestore.collection(UPDATES_COLLECTION)
            .add(record)
            .await()
    }

    suspend fun setStation(stationId: String, data: Map<String, Any>) {
        firestore.collection(STATIONS_COLLECTION)
            .document(stationId)
            .set(data)
            .await()
    }

    suspend fun getStationsCount(): Int {
        val snapshot = firestore.collection(STATIONS_COLLECTION)
            .get()
            .await()
        return snapshot.size()
    }
}
