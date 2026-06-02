package com.crimeafuel.app.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crimeafuel.app.domain.model.*
import com.crimeafuel.app.domain.repository.AuthRepository
import com.crimeafuel.app.domain.repository.StationRepository
import com.crimeafuel.app.domain.usecase.FilterStationsUseCase
import com.crimeafuel.app.domain.usecase.GetNearestStationsUseCase
import com.crimeafuel.app.domain.usecase.GetStationsUseCase
import com.crimeafuel.app.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val allStations: List<Station> = emptyList(),
    val filteredStations: List<Station> = emptyList(),
    val selectedStation: Station? = null,
    val filterState: FilterState = FilterState(),
    val userLocation: Location? = null,
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val importedStation: com.crimeafuel.app.util.ImportedStationData? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val getStationsUseCase: GetStationsUseCase,
    private val filterStationsUseCase: FilterStationsUseCase,
    private val getNearestStationsUseCase: GetNearestStationsUseCase,
    private val stationRepository: StationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadStations()
        observeAuth()
        seedDataIfNeeded()
        observeSharedIntent()
    }

    private fun observeSharedIntent() {
        viewModelScope.launch {
            com.crimeafuel.app.util.SharedIntentManager.sharedText.collect { text ->
                if (text != null) {
                    processSharedText(text)
                    com.crimeafuel.app.util.SharedIntentManager.clear()
                }
            }
        }
    }

    fun processSharedText(text: String, fallbackLat: Double? = null, fallbackLng: Double? = null) {
        viewModelScope.launch {
            val imported = com.crimeafuel.app.util.YandexMapsParser.parseSharedText(text)
            if (imported != null) {
                val finalLat = imported.lat ?: fallbackLat
                val finalLng = imported.lng ?: fallbackLng

                if (finalLat != null && finalLng != null) {
                    // Check if exists within 50 meters
                    val exists = _uiState.value.allStations.any { station ->
                        GetNearestStationsUseCase.distanceKm(
                            station.latitude, station.longitude, finalLat, finalLng
                        ) <= 0.05
                    }
                    
                    if (exists) {
                        _uiState.update { it.copy(error = "Эта заправка уже есть на карте (в радиусе 50м)") }
                    } else {
                        _uiState.update { it.copy(importedStation = imported.copy(lat = finalLat, lng = finalLng)) }
                    }
                } else {
                    _uiState.update { it.copy(error = "Ссылка не содержит точных координат. Нажмите на карту и вставьте из буфера.") }
                }
            } else {
                _uiState.update { it.copy(error = "Не удалось распознать ссылку Яндекс.Карт") }
            }
        }
    }

    fun clearImportedStation() {
        _uiState.update { it.copy(importedStation = null) }
    }

    private fun loadStations() {
        viewModelScope.launch {
            getStationsUseCase()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { stations ->
                    _uiState.update { state ->
                        val filtered = filterStationsUseCase(stations, state.filterState)
                        val updatedSelected = state.selectedStation?.let { current -> 
                            stations.find { it.id == current.id } ?: current 
                        }
                        state.copy(
                            allStations = stations,
                            filteredStations = filtered,
                            selectedStation = updatedSelected,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.update { it.copy(isLoggedIn = loggedIn) }
            }
        }
    }

    private fun seedDataIfNeeded() {
        viewModelScope.launch {
            try {
                stationRepository.seedInitialData()
            } catch (_: Exception) {
                // Seed errors are non-fatal
            }
        }
    }

    fun selectStation(station: Station?) {
        _uiState.update { it.copy(selectedStation = station) }
    }

    fun updateFilter(filterState: FilterState) {
        _uiState.update { state ->
            val filtered = filterStationsUseCase(state.allStations, filterState)
            state.copy(filterState = filterState, filteredStations = filtered)
        }
    }

    fun toggleFuelTypeFilter(fuelType: FuelType) {
        val currentFilter = _uiState.value.filterState
        val currentTypes = currentFilter.fuelTypes.toMutableList()
        if (fuelType in currentTypes) {
            currentTypes.remove(fuelType)
        } else {
            currentTypes.add(fuelType)
        }
        updateFilter(currentFilter.copy(fuelTypes = currentTypes))
    }

    fun toggleRegionFilter(region: Region) {
        val currentFilter = _uiState.value.filterState
        val currentRegions = currentFilter.regions.toMutableList()
        if (region in currentRegions) {
            currentRegions.remove(region)
        } else {
            currentRegions.add(region)
        }
        updateFilter(currentFilter.copy(regions = currentRegions))
    }

    fun togglePaymentFilter(method: PaymentMethod) {
        val currentFilter = _uiState.value.filterState
        val currentMethods = currentFilter.paymentMethods.toMutableList()
        if (method in currentMethods) {
            currentMethods.remove(method)
        } else {
            currentMethods.add(method)
        }
        updateFilter(currentFilter.copy(paymentMethods = currentMethods))
    }

    fun clearFilters() {
        updateFilter(FilterState())
    }

    fun setUserLocation(location: Location) {
        _uiState.update { it.copy(userLocation = location) }
    }

    fun getDistanceToStation(station: Station): String? {
        val location = _uiState.value.userLocation ?: return null
        val distance = GetNearestStationsUseCase.distanceKm(
            location.latitude, location.longitude,
            station.latitude, station.longitude
        )
        return GetNearestStationsUseCase.formatDistance(distance)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                stationRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun addUserStation(network: String, address: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val newStation = Station(
                    id = java.util.UUID.randomUUID().toString(),
                    number = "User",
                    network = network,
                    address = address,
                    region = Region.SIMFEROPOL, // Default
                    latitude = latitude,
                    longitude = longitude,
                    fuelStatuses = emptyList(), // Can be updated later
                    paymentMethods = emptyList(),
                    lastUpdated = System.currentTimeMillis(),
                    lastUpdatedBy = "user",
                    comment = "User added station",
                    isVerified = false,
                    isUserAdded = true
                )
                stationRepository.addStation(newStation)
                // refresh stations
                stationRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при добавлении АЗС: ${e.message}") }
            }
        }
    }

    fun deleteStation(stationId: String, reason: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId
            if (userId == null) {
                _uiState.update { it.copy(error = "Необходимо авторизоваться для удаления") }
                return@launch
            }
            try {
                stationRepository.deleteStation(stationId, reason, userId)
                // Deselect the deleted station if it was selected
                _uiState.update { state ->
                    val selected = if (state.selectedStation?.id == stationId) null else state.selectedStation
                    state.copy(selectedStation = selected)
                }
                // Refresh to sync/update list
                stationRepository.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при удалении: ${e.message}") }
            }
        }
    }
}
