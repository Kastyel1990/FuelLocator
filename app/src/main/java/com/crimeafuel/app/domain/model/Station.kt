package com.crimeafuel.app.domain.model

data class Station(
    val id: String,
    val number: String,
    val network: String,
    val address: String,
    val region: Region,
    val latitude: Double,
    val longitude: Double,
    val fuelStatuses: List<FuelStatus>,
    val paymentMethods: List<PaymentMethod>,
    val lastUpdated: Long, // millis since epoch
    val lastUpdatedBy: String?,
    val comment: String?,
    val isVerified: Boolean,
    val isUserAdded: Boolean = false
) {
    /**
     * Returns the "best" availability across all selected fuel types.
     * Priority: FREE_SALE > CARDS_ONLY > NOT_AVAILABLE > UNKNOWN
     */
    fun bestAvailability(fuelTypes: List<FuelType>? = null): Availability {
        val statuses = if (fuelTypes.isNullOrEmpty()) {
            fuelStatuses
        } else {
            fuelStatuses.filter { it.fuelType in fuelTypes }
        }
        return when {
            statuses.any { it.availability == Availability.FREE_SALE } -> Availability.FREE_SALE
            statuses.any { it.availability == Availability.CARDS_ONLY } -> Availability.CARDS_ONLY
            statuses.any { it.availability == Availability.NOT_AVAILABLE } -> Availability.NOT_AVAILABLE
            else -> Availability.UNKNOWN
        }
    }

    /**
     * Check if station has any of the specified fuel types available
     */
    fun hasFuel(fuelTypes: List<FuelType>): Boolean {
        if (fuelTypes.isEmpty()) return true
        return fuelStatuses.any { status ->
            status.fuelType in fuelTypes &&
                    (status.availability == Availability.FREE_SALE ||
                            status.availability == Availability.CARDS_ONLY)
        }
    }

    /**
     * Check if station accepts any of the specified payment methods
     */
    fun acceptsPayment(methods: List<PaymentMethod>): Boolean {
        if (methods.isEmpty()) return true
        return paymentMethods.any { it in methods }
    }
}
