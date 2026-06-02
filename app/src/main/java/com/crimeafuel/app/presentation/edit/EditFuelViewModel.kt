package com.crimeafuel.app.presentation.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crimeafuel.app.domain.model.*
import com.crimeafuel.app.domain.repository.StationRepository
import com.crimeafuel.app.domain.usecase.UpdateFuelStatusUseCase
import com.crimeafuel.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditFuelUiState(
    val station: Station? = null,
    val fuelStatuses: Map<FuelType, Availability> = emptyMap(),
    val paymentMethods: Set<PaymentMethod> = emptySet(),
    val comment: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditFuelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stationRepository: StationRepository,
    private val updateFuelStatusUseCase: UpdateFuelStatusUseCase
) : ViewModel() {

    private val stationId: String = savedStateHandle.get<String>(Screen.STATION_ID_ARG) ?: ""

    private val _uiState = MutableStateFlow(EditFuelUiState())
    val uiState: StateFlow<EditFuelUiState> = _uiState.asStateFlow()

    init {
        loadStation()
    }

    private fun loadStation() {
        viewModelScope.launch {
            try {
                val station = stationRepository.getStation(stationId)
                if (station != null) {
                    val fuelMap = mutableMapOf<FuelType, Availability>()
                    station.fuelStatuses.forEach { status ->
                        fuelMap[status.fuelType] = status.availability
                    }
                    // Ensure all fuel types are represented
                    FuelType.entries.forEach { type ->
                        if (type !in fuelMap) {
                            fuelMap[type] = Availability.UNKNOWN
                        }
                    }

                    _uiState.update {
                        it.copy(
                            station = station,
                            fuelStatuses = fuelMap,
                            paymentMethods = station.paymentMethods.toSet(),
                            comment = station.comment ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "АЗС не найдена", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message, isLoading = false)
                }
            }
        }
    }

    fun setFuelAvailability(fuelType: FuelType, availability: Availability) {
        _uiState.update { state ->
            val updated = state.fuelStatuses.toMutableMap()
            updated[fuelType] = availability
            state.copy(fuelStatuses = updated)
        }
    }

    fun togglePaymentMethod(method: PaymentMethod) {
        _uiState.update { state ->
            val updated = state.paymentMethods.toMutableSet()
            if (method in updated) {
                updated.remove(method)
            } else {
                updated.add(method)
            }
            state.copy(paymentMethods = updated)
        }
    }

    fun updateComment(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    fun save() {
        val state = _uiState.value
        if (state.station == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val fuelStatuses = state.fuelStatuses.map { (type, availability) ->
                FuelStatus(type, availability)
            }

            val result = updateFuelStatusUseCase(
                stationId = stationId,
                fuelStatuses = fuelStatuses,
                paymentMethods = state.paymentMethods.toList(),
                comment = state.comment.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = e.localizedMessage ?: "Ошибка сохранения"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
