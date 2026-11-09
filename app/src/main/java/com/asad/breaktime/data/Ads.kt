package com.asad.breaktime.data

import android.app.Activity

/**
 * Placeholder for the one ad free users see after tapping "Start break".
 * When AdMob is connected, this will show an interstitial ad and call [onDone] when it closes.
 * Never show ads on the break (lock) screen: Google Play does not allow it.
 */
object Ads {
    fun showBreakStartAd(activity: Activity, onDone: () -> Unit) {
        onDone()
    }
}
