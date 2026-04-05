package com.ejectbutton.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.LocalAppStrings
import kotlin.math.roundToInt
import kotlin.math.sqrt

private val IncomingGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0D0A14), Color(0xFF1A1025), Color(0xFF2C2040), Color(0xFF3D2E54))
)

@Composable
fun FakeIncomingCallScreen(
    callerName: String,
    callerLabel: String,
    prompterHint: String,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IncomingGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(100.dp))

            // 발신자 아바타
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = callerName.take(1),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(callerName, fontSize = 38.sp, fontWeight = FontWeight.Light, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(callerLabel, fontSize = 15.sp, color = Color.White.copy(alpha = 0.55f))
            Spacer(Modifier.height(10.dp))
            Text(strings.incomingCallLabel, fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f), letterSpacing = 3.sp)

            Spacer(Modifier.weight(1f))

            // 드래그 힌트
            Text(
                text = strings.dragHint,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(24.dp))

            // 거절 / 수락
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DraggableCallButton(
                    icon  = Icons.Default.CallEnd,
                    color = Color(0xFFEF4444),
                    label = strings.decline,
                    onTrigger = onDecline,
                )
                DraggableCallButton(
                    icon  = Icons.Default.Call,
                    color = Color(0xFF22C55E),
                    label = strings.accept,
                    onTrigger = onAccept,
                )
            }

            Spacer(Modifier.height(52.dp))

            if (prompterHint.isNotBlank()) {
                Text(
                    text = prompterHint,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            } else {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DraggableCallButton(
    icon: ImageVector,
    color: Color,
    label: String,
    onTrigger: () -> Unit,
) {
    val THRESHOLD = 75f
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }

    val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
    val fraction = (distance / THRESHOLD).coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.75f + fraction * 0.25f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            if (!triggered) {
                                change.consume()
                                offsetX += drag.x
                                offsetY += drag.y
                            }
                        },
                        onDragEnd = {
                            if (!triggered) {
                                val dist = sqrt(offsetX * offsetX + offsetY * offsetY)
                                if (dist >= THRESHOLD) {
                                    triggered = true
                                    onTrigger()
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            if (!triggered) { offsetX = 0f; offsetY = 0f }
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
    }
}
