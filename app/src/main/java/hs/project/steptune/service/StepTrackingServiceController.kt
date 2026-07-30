package hs.project.steptune.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object StepTrackingServiceController {

    fun start(context: Context): Boolean {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, StepTrackingService::class.java)
        return try {
            ContextCompat.startForegroundService(applicationContext, intent)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    fun stop(context: Context): Boolean {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, StepTrackingService::class.java)
        return applicationContext.stopService(intent)
    }
}
