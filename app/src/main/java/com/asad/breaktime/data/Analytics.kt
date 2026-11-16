package com.asad.breaktime.data

import android.content.Context
import com.asad.breaktime.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

/**
 * Anonymous usage analytics (PostHog), used to see where people get stuck, e.g. in setup.
 * Only step names and simple numbers are sent: never app names, screen contents or recordings.
 * Does nothing until a PostHog project key is set in app/build.gradle.kts.
 */
object Analytics {
    @Volatile private var ready = false

    fun init(context: Context) {
        if (ready || BuildConfig.POSTHOG_KEY.isBlank()) return
        synchronized(this) {
            if (ready) return
            val config = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_KEY,
                host = "https://us.i.posthog.com",
            ).apply {
                captureScreenViews = false // Screens are reported by name from the app itself.
                captureApplicationLifecycleEvents = true // App installed / opened / backgrounded.
                sessionReplay = false // Never record the screen.
            }
            runCatching { PostHogAndroid.setup(context.applicationContext, config) }
                .onSuccess { ready = true }
        }
    }

    /** Records one step, e.g. "break_started". [props] must be simple values, nothing personal. */
    fun track(context: Context, event: String, props: Map<String, Any> = emptyMap()) {
        init(context)
        if (ready) runCatching { PostHog.capture(event = event, properties = props) }
    }

    fun screen(context: Context, name: String) {
        init(context)
        if (ready) runCatching { PostHog.screen(screenTitle = name) }
    }
}
