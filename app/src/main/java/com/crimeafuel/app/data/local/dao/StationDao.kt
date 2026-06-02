package com.crimeafuel.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crimeafuel.app.data.local.entity.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY network, number")
    fun getAllStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE id = :stationId")
    suspend fun getStation(stationId: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stations: List<StationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(station: StationEntity)

    @Update
    suspend fun update(station: StationEntity)

    @Query("SELECT COUNT(*) FROM stations")
    suspend fun getCount(): Int

    @Query("DELETE FROM stations")
    suspend fun deleteAll()

    @Query("DELETE FROM stations WHERE id = :stationId")
    suspend fun deleteStation(stationId: String)

    @Query("SELECT * FROM stations WHERE region = :region ORDER BY network, number")
    fun getStationsByRegion(region: String): Flow<List<StationEntity>>
}
