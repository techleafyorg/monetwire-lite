# Changelog

## 1.0.0 - 2026-11-21

First public version of MonetWire Lite.

- Google consent form (UMP 4.0.0), refreshed on every launch. The Mobile Ads SDK starts only once
  `canRequestAds()` allows it.
- Privacy options entry point for where Google requires one.
- Large anchored adaptive banners. The container stays hidden until an ad loads.
- Preloaded interstitials with AdMob's pacing rules built in: at least two user actions between ads,
  a minimum gap in seconds, and a per-session cap.
- `adsOff` switch for paying users.
- Sample app. Test device IDs come from `local.properties`.

Built on Google Mobile Ads Next-Gen SDK 1.7.0, minSdk 24, targetSdk 36.
Tested on a OnePlus phone running Android 14.
