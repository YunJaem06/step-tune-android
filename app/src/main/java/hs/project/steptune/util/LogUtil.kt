package hs.project.steptune.util

import android.util.Log
import hs.project.steptune.BuildConfig

object LogUtil {
    private const val DEBUG_TAG = "DEBUG:::By-LogUtil:::"
    private const val WARN_TAG = "WARN:::By-LogUtil:::"
    private const val ERROR_TAG = "ERROR:::By-LogUtil:::"
    private const val MAX_LOG_LENGTH = 3_000

    fun d(message: String?) {
        log(Log.DEBUG, DEBUG_TAG, message)
    }

    fun w(message: String?) {
        log(Log.WARN, WARN_TAG, message)
    }

    fun e(message: String?, throwable: Throwable? = null) {
        val logMessage = buildString {
            append(message.orEmpty())
            if (throwable != null) {
                if (isNotEmpty()) append('\n')
                append(Log.getStackTraceString(throwable))
            }
        }
        log(Log.ERROR, ERROR_TAG, logMessage)
    }

    private fun log(priority: Int, tag: String, message: String?) {
        if (!BuildConfig.DEBUG) return

        val formattedMessage = "${callerPrefix()} ${message.orEmpty()}"
        formattedMessage.chunked(MAX_LOG_LENGTH).forEach { chunk ->
            Log.println(priority, tag, chunk)
        }
    }

    private fun callerPrefix(): String {
        val caller = Thread.currentThread().stackTrace.firstOrNull { element ->
            element.className != LogUtil::class.java.name &&
                element.className != Thread::class.java.name &&
                element.className != "dalvik.system.VMStack"
        } ?: return "[unknown]"

        return "[${caller.fileName}:${caller.lineNumber}::${caller.methodName}]"
    }
}
