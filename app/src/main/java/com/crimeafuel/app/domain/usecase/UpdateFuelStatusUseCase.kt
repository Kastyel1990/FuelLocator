package com.crimeafuel.app.domain.usecase

import com.crimeafuel.app.domain.model.FuelStatus
import com.crimeafuel.app.domain.model.PaymentMethod
import com.crimeafuel.app.domain.repository.AuthRepository
import com.crimeafuel.app.domain.repository.StationRepository
import javax.inject.Inject

class UpdateFuelStatusUseCase @Inject constructor(
    private val stationRepository: StationRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        stationId: String,
        fuelStatuses: List<FuelStatus>,
        paymentMethods: List<PaymentMethod>,
        comment: String?
    ): Result<Unit> {
        val userId = authRepository.currentUserId
            ?: return Result.failure(IllegalStateException("User not logged in"))

        return try {
            stationRepository.updateFuelStatus(
                stationId = stationId,
                fuelStatuses = fuelStatuses,
                paymentMethods = paymentMethods,
                comment = comment,
                userId = userId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
