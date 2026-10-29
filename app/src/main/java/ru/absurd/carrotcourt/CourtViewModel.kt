package ru.absurd.carrotcourt

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ru.absurd.carrotcourt.data.*

class CourtViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CourtStore(application)
    private val engine = CourtEngine()
    var state by mutableStateOf(store.read())
        private set
    var animationTick by mutableStateOf(0)
        private set
    fun act(action: Action) {
        val next = engine.act(state, action)
        if (next == state) return
        state = next
        animationTick++
        store.write(state)
    }
    fun toggleSound() { state = state.copy(sound = !state.sound); store.write(state) }
    fun dismissEvent() { state = state.copy(event = null); store.write(state) }
}
