package hs.project.steptune.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import hs.project.steptune.core.util.PermissionUtils
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StepTrackingBootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val applicationContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = settingsRepository.observePreferences().first()
                if (
                    preferences.autoStartTrackingEnabled &&
                    PermissionUtils.hasTrackingPermissions(applicationContext)
                ) {
                    StepTrackingServiceController.start(applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
