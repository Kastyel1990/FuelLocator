package com.crimeafuel.app.domain.model

data class FilterState(
    val fuelTypes: List<FuelType> = emptyList(),
    val regions: List<Region> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList()
) {
    val isDefault: Boolean
        get() = fuelTypes.isEmpty() && regions.isEmpty() && paymentMethods.isEmpty()

    fun matches(station: Station): Boolean {
        val matchesFuel = fuelTypes.isEmpty() || station.hasFuel(fuelTypes)
        val matchesRegion = regions.isEmpty() || station.region in regions
        val matchesPayment = paymentMethods.isEmpty() || station.acceptsPayment(paymentMethods)
        return matchesFuel && matchesRegion && matchesPayment
    }
}
