package com.crimeafuel.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

import javax.inject.Inject

@HiltAndroidApp
class CrimeFuelApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Configure Yandex MapKit
        com.yandex.mapkit.MapKitFactory.setApiKey("81eabc5c-bddb-4254-912b-b1afe666d851")
        com.yandex.mapkit.MapKitFactory.initialize(this)
    }
}
