package hs.project.steptune.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StepTrackingStateDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val state: Flow<StepTrackingState> = dataStore.data.map { prefs ->
        StepTrackingState(
            trackingDate = prefs[TRACKING_DATE],
            baselineSensorSteps = prefs[BASELINE_SENSOR_STEPS],
            offsetSteps = prefs[OFFSET_STEPS] ?: 0
        )
    }

    suspend fun updateState(
        trackingDate: String,
        baselineSensorSteps: Int,
        offsetSteps: Int
    ) {
        dataStore.edit { prefs ->
            prefs[TRACKING_DATE] = trackingDate
            prefs[BASELINE_SENSOR_STEPS] = baselineSensorSteps
            prefs[OFFSET_STEPS] = offsetSteps
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(TRACKING_DATE)
            prefs.remove(BASELINE_SENSOR_STEPS)
            prefs.remove(OFFSET_STEPS)
        }
    }

    companion object {
        private val TRACKING_DATE = stringPreferencesKey("tracking_date")
        private val BASELINE_SENSOR_STEPS = intPreferencesKey("baseline_sensor_steps")
        private val OFFSET_STEPS = intPreferencesKey("offset_steps")
    }
}
