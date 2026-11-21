package dev.monetwire.sample

import android.app.Application
import dev.monetwire.DebugGeography
import dev.monetwire.MonetConfig
import dev.monetwire.MonetWire

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MonetWire.init(
            this,
            MonetConfig(
                appId = getString(R.string.admob_app_id),
                bannerUnitId = getString(R.string.banner_unit_id),
                interstitialUnitId = getString(R.string.interstitial_unit_id),
                testDeviceIds = BuildConfig.TEST_DEVICES.split(',').map(String::trim).filter(String::isNotEmpty),
                // So you see the consent form here even outside Europe. Never in a release build.
                // On a real phone this only works once the phone is in testDeviceIds.
                debugGeography = if (BuildConfig.DEBUG) DebugGeography.EEA else DebugGeography.OFF,
            ),
        )
    }
}
