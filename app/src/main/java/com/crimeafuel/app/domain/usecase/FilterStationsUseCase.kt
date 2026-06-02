package com.crimeafuel.app.domain.usecase

import com.crimeafuel.app.domain.model.FilterState
import com.crimeafuel.app.domain.model.Station
import javax.inject.Inject

class FilterStationsUseCase @Inject constructor() {
    operator fun invoke(stations: List<Station>, filterState: FilterState): List<Station> {
        if (filterState.isDefault) return stations
        return stations.filter { filterState.matches(it) }
    }
}
