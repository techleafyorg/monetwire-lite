package dev.monetwire

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's consent form (UMP).
 *
 * Google wants the consent status refreshed on every launch, and no ad request before
 * canRequestAds() says yes - in the EEA, UK and Switzerland a request without an answer
 * only gets "limited ads", which pay a lot less.
 */
internal object Consent {

    private const val TAG = "MonetWire"

    fun gather(activity: Activity, config: MonetConfig, done: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            params(activity, config),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    done(info.canRequestAds())
                }
            },
            { error ->
                // No network, usually. An answer saved from an earlier launch still counts.
                Log.w(TAG, "Consent update failed: ${error.message}")
                done(info.canRequestAds())
            },
        )
    }

    /** True once the user has answered, or when no answer is needed where they are. */
    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).canRequestAds()

    /**
     * Where the rules say users must be able to change their answer later. Show a "Privacy
     * options" row in your settings when this is true - it is a requirement, not a nice-to-have.
     */
    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, done: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Log.w(TAG, "Privacy options: ${error.message}")
            done()
        }
    }

    /** Testing only: forgets the answer so the form shows again on the next launch. */
    fun reset(context: Context) {
        UserMessagingPlatform.getConsentInformation(context).reset()
    }

    private fun params(context: Context, config: MonetConfig): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        val geography = when (config.debugGeography) {
            DebugGeography.OFF -> null
            DebugGeography.EEA -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            DebugGeography.US_STATE -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE
        }
        if (geography != null) {
            val debug = ConsentDebugSettings.Builder(context).setDebugGeography(geography)
            config.testDeviceIds.forEach(debug::addTestDeviceHashedId)
            builder.setConsentDebugSettings(debug.build())
        }
        return builder.build()
    }
}
