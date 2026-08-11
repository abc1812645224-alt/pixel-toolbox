package com.example.pixeltoolbox.shizuku

import android.app.IActivityManager
import android.app.IInstrumentationWatcher
import android.app.UiAutomationConnection
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.ServiceManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider

class ShizukuProviderWrapper : ShizukuProvider() {
    override fun onCreate(): Boolean {
        HiddenApiBypass.setHiddenApiExemptions("")
        return super.onCreate()
    }

    companion object {
        private const val TAG = "ShizukuProviderWrapper"
        private const val INSTRUMENTATION_RESULT_TIMEOUT_MS = 15_000L
        private val instrumentationMutex = Mutex()

        suspend fun overrideImsConfig(context: Context, data: Bundle): String? {
            val primaryArgs = Bundle(data)
            val result = startInstrumentation(context, ImsModifier::class.java, primaryArgs, true)
            if (result == null) {
                Log.w(TAG, "overrideImsConfig: failed with empty result")
                return tryOverrideWithBroker(context, data, "failed with empty result")
            }
            if (result.getBoolean(ImsModifier.BUNDLE_RESULT)) {
                return null
            }
            val msg = result.getString(ImsModifier.BUNDLE_RESULT_MSG) ?: "unknown error"
            return tryOverrideWithBroker(context, data, msg)
        }

        suspend fun readCarrierConfig(
            context: Context,
            subId: Int,
        ): Bundle? {
            val args = Bundle().apply {
                putInt(ConfigReaderInstrumentation.KEY_SELECT_SIM_ID, subId)
            }
            val result = startInstrumentation(context, ConfigReaderInstrumentation::class.java, args, true)
            return result?.getBundle(ConfigReaderInstrumentation.KEY_RESULT)
        }

        suspend fun readSimInfoList(context: Context): List<Map<String, Any>> {
            val result = startInstrumentation(context, ConfigReaderInstrumentation::class.java, null, true)
            if (result == null) {
                Log.w(TAG, "readSimInfoList: failed with empty result")
                return emptyList()
            }
            // Return raw bundle for caller to parse
            return emptyList()
        }

        private suspend fun startInstrumentation(
            context: Context,
            cls: Class<*>,
            args: Bundle?,
            receiveResult: Boolean,
        ): Bundle? = instrumentationMutex.withLock {
            val deferredResult = CompletableDeferred<Bundle?>()
            var watcher: IInstrumentationWatcher.Stub? = null
            if (receiveResult) {
                watcher = object : IInstrumentationWatcher.Stub() {
                    override fun instrumentationStatus(
                        name: ComponentName?,
                        resultCode: Int,
                        results: Bundle?
                    ) {
                    }

                    override fun instrumentationFinished(
                        name: ComponentName?,
                        resultCode: Int,
                        results: Bundle?
                    ) {
                        deferredResult.complete(results)
                    }
                }
            }

            val binder = ServiceManager.getService(Context.ACTIVITY_SERVICE)
            val am = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(binder))
            val name = ComponentName(context, cls)
            val flags = 8 // ActivityManager.INSTR_FLAG_NO_RESTART
            val connection = UiAutomationConnection()
            var started = false
            try {
                Log.d(TAG, "startInstrumentation: call with component: $name")
                am.startInstrumentation(name, null, flags, args, watcher, connection, 0, null)
                started = true
                Log.i(TAG, "instrumentation started successfully")
                if (receiveResult) {
                    return withTimeoutOrNull(INSTRUMENTATION_RESULT_TIMEOUT_MS) {
                        deferredResult.await()
                    }
                }
                return null
            } catch (e: CancellationException) {
                if (started && receiveResult) {
                    withContext(NonCancellable) {
                        withTimeoutOrNull(INSTRUMENTATION_RESULT_TIMEOUT_MS) {
                            deferredResult.await()
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "failed to start instrumentation", e)
                return null
            }
        }

        private suspend fun tryOverrideWithBroker(
            context: Context,
            data: Bundle,
            msg: String,
        ): String? {
            if (!shouldRetryWithBroker(msg)) {
                return msg
            }
            val brokerArgs = Bundle(data)
            val brokerResult =
                startInstrumentation(context, BrokerInstrumentation::class.java, brokerArgs, true)
            if (brokerResult == null) {
                Log.w(TAG, "overrideImsConfig: broker failed with empty result")
                return msg
            }
            if (brokerResult.getBoolean(ImsModifier.BUNDLE_RESULT)) {
                return null
            }
            return brokerResult.getString(ImsModifier.BUNDLE_RESULT_MSG) ?: msg
        }

        private fun shouldRetryWithBroker(message: String): Boolean {
            val lower = message.lowercase()
            return lower.contains("persistent=true") ||
                lower.contains("system app") ||
                lower.contains("securityexception") ||
                lower.contains("security exception") ||
                lower.contains("empty result")
        }
    }
}
