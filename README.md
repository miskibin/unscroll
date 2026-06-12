# Unscroll

Block the *endless scrolling*, not the apps.

Unscroll limits short-form video feeds — **Instagram Reels, YouTube Shorts and
TikTok's For You feed** — while leaving the rest of each app alone. DMs,
search, profiles and normal videos always work.

## How it works

You get a scroll allowance (default **2 minutes every 10 minutes**, both
configurable). An accessibility service recognises when a short-form feed is
on screen by looking at the view ids of the supported apps:

| App | Detection |
| --- | --- |
| Instagram | `clips_viewer_view_pager` (the fullscreen Reels viewer) |
| YouTube | `reel_recycler` / `reel_progress_bar` (the Shorts player) |
| TikTok | `video_container` + full-screen ViewPager heuristic (best effort — TikTok obfuscates its ids) |

While a feed is visible, watch time is charged against the allowance. When it
runs out, Unscroll presses **Back** (rate limited, never more than once per
2 s) and shows a toast with the time until the allowance refills. No
activities are launched, you are never thrown to the home screen, and nothing
happens unless a feed is *positively* detected while the master switch is on.

The service only receives events from the supported packages
(`android:packageNames` in the service config) and never reads, stores or
transmits screen content.

## Build

**Prerequisites:** [Android Studio](https://developer.android.com/studio) or
an Android SDK + Gradle 8.14+.

1. Open the project in Android Studio (or run `gradle :app:assembleDebug`).
2. The debug build signs with `debug.keystore` in the repo root.
3. Install on a device, open Unscroll and follow the one-time setup to enable
   the accessibility service.

Run unit tests with `gradle :app:testDebugUnitTest`.
