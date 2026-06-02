package com.crimeafuel.app.domain.usecase

import com.crimeafuel.app.domain.model.Station
import com.crimeafuel.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStationsUseCase @Inject constructor(
    private val stationRepository: StationRepository
) {
    operator fun invoke(): Flow<List<Station>> {
        return stationRepository.getAllStations()
    }
}
