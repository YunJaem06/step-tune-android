package hs.project.steptune.domain.usecase

import hs.project.steptune.domain.repository.PedometerRepository
import javax.inject.Inject

class GetTodayProgressUseCase @Inject constructor(
    private val repository: PedometerRepository
) {
    operator fun invoke(date: String) = repository.observeDailyProgress(date)
}

