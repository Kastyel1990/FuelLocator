package com.crimeafuel.app.presentation.navigation

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Login : Screen("login")
    data object Settings : Screen("settings")
    data object EditFuel : Screen("edit_fuel/{stationId}") {
        fun createRoute(stationId: String) = "edit_fuel/$stationId"
    }

    companion object {
        const val STATION_ID_ARG = "stationId"
    }
}
