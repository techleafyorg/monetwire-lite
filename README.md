# MonetWire Lite

Consent-first AdMob for Android, in Kotlin. Google's consent form, banners and interstitials, wired up
the way Google's own docs say to and nothing more.

I kept rebuilding this in every app: the UMP consent dance, starting the SDK only after it, banners that
don't leave a blank strip on no-fill, interstitials that don't get the app flagged. So it's a small
module now.

**Built on:** Google Mobile Ads Next-Gen SDK 1.7.0 · UMP 4.0.0 · minSdk 24 · targetSdk 36

## What it does

- **Consent before anything loads.** The consent status is refreshed on every launch, the form shows only
  where it's needed (EEA, UK, Switzerland, the US states that have rules), and the SDK starts only after
  `canRequestAds()` says yes. A returning user's earlier answer is used straight away, so their ads
  aren't held up by the network.
- **Privacy options.** `privacyOptionsRequired` tells you when Google wants a "Privacy options" entry in
  your settings, and `showPrivacyOptions()` opens it.
- **Banners that hide on no-fill.** The large anchored adaptive size. The container stays `GONE` until
  an ad has actually loaded.
- **Interstitials that follow AdMob's rules.** One kept loaded ahead of time, shown only at breaks you
  choose, never more than one per two user actions, with a minimum gap and a per-session cap.
- **A switch for paying users.** `MonetWire.adsOff = true` stops everything.

## Setup

1. Copy the `monetwire` folder into your project and add it to `settings.gradle.kts`:

   ```kotlin
   include(":monetwire")
   ```

   then to your app's dependencies:

   ```kotlin
   implementation(project(":monetwire"))
   ```

2. Put your AdMob app ID in the manifest. The consent SDK reads it from there:

   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
   ```

3. Init in your `Application`:

   ```kotlin
   MonetWire.init(
       this,
       MonetConfig(
           appId = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy",
           bannerUnitId = "ca-app-pub-xxxxxxxxxxxxxxxx/1111111111",
           interstitialUnitId = "ca-app-pub-xxxxxxxxxxxxxxxx/2222222222",
       ),
   )
   ```

4. Ask for consent on every launch, from your first activity:

   ```kotlin
   MonetWire.gatherConsent(this) { adsAllowed ->
       MonetWire.showBanner(this, bannerContainer)
   }
   ```

5. Count real user actions, and show an interstitial at a natural break:

   ```kotlin
   MonetWire.userAction()            // opened an item, finished a level...

   MonetWire.showInterstitialAtBreak(this) {
       goToNextScreen()              // runs after the ad, or right away if none was due
   }
   ```

6. Release the banner in `onDestroy`:

   ```kotlin
   MonetWire.releaseBanner(bannerContainer)
   ```

The `sample` app does all of this on one screen. It uses Google's test IDs, so run it as is.

## Testing

- Emulators get test ads automatically. On a real phone, add the ID each SDK prints in logcat to
  `testDeviceIds`. **Never tap real ads on your own phone.** AdMob limits accounts for it.
- `debugGeography = DebugGeography.EEA` makes the consent form show wherever you are. On a real phone it
  only works once that phone is in `testDeviceIds`. Without it the phone just reports "not in Europe"
  and no form shows. The sample turns it on for debug builds only. Make sure it's off in release.
- The sample reads your phones' IDs from `local.properties`, so they never get committed:

  ```properties
  monetwire.testDevices=5E11D366F565C9053F7E175CCF26913C
  ```

- Declining consent doesn't mean no ads. In Europe Google still serves limited, non-personalized ads, so
  `canShowAds` is true once the user has answered either way.
- `MonetWire.resetConsent()` forgets the answer, so the form shows again on the next launch.

## Things the code can't do for you

A lot of "ads not showing" problems are in the AdMob console, not the app:

- **Publish a consent message** in AdMob → Privacy & messaging, for European regulations and, if you
  have US users, US state regulations. Without a published message the form never shows.
- **Add your mediation networks to that message** if you use any. Otherwise they don't serve in Europe.
- **Set up `app-ads.txt`** on the website listed in your Play Store page.
- **Fill in the Data safety form** in Play Console. Google lists what the Mobile Ads SDK collects
  [here](https://developers.google.com/admob/android/privacy/play-data-disclosure).

## Edge-to-edge

Apps targeting Android 16 are always edge-to-edge, so a banner at the bottom of the screen ends up under
the gesture bar. Pad its container by the navigation bar inset. `MainActivity` in the sample shows one
way.

## Lite and Pro

Lite is free and stays free. It covers consent, banners and interstitials.

**MonetWire Pro** adds:

- Rewarded, app open and native ads, with their own placement rules built in
- "Remove ads" and subscriptions with Play Billing 9 (acknowledgement, restore, pending purchases,
  grace period)
- Firebase Remote Config control: frequency, per-placement switches and a kill switch, with no app
  update
- Mediation setup
- A debug screen
- Updates for every Google SDK and policy change

## License

MIT. See [LICENSE](LICENSE).
