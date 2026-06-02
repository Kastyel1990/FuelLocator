package com.crimeafuel.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.crimeafuel.app.data.local.Converters

@Entity(tableName = "stations")
@TypeConverters(Converters::class)
data class StationEntity(
    @PrimaryKey
    val id: String,
    val number: String,
    val network: String,
    val address: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val fuelStatusesJson: String, // JSON map: {"AI_92":"FREE_SALE","AI_95":"CARDS_ONLY",...}
    val paymentMethodsJson: String, // JSON list: ["CARDS","CASHLESS"]
    val lastUpdated: Long,
    val lastUpdatedBy: String?,
    val comment: String?,
    val isVerified: Boolean,
    val isUserAdded: Boolean
)
