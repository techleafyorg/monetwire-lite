package dev.monetwire

import android.app.Activity
import android.app.Application
import android.view.ViewGroup

/**
 * The whole kit, from the app's side.
 *
 * The order matters, and it's the one Google expects:
 * 1. [init] in Application.onCreate - only remembers the config, nothing loads yet.
 * 2. [gatherConsent] from your first activity, on every launch. The SDK starts once the user
 *    has answered (or where no answer is needed), never before.
 * 3. [showBanner] on screens that have one, [showInterstitialAtBreak] at natural breaks.
 *
 * Every call is meant for the main thread, and every callback comes back on it.
 */
object MonetWire {

    private lateinit var app: Application
    private lateinit var config: MonetConfig

    /**
     * Set this to true for a user who shouldn't see ads - a paid "remove ads", say. Banners and
     * interstitials stop loading at once. Screens already showing a banner should release it.
     */
    var adsOff = false
        set(value) {
            field = value
            if (value) Interstitials.drop()
        }

    fun init(app: Application, config: MonetConfig) {
        this.app = app
        this.config = config
        Interstitials.setUp(config)
    }

    /**
     * Refreshes the consent status and shows Google's form if this user needs to see it.
     * [done] gets whether ads may load now.
     *
     * An answer from an earlier launch still holds while the refresh runs, so ads start straight
     * away for returning users instead of waiting on the network.
     */
    fun gatherConsent(activity: Activity, done: (adsAllowed: Boolean) -> Unit = {}) {
        checkInit()
        if (canShowAds) startAds()
        Consent.gather(activity, config) { allowed ->
            if (allowed && !adsOff) startAds()
            done(allowed)
        }
    }

    /** Consent allows ads and the user hasn't turned them off. */
    val canShowAds: Boolean
        get() = ::app.isInitialized && !adsOff && Consent.canRequestAds(app)

    /** Show a "Privacy options" entry in settings when this is true. Google requires it there. */
    val privacyOptionsRequired: Boolean
        get() = ::app.isInitialized && Consent.privacyOptionsRequired(app)

    fun showPrivacyOptions(activity: Activity, done: () -> Unit = {}) {
        Consent.showPrivacyOptions(activity, done)
    }

    /** For testing: forget the consent answer, so the form shows again on the next launch. */
    fun resetConsent() {
        checkInit()
        Consent.reset(app)
    }

    /**
     * Puts a banner in [container] - an empty FrameLayout where the strip should go. The container
     * stays hidden unless an ad actually loads. Call [releaseBanner] from the screen's onDestroy.
     */
    fun showBanner(activity: Activity, container: ViewGroup) {
        checkInit()
        if (adsOff) return
        Banners.show(activity, container, config.bannerUnitId)
    }

    fun releaseBanner(container: ViewGroup) {
        Banners.release(container)
    }

    /**
     * Count a real user action here - opening an item, finishing a level, saving something.
     * The interstitial rules are measured in these.
     */
    fun userAction() {
        Interstitials.userAction()
    }

    /**
     * Call this at a natural break: after a save, between levels, when a list is done. Never on
     * app start or exit, and never in the middle of something - AdMob flags those.
     *
     * [then] runs right after the ad is closed, or right away when no ad is due. Put whatever
     * the app does next in there, so the flow is the same with or without an ad.
     */
    fun showInterstitialAtBreak(activity: Activity, then: () -> Unit = {}) {
        checkInit()
        if (adsOff || Interstitials.showing) {
            then()
            return
        }
        Interstitials.showAtBreak(activity, then)
    }

    private fun startAds() {
        Sdk.start(app, config)
        Sdk.whenReady(Interstitials::preload)
    }

    private fun checkInit() {
        check(::app.isInitialized) { "Call MonetWire.init() in Application.onCreate first" }
    }
}
