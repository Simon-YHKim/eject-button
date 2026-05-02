package com.ejectbutton.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.ui.theme.EjectCoral
import com.ejectbutton.ui.theme.EjectOnSurface
import com.ejectbutton.ui.theme.EjectSurface

/**
 * Î©îÏù∏ ?îÎ©¥ ?ÑÏóê ?ÑÏö∞??4-step ÏΩîÏπòÎßàÌÅ¨ ?¨Ïñ¥ (v1.5.0).
 *
 * ?†Í∑ú ?¨Ïö©?êÍ? OnboardingScreen ???ùÎÇ∏ ÏßÅÌõÑ, Î©îÏù∏ ?îÎ©¥ ÏßÑÏûÖ ??Ï≤?1?åÎßå
 * ?êÎèô ?úÏãú. 4Í∞??µÏã¨ Î≤ÑÌäº(?úÎÇòÎ¶¨Ïò§ Ïπ¥Îìú / ?∏Î¶¨Í±??†Í? / EJECT / ???§Ï†ï) ?? * spotlight cutout + tooltip ?ºÎ°ú ?àÎÇ¥?úÎã§.
 *
 * Íµ¨ÌòÑ Î∞©Ïãù:
 * - Box(fillMaxSize) + graphicsLayer(CompositingStrategy.Offscreen) Î°?BlendMode.Clear ?úÏÑ±?? * - drawWithContent ?àÏóê??dim ?ÑÏ≤¥ Ïπ†Ìïú ??drawRoundRect(BlendMode.Clear) Î°?spotlight ?ÅÏó≠Îß??¨Î™Ö?? * - ?∏Í≥Ω??EjectCoral ??ring ?ºÎ°ú Í∞ïÏ°∞
 * - Tooltip Card ???îÎ©¥ ?òÎã® Í≥†Ï†ï (?îÎ∞î?¥Ïä§ ?§Ïñë??Í≥†Î†§???®Ïàú??
 *
 * ?∏Ï∂úÎ∂Ä (MainScreen.kt) Í∞Ä [androidx.compose.ui.layout.onGloballyPositioned] Î°? * 4Í∞?Î≤ÑÌäº??window-relative Ï¢åÌëú + ?¨Í∏∞Î•?Íµ¨Ìï¥ [spotlights] Î°??ÑÎã¨?úÎã§.
 * Ï¢åÌëú ÎØ∏Ï∏°??step ?Ä dim Îß??úÏãú.
 *
 * @param spotlights step index(0..3) ??spotlight ?ïÎ≥¥(window pixel offset+size). null ?¥Î©¥ ÎØ∏Ï∏°??
 * @param onDone 4-step Î™®Îëê ?ùÎÇòÍ±∞ÎÇò "Í±¥ÎÑà?∞Í∏∞" ?¥Î¶≠ ???∏Ï∂ú. EjectPrefs.saveCoachmarkSeen(true) ??Ï≤òÎ¶¨.
 */
@Composable
fun CoachmarkOverlay(
    spotlights: List<CoachmarkSpotlight?>,
    onDone: () -> Unit,
) {
    val strings = LocalAppStrings.current

    val steps = remember(strings) {
        listOf(
            strings.coachmarkStep1Title to strings.coachmarkStep1Desc,
            strings.coachmarkStep2Title to strings.coachmarkStep2Desc,
            strings.coachmarkStep3Title to strings.coachmarkStep3Desc,
            strings.coachmarkStep4Title to strings.coachmarkStep4Desc,
        )
    }
    val total = steps.size
    var index by remember { mutableIntStateOf(0) }

    val spot = spotlights.getOrNull(index)
    val coralColor = EjectCoral

    Box(
        modifier = Modifier
            .fillMaxSize()
            // dim + cutout ?©ÏÑ±???ÑÌïú offscreen layer (BlendMode.Clear ?úÏÑ±??Ï°∞Í±¥)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // ?ÑÏ≤¥ ?îÎ©¥ dim
                drawRect(color = Color.Black.copy(alpha = 0.78f))
                // spotlight cutout (?àÏùÑ ?åÎßå)
                spot?.let {
                    val pad = 8.dp.toPx()
                    val left = it.offset.x - pad
                    val top = it.offset.y - pad
                    val w = it.size.width + pad * 2
                    val h = it.size.height + pad * 2
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                        blendMode = BlendMode.Clear,
                    )
                    // spotlight ?∏Í≥Ω Îπ®Í∞Ñ ring (Í∞ïÏ°∞)
                    drawRoundRect(
                        color = coralColor,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top row ??ÏßÑÌñâ??+ Í±¥ÎÑà?∞Í∏∞
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${index + 1} / $total",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                TextButton(onClick = onDone) {
                    Text(
                        text = strings.coachmarkSkip,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
            }

            // Í∞ÄÎ≥Ä spacer ??tooltip ??spotlight ?Ä Í≤πÏπòÏßÄ ?äÍ≤å ?¥Î¶º Î∞∞Ïπò.
            // spot.offset.y < 800px (?Ä???îÎ©¥ ?ÅÎ∞òÎ∂Ä) ?¥Î©¥ tooltip ???îÎ©¥ ?òÎã®???êÍ∏∞ ?ÑÌï¥
            // ?ÑÏ™Ω ?¨Î∞±???¨Í≤å, Í∑??∏Ïóî ?ëÍ≤å.
            Spacer(Modifier.height(if (spot != null && spot.offset.y < 800f) 240.dp else 64.dp))

            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (fadeIn(tween(180))) togetherWith (fadeOut(tween(120)))
                },
                label = "coachmark-step",
                modifier = Modifier.fillMaxWidth(),
            ) { i ->
                val (title, desc) = steps[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EjectSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        Text(
                            text = title,
                            color = EjectCoral,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = desc,
                            color = EjectOnSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ?§Ïùå / ?ÑÎ£å Î≤ÑÌäº
            Button(
                onClick = {
                    if (index < total - 1) index += 1 else onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EjectCoral,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    text = if (index < total - 1) strings.coachmarkNext else strings.onboardingFinalDismiss,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

/**
 * ÏΩîÏπòÎßàÌÅ¨ spotlight ?ÅÏó≠ ?ïÎ≥¥.
 * MainScreen ?êÏÑú [androidx.compose.ui.layout.onGloballyPositioned] Î°? * window-relative Ï¢åÌëú + ?¨Í∏∞Î•?Ï∏°Ï†ï??Ï±ÑÏö¥??
 *
 * @property offset window Ï¢åÌëúÍ≥ÑÏùò Ï¢åÏÉÅ??(px). LayoutCoordinates.positionInWindow() Í≤∞Í≥º.
 * @property size Î≤ÑÌäº ?ÅÏó≠ ?¨Í∏∞ (px).
 */
data class CoachmarkSpotlight(
    val offset: Offset,
    val size: Size,
)
