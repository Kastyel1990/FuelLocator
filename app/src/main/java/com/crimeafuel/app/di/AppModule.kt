package com.crimeafuel.app.di

import com.crimeafuel.app.data.repository.AuthRepositoryImpl
import com.crimeafuel.app.data.repository.StationRepositoryImpl
import com.crimeafuel.app.domain.repository.AuthRepository
import com.crimeafuel.app.domain.repository.StationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindStationRepository(impl: StationRepositoryImpl): StationRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
