package dev.monetwire

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

/**
 * One interstitial kept loaded ahead of time, shown only when [InterstitialRules] allow it.
 *
 * Loading at the moment of the break would make the user stare at nothing for a second or two,
 * and AdMob counts that against you too. So the next one starts loading as soon as the last one
 * is closed.
 */
internal object Interstitials {

    private const val TAG = "MonetWire"

    // Google won't show an interstitial that has waited longer than an hour.
    private const val EXPIRY_MS = 60 * 60 * 1000L
    private const val RETRY_MS = 60_000L

    private var unitId = ""
    private lateinit var rules: InterstitialRules

    private var ad: InterstitialAd? = null
    private var loadedAt = 0L
    private var loading = false

    private var actionsSinceLast = 0
    private var lastShownAt = 0L
    private var shownThisSession = 0
    var showing = false
        private set

    fun setUp(config: MonetConfig) {
        unitId = config.interstitialUnitId
        rules = config.interstitials
        // The first break counts like any other: a couple of actions before it.
        actionsSinceLast = 0
    }

    fun userAction() {
        actionsSinceLast++
    }

    fun preload() {
        if (unitId.isEmpty() || loading || hasFreshAd() || !MonetWire.canShowAds) return
        if (shownThisSession >= rules.maxPerSession) return
        loading = true
        InterstitialAd.load(AdRequest.Builder(unitId).build(), object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                Sdk.main.post {
                    loading = false
                    this@Interstitials.ad = ad
                    loadedAt = now()
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w(TAG, "Interstitial failed: ${adError.code} ${adError.message}")
                // No fill is normal now and then. Try again later rather than hammering AdMob.
                Sdk.main.post {
                    loading = false
                    Sdk.main.postDelayed(::preload, RETRY_MS)
                }
            }
        })
    }

    /**
     * Shows the ad if this break is allowed one, then calls [then] - always, whether an ad showed
     * or not, so the app's own flow carries on the same way either way.
     */
    fun showAtBreak(activity: Activity, then: () -> Unit) {
        val ready = if (hasFreshAd()) ad else null
        if (ready == null || !allowedNow()) {
            if (ready == null) preload()
            then()
            return
        }
        ad = null
        showing = true
        ready.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                Sdk.main.post {
                    showing = false
                    then()
                    preload()
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.w(TAG, "Interstitial didn't show: ${fullScreenContentError.message}")
                Sdk.main.post {
                    showing = false
                    then()
                    preload()
                }
            }
        }
        actionsSinceLast = 0
        lastShownAt = now()
        shownThisSession++
        ready.show(activity)
    }

    fun drop() {
        ad?.destroy()
        ad = null
    }

    private fun allowedNow(): Boolean =
        MonetWire.canShowAds &&
            actionsSinceLast >= rules.actionsBetween &&
            shownThisSession < rules.maxPerSession &&
            (lastShownAt == 0L || now() - lastShownAt >= rules.secondsBetween * 1000L)

    private fun hasFreshAd(): Boolean {
        if (ad != null && now() - loadedAt > EXPIRY_MS) drop()
        return ad != null
    }

    private fun now() = SystemClock.elapsedRealtime()
}
