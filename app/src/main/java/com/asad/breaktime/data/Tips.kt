package com.asad.breaktime.data

/** Gentle suggestions shown while apps are locked. */
object Tips {
    val all = listOf(
        "Go for a 10-minute walk. No phone needed.",
        "Step outside and find three things you can hear.",
        "Try two minutes of slow breathing: in for 4, hold for 4, out for 6.",
        "Drink a full glass of water.",
        "Stretch your neck, shoulders and back for a few minutes.",
        "Call or visit someone instead of scrolling.",
        "Look at something far away for 20 seconds to rest your eyes.",
        "Sit quietly for five minutes and just notice your thoughts.",
        "Tidy one small thing around you.",
        "Write down one thing you're grateful for today.",
        "Get some sunlight on your face, even for a minute.",
        "Read a few pages of a real book.",
    )

    fun random(): String = all.random()
}
