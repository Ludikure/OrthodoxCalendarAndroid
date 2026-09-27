package com.orthodox.calendar.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory
import com.orthodox.calendar.BuildConfig
import com.orthodox.calendar.ui.util.toIsoDate
import java.time.LocalDate

/**
 * When to ask for a Play Store rating.
 *
 * People open this app to check one day, often daily, so launches alone say
 * little; distinct days of use say the app has earned a place. We ask once the
 * app has been opened on [REQUIRED_DAYS] separate days, at most once per app
 * version, and start counting again after each ask. Play decides whether the
 * review sheet actually appears (it enforces its own quota), so a request here
 * is a hint, never a guarantee.
 *
 * Mirror of `OrthodoxCalendar/App/ReviewPrompt.swift`.
 */
class ReviewPrompt(
    private val prefs: SharedPreferences,
    private val version: String = BuildConfig.VERSION_NAME,
) {
    constructor(context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    val activeDays: Int get() = prefs.getInt(KEY_ACTIVE_DAYS, 0)

    /** Counts [date] as a day of use; a second call on the same day counts nothing. */
    fun recordActive(date: LocalDate = LocalDate.now()) {
        val day = date.toIsoDate()
        if (prefs.getString(KEY_LAST_ACTIVE_DAY, null) == day) return
        prefs.edit()
            .putString(KEY_LAST_ACTIVE_DAY, day)
            .putInt(KEY_ACTIVE_DAYS, activeDays + 1)
            .apply()
    }

    val shouldPrompt: Boolean
        get() = activeDays >= REQUIRED_DAYS &&
            prefs.getString(KEY_PROMPTED_VERSION, null) != version

    /** Call right after requesting the review. */
    fun markPrompted() {
        prefs.edit()
            .putString(KEY_PROMPTED_VERSION, version)
            .putInt(KEY_ACTIVE_DAYS, 0)
            .apply()
    }

    /**
     * Asks Play for the in-app review sheet. Every failure — no Play Store, no
     * network, quota spent — is silent: nothing about a rating is worth an error
     * in front of someone reading the day's saints.
     */
    fun request(activity: Activity) {
        markPrompted()
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful && !activity.isFinishing) {
                    runCatching { manager.launchReviewFlow(activity, task.result) }
                }
            }
        }
    }

    companion object {
        const val REQUIRED_DAYS = 5
        private const val PREFS_NAME = "review_prompt"
        private const val KEY_LAST_ACTIVE_DAY = "lastActiveDay"
        private const val KEY_ACTIVE_DAYS = "activeDays"
        private const val KEY_PROMPTED_VERSION = "promptedVersion"
    }
}
