package com.crimeafuel.app.di

import android.content.Context
import androidx.room.Room
import com.crimeafuel.app.data.local.CrimeaFuelDatabase
import com.crimeafuel.app.data.local.dao.StationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CrimeaFuelDatabase {
        return Room.databaseBuilder(
            context,
            CrimeaFuelDatabase::class.java,
            CrimeaFuelDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
         .build()
    }

    @Provides
    fun provideStationDao(database: CrimeaFuelDatabase): StationDao {
        return database.stationDao()
    }
}
