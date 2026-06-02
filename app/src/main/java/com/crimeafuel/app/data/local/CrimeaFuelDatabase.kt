package com.crimeafuel.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crimeafuel.app.data.local.dao.StationDao
import com.crimeafuel.app.data.local.entity.StationEntity

@Database(
    entities = [StationEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CrimeaFuelDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        const val DATABASE_NAME = "crimea_fuel_db"
    }
}
