package com.crimeafuel.app.domain.model

enum class Availability(val displayName: String, val code: String) {
    FREE_SALE("Свободная продажа", "СП"),
    CARDS_ONLY("Талоны/Топл. карты", "ТК"),
    NOT_AVAILABLE("Нет в наличии", "—"),
    UNKNOWN("Нет данных", "?");

    companion object {
        fun fromCode(code: String): Availability {
            return when (code.trim().uppercase()) {
                "СП", "SP", "FREE_SALE" -> FREE_SALE
                "ТК", "TK", "CARDS_ONLY" -> CARDS_ONLY
                "—", "-", "NOT_AVAILABLE", "" -> NOT_AVAILABLE
                else -> UNKNOWN
            }
        }
    }
}

data class FuelStatus(
    val fuelType: FuelType,
    val availability: Availability
)
