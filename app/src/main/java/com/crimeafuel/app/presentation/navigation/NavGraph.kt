package com.crimeafuel.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crimeafuel.app.presentation.auth.LoginScreen
import com.crimeafuel.app.presentation.edit.EditFuelScreen
import com.crimeafuel.app.presentation.map.MapScreen

@Composable
fun CrimeaFuelNavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToEdit = { stationId ->
                    navController.navigate(Screen.EditFuel.createRoute(stationId))
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.popBackStack()
                },
                onSkip = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            com.crimeafuel.app.presentation.settings.SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditFuel.route,
            arguments = listOf(
                navArgument(Screen.STATION_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getString(Screen.STATION_ID_ARG) ?: return@composable
            EditFuelScreen(
                stationId = stationId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
