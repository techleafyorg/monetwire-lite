package dev.monetwire

/**
 * What the kit needs to know about your app.
 *
 * Only [appId] is required. Leave a unit ID empty and that format just stays off - handy while
 * you wait for AdMob to approve a new unit.
 */
class MonetConfig(
    /** The AdMob app ID, the one with the "~" in it. */
    val appId: String,
    val bannerUnitId: String = "",
    val interstitialUnitId: String = "",
    /**
     * Phones that should get test ads. Both SDKs print the same ID in logcat the first time they run
     * on a phone - look for "setTestDeviceIds". Emulators count as test devices already.
     */
    val testDeviceIds: List<String> = emptyList(),
    /**
     * Makes the consent form show up while you test, wherever you are. On a real phone it is ignored
     * unless the phone is in [testDeviceIds]. Never ship this on.
     */
    val debugGeography: DebugGeography = DebugGeography.OFF,
    /** For apps made for kids. Google then serves only ads that are fine for children. */
    val childDirected: Boolean = false,
    val interstitials: InterstitialRules = InterstitialRules(),
)

enum class DebugGeography { OFF, EEA, US_STATE }

/**
 * How often a full-screen ad may appear.
 *
 * AdMob's own limit is one interstitial per two user actions, and an app that shows more risks
 * limited ad serving - so [actionsBetween] never goes below 2, whatever you pass.
 */
class InterstitialRules(
    actionsBetween: Int = 2,
    /** At least this long between two of them, whatever the user does. */
    val secondsBetween: Int = 60,
    /** After this many in one run of the app, no more until it is started again. */
    val maxPerSession: Int = 5,
) {
    val actionsBetween: Int = actionsBetween.coerceAtLeast(2)
}
