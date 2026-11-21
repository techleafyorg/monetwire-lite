package dev.monetwire

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import java.lang.ref.WeakReference

/**
 * Anchored adaptive banners - the full-width strip at the top or bottom of a screen.
 *
 * The container stays GONE until an ad has actually loaded, so a screen with no fill (or no
 * consent) looks like it never had a banner at all. AdMob refreshes a loaded banner by itself;
 * we only load it once per screen.
 *
 * Android 16 forces edge-to-edge, so a banner pinned to the bottom sits under the gesture bar
 * unless its container is padded by the navigation bar inset. The sample app shows how.
 */
internal object Banners {

    private const val TAG = "MonetWire"

    fun show(activity: Activity, container: ViewGroup, unitId: String) {
        if (unitId.isEmpty() || bannerIn(container) != null) return
        container.visibility = View.GONE

        val activityRef = WeakReference(activity)
        val containerRef = WeakReference(container)
        Sdk.whenReady {
            val a = activityRef.get() ?: return@whenReady
            val c = containerRef.get() ?: return@whenReady
            if (!a.isDestroyed && MonetWire.canShowAds) load(a, c, unitId)
        }
    }

    fun release(container: ViewGroup) {
        val adView = bannerIn(container) ?: return
        container.removeView(adView)
        adView.destroy()
        container.visibility = View.GONE
    }

    private fun load(activity: Activity, container: ViewGroup, unitId: String) {
        val density = activity.resources.displayMetrics.density
        // Before the first layout the container has no width yet - the screen's width is right for
        // the usual full-width strip.
        val widthPx = container.width.takeIf { it > 0 } ?: activity.resources.displayMetrics.widthPixels
        // The large anchored size - since SDK 1.7 Google's replacement for the plain one.
        val size = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, (widthPx / density).toInt())

        val adView = AdView(activity)
        container.addView(adView)
        adView.loadAd(BannerAdRequest.Builder(unitId, size).build(), object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                Sdk.main.post {
                    if (!activity.isDestroyed && adView.parent === container) container.visibility = View.VISIBLE
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w(TAG, "Banner failed: ${adError.code} ${adError.message}")
                Sdk.main.post {
                    if (adView.parent === container) container.removeView(adView)
                    adView.destroy()
                }
            }
        })
    }

    private fun bannerIn(container: ViewGroup): AdView? =
        (0 until container.childCount).map(container::getChildAt).firstOrNull { it is AdView } as AdView?
}
