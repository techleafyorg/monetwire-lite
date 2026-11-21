package dev.monetwire

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener

/**
 * Starts the Mobile Ads SDK once, and only after consent allows it.
 *
 * Google asks for initialize() on a background thread. Its callback waits for every mediation
 * adapter, up to 30 seconds when one is slow, so we don't wait for it: after a few seconds we
 * call the SDK ready anyway, and a network still starting just misses the first requests.
 */
internal object Sdk {

    private const val TAG = "MonetWire"
    private const val READY_ANYWAY_MS = 3_000L

    val main = Handler(Looper.getMainLooper())

    var ready = false
        private set

    private var starting = false
    private val waiting = ArrayList<() -> Unit>()

    fun start(context: Context, config: MonetConfig) {
        if (starting) return
        starting = true
        val app = context.applicationContext

        val requests = RequestConfiguration.Builder()
            .setTestDeviceIds(config.testDeviceIds)
            .apply { if (config.childDirected) setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD) }
            .build()
        val init = InitializationConfig.Builder(config.appId)
            .setRequestConfiguration(requests)
            .build()

        Thread {
            MobileAds.initialize(app, init, object : OnAdapterInitializationCompleteListener {
                override fun onAdapterInitializationComplete(status: InitializationStatus) {
                    // Worth a look when adding mediation: a network missing here never fills.
                    Log.i(TAG, "Adapters: " + status.adapterStatusMap.entries.joinToString { (name, s) ->
                        "${name.substringAfterLast('.')}=${s.initializationState}"
                    })
                    main.post(::markReady)
                }
            })
        }.start()
        main.postDelayed(::markReady, READY_ANYWAY_MS)
    }

    /** Runs [block] on the main thread once the SDK can take requests - right away if it already can. */
    fun whenReady(block: () -> Unit) {
        if (ready) block() else waiting += block
    }

    private fun markReady() {
        if (ready) return
        ready = true
        val queued = waiting.toList()
        waiting.clear()
        queued.forEach { it() }
    }
}
