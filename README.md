# BreakTime

An Android app that helps people take real breaks from social media and AI apps.
Pick the apps that pull you in, set a timer, and they stay locked until the break is over.
While they're locked, BreakTime suggests things to do offline, like a short walk or a breathing exercise.

## What's in this first version (free plan)

- **Choose apps to lock** (up to 3 on the free plan). Popular apps like Instagram, TikTok, YouTube and ChatGPT appear at the top.
- **Break timer**: 15 min, 30 min, 45 min, 1 hr, 2 hr, or a custom time up to 12 hours.
- **Lock screen**: opening a locked app during a break shows a calm screen with the time left and an offline suggestion.
- **Unlock + notification** when the timer ends. A countdown also shows in the notification bar.
- **Progress**: day streak, number of breaks, hours offline, and a history list.
- **End early** is allowed (with a confirmation), but doesn't count toward the streak.

Coming later: premium plan (unlimited apps, scheduled breaks, strict mode, detailed stats) and one ad after "Start break" for free users.

## Getting the app onto your phone

Every time code is pushed to GitHub, GitHub builds the app automatically.

1. On GitHub, open the repository and click the **Actions** tab.
2. Click the latest **Build app** run with a green check mark.
3. Scroll to **Artifacts** and download **BreakTime-apk**. It's a .zip; inside is `app-debug.apk`.
4. Send the .apk to your Android phone (email, Google Drive, or USB) and open it.
5. Android will ask to allow installing from this source. Allow it, then install.

## First-time setup on the phone

1. Open BreakTime and allow notifications.
2. Tap **Open settings** on the red card, find **BreakTime app lock** under Accessibility (sometimes under "Installed apps" or "Downloaded apps"), and switch it on.
   - On Android 13 and newer, if the switch is greyed out: go to Settings > Apps > BreakTime, tap the ⋮ menu at the top right, choose **Allow restricted settings**, then try again. This only happens for apps installed outside the Play Store.
3. Go to the **Apps** tab and tick the apps to lock.
4. Go back to **Break**, choose a time, and tap **Start break**.

## How it works (for developers)

- `service/BlockerService.kt`: an Accessibility service that only listens for "a new app came to the front" events and reads the app's package name. If that app is locked and a break is running, it opens `ui/BlockActivity.kt` on top.
- `data/BreakStore.kt`: saves locked apps, the current break and history on the phone (SharedPreferences). Nothing is sent anywhere.
- `service/BreakScheduler.kt`: starts and ends breaks, sets the end-of-break alarm, and shows notifications.
- `data/Ads.kt`: placeholder for the future AdMob ad after "Start break".

Built with Kotlin and Jetpack Compose. Minimum Android 8.0 (API 26), targets Android 15 (API 35).
To build locally: install Android Studio, open this folder, and press Run.
