package hs.project.steptune.domain.model

data class StatsOverview(
    val summary: StatsSummary = StatsSummary(),
    val records: List<StatsRecord> = emptyList()
)

