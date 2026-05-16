package com.ejectbutton.ui.call

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * v1.5.23 — Trampoline activity used as the **target of full-screen intents**
 * fired by [com.ejectbutton.service.FakeCallOverlayService].
 *
 * The activity is intentionally a no-op:
 *  - It exists only so that `NotificationCompat.Builder.setFullScreenIntent`
 *    has a valid Activity to launch when the device is locked / screen-off.
 *  - System launches this Activity → that single act dismisses the keyguard
 *    (because of [setShowWhenLocked]) and wakes the screen (because of
 *    [setTurnScreenOn] / `FLAG_TURN_SCREEN_ON`).
 *  - The actual fake-call UI (FakeIncomingCallScreenV2) is rendered by the
 *    foreground service via SYSTEM_ALERT_WINDOW, so this activity does not
 *    need any Compose UI of its own. It finishes right away — the overlay
 *    keeps showing because it lives in WindowManager, not the activity stack.
 *
 * Why the trampoline (and not just letting the service overlay show itself):
 *   Before Android 10 a foreground service could call startActivity() and
 *   immediately bring itself to the foreground over the lock screen. After
 *   Android 10's "background activity start" restrictions, the only reliable
 *   way to launch UI over the lock screen is a *full-screen intent on a
 *   high-priority notification, targeting a real Activity*. This file is
 *   that Activity.
 *
 * The Activity calls [KeyguardManager.requestDismissKeyguard] for devices
 * where flags alone leave the lock screen partially in front of the overlay.
 */
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 27+ — preferred: tell the system this activity is allowed to
        // show on top of the keyguard and to turn the screen on while it
        // launches. These supersede the older FLAG_SHOW_WHEN_LOCKED /
        // FLAG_TURN_SCREEN_ON window flags (which are deprecated on
        // Activity but still required for Service overlays).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // KeyguardManager.requestDismissKeyguard — covers the (rare) case
        // where setShowWhenLocked alone leaves a secure keyguard layer over
        // the SYSTEM_ALERT_WINDOW overlay. No-op on devices without a secure
        // lock. Safe to call from a non-foreground state because the activity
        // is the launch target of a full-screen intent — it's foreground by
        // definition the moment onCreate runs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val km = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        }

        // The foreground service overlay is already showing the actual UI;
        // this activity has nothing else to do. Finish so that the user can
        // dismiss / interact with the overlay without an empty activity in
        // the back stack.
        finish()
    }
}
