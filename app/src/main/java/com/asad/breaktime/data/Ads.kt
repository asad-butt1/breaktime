package com.asad.breaktime.data

import android.app.Activity
import android.content.Context
import com.asad.breaktime.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The one ad free users see: a full-screen ad after tapping "Start break".
 * Never on app open and never on the break (lock) screen: Google Play doesn't allow ads there.
 * Premium users never see ads.
 *
 * Ad unit IDs come from app/build.gradle.kts (Google's test IDs until the real AdMob account is set up).
 */
object Ads {
    private val started = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    /** Minimum gap between two ads, so quickly restarting a break doesn't show another one. */
    private const val COOLDOWN_MS = 3 * 60_000L

    /**
     * Call when the main screen opens. Asks for ad consent where the law requires it (EU/UK),
     * then starts the ads SDK and loads the first ad.
     */
    fun init(activity: Activity) {
        if (BreakStore.get(activity).isPremium) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consent.canRequestAds()) startSdk(activity)
                }
            },
            { /* Couldn't check consent (offline); try again next launch. */ },
        )
        // Consent from an earlier launch: start right away.
        if (consent.canRequestAds()) startSdk(activity)
    }

    /** True when the user must be offered a way to change their ad choices (shown in Premium screen). */
    fun privacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
    }

    private fun startSdk(context: Context) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(context.applicationContext) { load(context.applicationContext) }
    }

    private fun load(context: Context) {
        if (interstitial != null || loading || BreakStore.get(context).isPremium) return
        loading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loading = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                }
            },
        )
    }

    /** Shows the ad (if one is ready) and calls [onDone] when it closes. Never blocks starting a break. */
    fun showBreakStartAd(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (BreakStore.get(activity).isPremium || ad == null || now - lastShownAt < COOLDOWN_MS) {
            onDone()
            return
        }
        interstitial = null
        lastShownAt = now
        val app = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onDone()
                load(app)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onDone()
                load(app)
            }
        }
        ad.show(activity)
    }
}
