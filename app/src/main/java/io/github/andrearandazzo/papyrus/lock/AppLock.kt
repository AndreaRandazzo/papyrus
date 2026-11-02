package io.github.andrearandazzo.papyrus.lock

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import io.github.andrearandazzo.papyrus.data.LockTimeout
import io.github.andrearandazzo.papyrus.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** When to lock again after Papyrus has been in the background for [awayMillis]. */
object LockPolicy {
    /**
     * Opening a document, the camera or a file picker takes the user to another app on purpose;
     * coming back from it within this time doesn't ask to unlock again.
     */
    const val OTHER_APP_GRACE_MILLIS = 5 * 60_000L

    fun shouldLock(timeout: LockTimeout, awayMillis: Long, wentToOtherApp: Boolean): Boolean {
        val allowed = if (wentToOtherApp) maxOf(timeout.millis, OTHER_APP_GRACE_MILLIS) else timeout.millis
        return awayMillis >= allowed
    }
}

/**
 * Settings > App lock: whether Papyrus is locked right now. It locks at start and when it comes
 * back after enough time in the background; [unlock] is called after the phone confirms it's the
 * owner (fingerprint, face or PIN).
 *
 * Registered for every activity's lifecycle: Papyrus is in the background when none is started.
 * Rotation or a text size change restarts a screen without counting as leaving.
 */
class AppLock(
    private val preferences: UserPreferences,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : Application.ActivityLifecycleCallbacks {

    private val _locked = MutableStateFlow(preferences.appLock.value)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var startedActivities = 0
    private var leftAt: Long? = null
    private var wentToOtherApp = false

    fun unlock() {
        _locked.value = false
    }

    /** Papyrus is about to open another app (viewer, camera, picker, browser) for the user. */
    fun onOpeningOtherApp() {
        wentToOtherApp = true
    }

    override fun onActivityStarted(activity: Activity) {
        if (startedActivities == 0) {
            val away = leftAt?.let { clock() - it }
            if (away != null && preferences.appLock.value &&
                LockPolicy.shouldLock(preferences.lockTimeout.value, away, wentToOtherApp)
            ) {
                _locked.value = true
            }
            leftAt = null
            wentToOtherApp = false
        }
        startedActivities++
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities == 0 && !activity.isChangingConfigurations) leftAt = clock()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
