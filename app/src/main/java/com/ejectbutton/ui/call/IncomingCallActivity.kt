package com.ejectbutton.ui.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

/**
 * v1.5.25 — Stay-resident trampoline that surfaces the SYSTEM_ALERT_WINDOW
 * overlay over a secure keyguard **without** triggering the PIN/pattern
 * authentication prompt.
 *
 * Why the v1.5.23 → v1.5.24 approach (call requestDismissKeyguard + finish())
 * wasn't sufficient on a secure-lock device:
 *   – Real Android 14 phone (Galaxy / LG U+ pattern lock) reported the
 *     pattern-entry screen popping up before the fake-call UI. Root cause:
 *     KeyguardManager.requestDismissKeyguard() on a secure keyguard *always*
 *     surfaces the user-credential prompt before letting any UI through.
 *     That's exactly the experience we're trying to *avoid* — real incoming
 *     calls bypass this because the system gives the default Phone app a
 *     privileged role, which a third-party app cannot claim.
 *
 *   – With requestDismissKeyguard removed and the activity finish()ed,
 *     SYSTEM_ALERT_WINDOW overlays cannot show on top of a secure keyguard
 *     either (Android 8+ tightened the policy). So finish() = the overlay
 *     gets sandwiched under the lock screen the moment we leave.
 *
 * v1.5.25 strategy:
 *   – setShowWhenLocked(true) + setTurnScreenOn(true) keep this activity
 *     visible over the lock screen (no credential prompt — those flags do
 *     NOT require keyguard dismissal).
 *   – **Don't** call requestDismissKeyguard. The keyguard stays in its
 *     non-authenticated state, but this activity is allowed to render on
 *     top of it.
 *   – **Don't** finish() in onCreate. The activity stays foreground for the
 *     entire fake call. Its window provides the "show over lock screen"
 *     privilege that the SYSTEM_ALERT_WINDOW overlay piggybacks on
 *     (overlays drawn while a setShowWhenLocked activity is foreground
 *     inherit the same z-order vs. keyguard).
 *   – When the fake call ends (FakeCallOverlayService.stopSelf), a local
 *     broadcast tells this activity to finish so we don't leave a stale
 *     transparent activity over the lock screen.
 *
 * The activity has NO Compose content of its own. The call UI is rendered
 * by FakeCallOverlayService's SYSTEM_ALERT_WINDOW overlay — the activity
 * is just the privilege carrier.
 */
class IncomingCallActivity : ComponentActivity() {

    companion object {
        /**
         * v1.5.25 — Broadcast sent by [com.ejectbutton.service.FakeCallOverlayService.dismiss]
         * (or any other "fake call has ended" code path) so that this activity
         * can finish() itself and stop holding the show-when-locked privilege.
         */
        const val ACTION_FAKE_CALL_ENDED = "com.ejectbutton.action.FAKE_CALL_ENDED"
    }

    private val endReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FAKE_CALL_ENDED) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 27+ — preferred way to render over the keyguard without
        // triggering credential prompts. These flags do not authenticate
        // the user; they just give the window permission to draw on top of
        // the lock screen surface.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Belt-and-suspenders for older devices and OEM skins that still
        // pay attention to window flags even on API 27+.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // v1.5.25 — DO NOT call KeyguardManager.requestDismissKeyguard.
        //   On a secure keyguard (PIN / pattern / biometric), that call
        //   forces a credential prompt, which interrupts the fake-call
        //   experience. We intentionally leave the keyguard locked and
        //   render on top of it instead.

        // Register receiver so we can finish() when the fake call ends.
        val filter = IntentFilter(ACTION_FAKE_CALL_ENDED)
        ContextCompat.registerReceiver(
            this,
            endReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // v1.5.25 — DO NOT finish() here. We need the activity to stay in
        //   the foreground so SYSTEM_ALERT_WINDOW overlays drawn by the
        //   service inherit our show-when-locked privilege. The fake call
        //   ends → service broadcasts ACTION_FAKE_CALL_ENDED → finish().
    }

    override fun onDestroy() {
        try { unregisterReceiver(endReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}
