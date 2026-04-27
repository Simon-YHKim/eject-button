package com.ejectbutton.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.LocalAppStrings
import com.microsoft.clarity.modifiers.clarityMask
import kotlinx.coroutines.delay

private val InCallGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1428), Color(0xFF2A1C3B),
        Color(0xFF3D2E54), Color(0xFF524B61), Color(0xFF6D5D54),
    )
)
private val GlassBtn = Color.White.copy(alpha = 0.15f)

@Composable
fun FakeInCallScreen(
    callerName: String,
    prompterHint: String,
    onEndCall: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsed++
        }
    }

    val timer = remember(elapsed) {
        "%02d:%02d".format(elapsed / 60, elapsed % 60)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InCallGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 통화 시간
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Call, null, tint = Color.White.copy(.8f), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(timer, fontSize = 18.sp, color = Color.White.copy(.8f), letterSpacing = 2.sp)
                Spacer(Modifier.width(20.dp))
                Icon(Icons.Default.Videocam, null, tint = Color.White.copy(.4f), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // v1.0.1 — Clarity 마스킹: 등록 contact 이름 PII 보호.
            Text(callerName, fontSize = 52.sp, fontWeight = FontWeight.Light, color = Color.White,
                modifier = Modifier.clarityMask())
            Text(strings.callerMobile, fontSize = 18.sp, fontWeight = FontWeight.Light, color = Color.White.copy(.75f))

            Spacer(modifier = Modifier.height(28.dp))

            // 녹음 타이머 Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(.3f))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(timer, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 컨트롤 그리드 3x2
            val controls: List<Triple<ImageVector, String, () -> Unit>> = listOf(
                Triple(Icons.AutoMirrored.Filled.PhoneCallback, strings.btnAddCall, {}),
                Triple(Icons.Filled.MicOff,                        strings.btnMute,    {}),
                Triple(Icons.Filled.Bluetooth,                     strings.btnBluetooth, {}),
                Triple(Icons.AutoMirrored.Filled.VolumeUp,         strings.btnSpeaker, {}),
                Triple(Icons.Filled.Dialpad,       strings.btnKeypad,  {}),
                Triple(Icons.Filled.MoreVert,      strings.btnMore,    {}),
            )

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                controls.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { (icon, label, action) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(GlassBtn)
                                        .clickable(onClick = action),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, label, tint = Color.White, modifier = Modifier.size(30.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(label, fontSize = 11.sp, color = Color.White.copy(.8f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 끊기 버튼
            Box(
                Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
                    .clickable(onClick = onEndCall),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CallEnd, strings.endCall, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (prompterHint.isNotBlank()) {
                Text(
                    prompterHint,
                    fontSize = 10.sp,
                    color = Color.White.copy(.18f),
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
