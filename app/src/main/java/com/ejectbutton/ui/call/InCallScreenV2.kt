package com.ejectbutton.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.R
import com.ejectbutton.data.LocalAppStrings
import kotlinx.coroutines.delay

// ──────────────────────────────────────────────────────────────────────────
//  InCallScreenV2
//  One UI 8.5 active-call screen. Drop-in replacement for the existing
//  in-call composable. Pulls all copy from LocalAppStrings so every language
//  the app already supports (en / ko / zh-CN / zh-TW / ja / es / hi) is
//  covered automatically.
//
//  LAYOUT (top → bottom):
//   • (status bar / nav bar areas reserved but empty — system UI shows through)
//   • Top-center: 📞 + live timer + subtext ("Transcribing…" / "Call Assist active")
//   • Upper-right: 🎥 video icon; 🎤 green mic badge when recording
//   • Caller name + caller label (phone number / relation)
//   • AI assist floating pill (right-aligned, just above controls)
//   • 3×2 controls grid — recording tile is dark-squircle + green cassette icon;
//     all others are circular translucent tiles
//   • End-call 60dp round button
// ──────────────────────────────────────────────────────────────────────────

// Round 21 — One UI 8.5 in-call wallpaper. Same "base linear + 4 radial blobs"
// technique the incoming-call screen uses (see FakeIncomingCallScreenV2), but
// the blobs are shifted lower and warmed toward peach/coral so the upper ~1/3
// of the screen stays near-black — matching the Samsung reference screenshot
// ("텍스트로 변환 중…" state).  Colors sampled from the reference image.
private val InCallGradientBase = Brush.verticalGradient(
    colorStops = arrayOf(
        0.00f to Color(0xFF020204),   // near-black top
        0.30f to Color(0xFF08081A),   // deep navy (still dark void)
        0.55f to Color(0xFF221A34),   // indigo
        0.75f to Color(0xFF3F2B46),   // purple
        0.90f to Color(0xFF6B4452),   // warm plum
        1.00f to Color(0xFFA27063),   // dusty coral floor
    )
)

private val TileBg      = Color(0xFF3C374B).copy(alpha = 0.55f)
private val RecTileBg   = Color(0xFF1C1C22)
private val RecGreen    = Color(0xFF22C55E)
private val EndRed      = Color(0xFFEF4444)
private val AssistBg    = Color(0xFF32283C).copy(alpha = 0.70f)

@Composable
fun InCallScreenV2(
    callerName: String,
    callerLabel: String,                    // "폰 010-2484-1120" / "Mobile" / relation etc.
    elapsedSeconds: Int,                    // call duration in seconds
    isRecording: Boolean = true,            // shows green mic badge + recording tile style
    statusSubtext: String? = null,          // e.g. "Transcribing…"; null hides it
    bluetoothDeviceName: String? = null,    // e.g. "Galaxy Watch3\n(36A1)"; null hides BT label
    onMute: () -> Unit = {},
    onRecordingToggle: () -> Unit = {},
    onSpeaker: () -> Unit = {},
    onKeypad: () -> Unit = {},
    onBluetooth: () -> Unit = {},
    onMore: () -> Unit = {},
    onAssist: () -> Unit = {},
    onEndCall: () -> Unit = {},
) {
    val s = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InCallGradientBase)
            .drawBehind {
                // Four radial "blobs" that produce the One UI 8.5 pastel feel.
                // Positions bias toward the bottom half so the top ~30% stays
                // near-black (matching the reference screenshot).
                val w = size.width
                val h = size.height
                drawBlob(Color(0xFF5A6EB4).copy(alpha = 0.35f), Offset(w * -0.10f, h * 0.42f), w * 0.70f) // cool blue, left-mid
                drawBlob(Color(0xFFD28866).copy(alpha = 0.55f), Offset(w *  1.10f, h * 0.78f), w * 0.85f) // warm coral, right-bottom
                drawBlob(Color(0xFFE9B886).copy(alpha = 0.45f), Offset(w *  0.95f, h * 1.05f), w * 0.70f) // peach, bottom-right corner
                drawBlob(Color(0xFF6E5A8A).copy(alpha = 0.45f), Offset(w * -0.10f, h * 1.05f), w * 0.80f) // lilac, bottom-left
            }
    ) {
        // ── Upper-right icons ──────────────────────────────────────────
        // Video camera (always shown) — sits roughly on the timer line.
        Icon(
            imageVector = Icons.Filled.Videocam,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 96.dp, end = 28.dp)
                .size(30.dp)
        )

        // Recording mic badge — green circle w/ a classic microphone glyph.
        // Round 26 — shrunk 44dp → 26dp (~40% reduction) to match the relative
        // size in the Samsung reference, which reads as ~8.5% of screen width
        // (26dp on a 448dp device). Icon and shadow scaled proportionally.
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .size(26.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(RecGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // ── Main column ────────────────────────────────────────────────
        // Round 26 — caller block was landing at ~28% of screen (10%p below
        // the reference's ~18% position). Trimmed top padding 72→48, first
        // spacer 24→16, and the subtext→name gap 64→16 to pull the whole
        // caller info block up by ~80dp. Status bar is hidden by the overlay
        // service so 48dp top padding still leaves a visible "dramatic black
        // void" at the top without pushing the timer down.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 36.dp)  // reserve system UI space
        ) {
            Spacer(Modifier.height(16.dp))

            // Top center: 📞 + timer + subtext
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatDuration(elapsedSeconds),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (!statusSubtext.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusSubtext,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                    )
                }
            }

            // Round 26 — gap 64→16dp. Round 22 over-corrected and pushed the
            // name all the way down to ~28% of screen; 16dp gives a tighter
            // ONE UI 8.5-style stacking while still letting the name breathe
            // below the subtext.
            Spacer(Modifier.height(16.dp))

            // Caller name + label
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 40.sp,                  // ↑ from 36sp (Round 22) — matches ~42sp in reference
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = callerLabel,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,                  // ↑ from 14sp — slightly larger + wider tracking feel
                )
            }

            // Round 24 — bumped weight ratio 4:1 → 14:1. The 4:1 split still
            // left Row 1 tiles at ~55% of screen (target is 60%). Giving ~93%
            // of the empty space to the upper gap lands Row 1 at ~60% and
            // Row 2 at ~74%, matching the reference. End-call breathing room
            // is instead created by a larger end-call bottom padding (see
            // below) rather than by a lower-gap spacer.
            Spacer(Modifier.weight(14f))

            // AI assist floating pill — Round 24: right-aligned but ABOVE the
            // Bluetooth (right-most) control tile. The Row below has
            // horizontal padding 46dp and columns are 96dp wide, so the
            // right-column center sits at (46 + 48) = 94dp from the right
            // edge. A 48dp sparkle circle therefore needs end-padding =
            // 94 - 24 = 70dp to line up vertically with the BT tile.
            // Previous 32dp was misaligning the sparkle ~38dp to the right
            // of the BT column.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 70.dp, bottom = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)                   // ↑ from 40dp (Round 22, match reference ~5.5% width)
                        .clip(CircleShape)
                        .background(AssistBg)
                        .clickable(onClick = onAssist),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = s.callAssist,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp) // ↑ from 18dp
                    )
                }
            }

            // 3×2 controls — Round 22 tuning:
            //   • Horizontal padding 28 → 46dp so the three columns pack
            //     tighter around the screen centerline (tile center-to-center
            //     ~130dp, matching the reference's 21% / 50% / 79% layout).
            //   • Row gap held at 40dp — that already produces the ~14%
            //     height-of-screen center-to-center between rows the reference
            //     uses. Verified by measuring 1344×2992 screenshots.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 46.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RecordingTile(
                        isRecording = isRecording,
                        elapsedSeconds = elapsedSeconds,
                        labelOff = s.record,
                        onClick = onRecordingToggle,
                    )
                    ControlTile(
                        icon = Icons.Filled.MicOff,
                        label = s.mute,
                        onClick = onMute,
                    )
                    ControlTile(
                        icon = Icons.Filled.Bluetooth,
                        label = bluetoothDeviceName ?: s.bluetooth,
                        onClick = onBluetooth,
                        labelSmall = bluetoothDeviceName != null,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ControlTile(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = s.speaker,
                        onClick = onSpeaker,
                    )
                    ControlTile(
                        icon = Icons.Filled.Dialpad,
                        label = s.keypad,
                        onClick = onKeypad,
                    )
                    ControlTile(
                        icon = Icons.Filled.MoreVert,
                        label = s.more,
                        onClick = onMore,
                    )
                }
            }

            // Round 24 — tiny remainder spacer (~7% of the weighted void); most
            // vertical balance now comes from the 14f above + the end-call's
            // own 40dp bottom padding, which lifts the red circle ~32dp off
            // the very bottom of the screen so it doesn't look "dangling"
            // (user feedback: "종료버튼 위로 올려야해").
            Spacer(Modifier.weight(1f))

            // End call — Round 26: bottom padding 76 → 36dp. Round 25's 76dp
            // over-lifted the controls block (Row 1 landed at 55%, Row 2 at
            // 67%, end-call at 81% — all ~6-9%p above the reference). 36dp
            // brings controls back down to Row 1 ~62%, Row 2 ~73%, end-call
            // ~88% which matches the reference's 60/74/90 within 2%p.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(20.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(EndRed)
                        .clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = s.endCall,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
//  Sub-composables
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun RowScope.RecordingTile(
    isRecording: Boolean,
    elapsedSeconds: Int,
    labelOff: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)                                           // ↑ from 60dp
                // Round 22: all tiles now use the same squircle shape — the
                // Samsung reference shows clear corner flatness on every tile,
                // not perfect circles. 24dp radius on a 72dp tile ≈ 1/3 side
                // which is the One UI 8.5 squircle ratio.
                .clip(RoundedCornerShape(24.dp))
                .background(if (isRecording) RecTileBg else TileBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Cassette glyph — the rec_icon_green vector (merged from
            // feat/vector-drawable) is authored with white strokes/fills so
            // the Icon tint recolors it. Use Icon+tint=RecGreen to keep the
            // Recording tile's green cassette look identical to the prior PNG.
            Icon(
                painter = painterResource(id = R.drawable.rec_icon_green),
                contentDescription = null,
                tint = RecGreen,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = if (isRecording) formatDuration(elapsedSeconds - 1).take(5)
                   else labelOff,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RowScope.ControlTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    labelSmall: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)                                            // ↑ from 60dp
                // Round 22: squircle, not circle (see RecordingTile comment).
                .clip(RoundedCornerShape(24.dp))
                .background(TileBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(28.dp)                         // ↑ from 24dp
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = if (labelSmall) 0.78f else 0.92f),
            fontSize = if (labelSmall) 12.sp else 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%02d:%02d".format(m, r)
}

// Soft radial "blob" used to produce the One UI 8.5 pastel wallpaper feel.
// Mirrors the identically-named helper in FakeIncomingCallScreenV2 — kept
// file-local so the two screens can tune their blob palettes independently.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(
    color: Color, center: Offset, radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

// ──────────────────────────────────────────────────────────────────────────
//  Optional helper — drives elapsedSeconds from a Composable coroutine.
//  Use if the caller doesn't already maintain a call duration state.
// ──────────────────────────────────────────────────────────────────────────
@Composable
fun rememberCallTimer(startSeconds: Int = 0): State<Int> {
    val seconds = remember { mutableStateOf(startSeconds) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds.value = seconds.value + 1
        }
    }
    return seconds
}
