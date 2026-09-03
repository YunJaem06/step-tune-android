package hs.project.steptune.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hs.project.steptune.R
import hs.project.steptune.domain.model.StatsOverview
import hs.project.steptune.domain.model.StatsPeriod
import hs.project.steptune.domain.model.StatsRecord
import hs.project.steptune.domain.model.StatsSummary
import hs.project.steptune.domain.model.WeeklyStepStatistics
import hs.project.steptune.ui.theme.StepTuneTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsRoute() {
    val viewModel: StatsViewModel = hiltViewModel()
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(Unit) {
        viewModel.refreshWeeklyStatistics()
    }
    ReportScreen(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onRetryWeeklyStatistics = viewModel::refreshWeeklyStatistics
    )
}

@Composable
fun ReportScreen(
    uiState: StatsUiState,
    onPeriodSelected: (StatsPeriod) -> Unit,
    onRetryWeeklyStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.stats_loading),
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.stats_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WeeklyComparisonCard(
            statistics = uiState.weeklyStatistics,
            isLoading = uiState.isWeeklyStatisticsLoading,
            loadFailed = uiState.weeklyStatisticsLoadFailed,
            onRetry = onRetryWeeklyStatistics
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.entries.forEach { period ->
                val selected = uiState.selectedPeriod == period
                if (selected) {
                    Button(
                        onClick = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(period.label())
                    }
                } else {
                    OutlinedButton(
                        onClick = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(period.label())
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = uiState.selectedPeriod.averageTitle(),
                value = "%,d %s".format(uiState.overview.summary.averageSteps, stringResource(R.string.stats_steps_unit))
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_best_record),
                value = "%,d %s".format(uiState.overview.summary.bestSteps, stringResource(R.string.stats_steps_unit))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_goal_achieved_days),
                value = "%,d %s".format(uiState.overview.summary.achievedDays, stringResource(R.string.stats_days_unit))
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.stats_total_steps),
                value = "%,d %s".format(uiState.overview.summary.totalSteps, stringResource(R.string.stats_steps_unit))
            )
        }

        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = uiState.selectedPeriod.recordsTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                uiState.overview.records.forEach { record ->
                    StatsRecordRow(record = record)
                }
            }
        }
    }
}

@Composable
private fun WeeklyComparisonCard(
    statistics: WeeklyStepStatistics?,
    isLoading: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_weekly_comparison_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when {
                isLoading -> Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.stats_weekly_comparison_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                loadFailed || statistics == null -> {
                    Text(
                        text = stringResource(R.string.stats_weekly_comparison_failed),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onRetry) {
                        Text(stringResource(R.string.common_retry))
                    }
                }

                else -> WeeklyComparisonContent(statistics)
            }
        }
    }
}

@Composable
private fun WeeklyComparisonContent(statistics: WeeklyStepStatistics) {
    Text(
        text = stringResource(
            R.string.stats_weekly_comparison_recorded_days,
            statistics.recordedDayCount
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WeeklyComparisonMetric(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.stats_weekly_comparison_today),
            value = "%,d %s".format(
                statistics.todayStepCount,
                stringResource(R.string.stats_steps_unit)
            )
        )
        WeeklyComparisonMetric(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.stats_weekly_comparison_average),
            value = "%,.0f %s".format(
                statistics.recent7DayAverage,
                stringResource(R.string.stats_steps_unit)
            )
        )
    }

    val comparisonText = when {
        statistics.changeRatePercent == null ->
            stringResource(R.string.stats_weekly_comparison_not_enough)

        statistics.differenceFromAverage > 0 ->
            stringResource(
                R.string.stats_weekly_comparison_above,
                abs(statistics.differenceFromAverage).roundToInt(),
                abs(statistics.changeRatePercent)
            )

        statistics.differenceFromAverage < 0 ->
            stringResource(
                R.string.stats_weekly_comparison_below,
                abs(statistics.differenceFromAverage).roundToInt(),
                abs(statistics.changeRatePercent)
            )

        else -> stringResource(R.string.stats_weekly_comparison_same)
    }
    Text(
        text = comparisonText,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun WeeklyComparisonMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatsRecordRow(record: StatsRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = record.label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "%,d %s".format(record.steps, stringResource(R.string.stats_steps_unit)),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatsPeriod.label(): String = when (this) {
    StatsPeriod.DAILY -> stringResource(R.string.stats_period_daily)
    StatsPeriod.WEEKLY -> stringResource(R.string.stats_period_weekly)
    StatsPeriod.MONTHLY -> stringResource(R.string.stats_period_monthly)
}

@Composable
private fun StatsPeriod.averageTitle(): String = when (this) {
    StatsPeriod.DAILY -> stringResource(R.string.stats_average_daily)
    StatsPeriod.WEEKLY -> stringResource(R.string.stats_average_weekly)
    StatsPeriod.MONTHLY -> stringResource(R.string.stats_average_monthly)
}

@Composable
private fun StatsPeriod.recordsTitle(): String = when (this) {
    StatsPeriod.DAILY -> stringResource(R.string.stats_records_daily)
    StatsPeriod.WEEKLY -> stringResource(R.string.stats_records_weekly)
    StatsPeriod.MONTHLY -> stringResource(R.string.stats_records_monthly)
}

@Preview(showBackground = true)
@Composable
private fun ReportScreenPreview() {
    StepTuneTheme {
        ReportScreen(
            uiState = StatsUiState(
                selectedPeriod = StatsPeriod.WEEKLY,
                overview = StatsOverview(
                    summary = StatsSummary(
                        averageSteps = 52730,
                        bestSteps = 68120,
                        achievedDays = 15,
                        totalSteps = 210920
                    ),
                    records = listOf(
                        StatsRecord("3/17 - 3/23", 48210),
                        StatsRecord("3/24 - 3/30", 52180),
                        StatsRecord("3/31 - 4/6", 42410),
                        StatsRecord("4/7 - 4/13", 68120)
                    )
                ),
                isLoading = false
            ),
            onPeriodSelected = {},
            onRetryWeeklyStatistics = {}
        )
    }
}
