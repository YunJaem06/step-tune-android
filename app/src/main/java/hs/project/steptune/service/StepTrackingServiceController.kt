package hs.project.steptune.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object StepTrackingServiceController {

    fun start(context: Context) {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, StepTrackingService::class.java)
        ContextCompat.startForegroundService(applicationContext, intent)
    }

    fun stop(context: Context) {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, StepTrackingService::class.java)
        applicationContext.stopService(intent)
    }
}
