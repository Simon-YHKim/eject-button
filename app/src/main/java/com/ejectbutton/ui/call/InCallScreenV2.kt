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
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
//     others are circular translucent tiles. Uses weight(1f) so tiles adapt
//     to screen width and long i18n labels wrap instead of truncating.
//   • 60dp red round end-call button
// ──────────────────────────────────────────────────────────────────────────

private val InCallGradient = Brush.verticalGradient(
    0.00f to Color(0xFF302A3A),
    0.35f to Color(0xFF3A2E46),
    0.70f to Color(0xFF5A3F4E),
    1.00f to Color(0xFF7A4A4C),
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
            .background(InCallGradient)
    ) {
        // ── Upper-right icons ──────────────────────────────────────────
        // Stacked in a Column anchored below the status bar; recording badge
        // sits above the video camera icon when recording is active.
        // Using statusBarsPadding() adapts to actual device insets (notches,
        // punch-holes, gesture vs 3-button nav) instead of hardcoded 44/118dp.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .shadow(8.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(RecGreen),
                    contentAlignment = Alignment.Center
                ) {
                    // tint = White fixes the green-on-green invisibility bug:
                    // the cassette drawable is drawn WHITE on green background,
                    // matching the design mockup (in-call-v3.html).
                    Icon(
                        painter = painterResource(id = R.drawable.rec_icon_green),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Video camera (always shown)
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(28.dp)
            )
        }

        // ── Main column ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()  // adapt to device status/nav bars
        ) {
            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(20.dp))

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
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = callerLabel,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            // AI assist floating pill — right-aligned above controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 32.dp, bottom = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AssistBg)
                        .clickable(onClick = onAssist),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = s.callAssist,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 3×2 controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
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
                        icon = Icons.Filled.VolumeUp,
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

            // End call (60dp round)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(18.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(EndRed)
                        .clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = s.endCall,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
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
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(if (isRecording) RoundedCornerShape(18.dp) else CircleShape)
                .background(if (isRecording) RecTileBg else TileBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Cassette / record icon — project's rec-icon-green vector drawable.
            // Icon() + tint = RecGreen preserves the original green visual intent
            // while allowing crisp rendering at any density (XML vector).
            Icon(
                painter = painterResource(id = R.drawable.rec_icon_green),
                contentDescription = null,
                tint = RecGreen,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = if (isRecording) formatDuration(elapsedSeconds).take(5)
                   else labelOff,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(TileBg)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = if (labelSmall) 0.78f else 0.92f),
            fontSize = if (labelSmall) 11.sp else 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%02d:%02d".format(m, r)
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
