package com.ejectbutton.ui.coachmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

class CoachmarkState {
    var isActive: Boolean by mutableStateOf(false)
        private set
    var index: Int by mutableStateOf(0)
        private set

    private val spots = mutableStateMapOf<String, Spot>()

    fun register(id: String, rect: Rect, shape: SpotShape) {
        spots[id] = Spot(rect, shape)
    }
    fun spotFor(id: String): Spot? = spots[id]

    fun start() { index = 0; isActive = true }
    fun next() { index += 1 }
    fun dismiss() { isActive = false }
}

@Composable
fun rememberCoachmarkState(): CoachmarkState = remember { CoachmarkState() }
