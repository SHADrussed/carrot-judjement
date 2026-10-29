package ru.absurd.carrotcourt

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import ru.absurd.carrotcourt.data.*

class CourtViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CourtStore(application)
    private val engine = CourtEngine()
    var state by mutableStateOf(store.read())
        private set
    var animationTick by mutableIntStateOf(0)
        private set
    fun act(action: Action) {
        val next = engine.act(state, action)
        if (next == state) return
        state = next
        animationTick++
        store.write(state)
    }
    fun toggleSound() { state = state.copy(sound = !state.sound); store.write(state) }
    fun toggleMusic() { state = state.copy(music = !state.music); store.write(state) }
    fun toggleAmbience() { state = state.copy(ambience = !state.ambience); store.write(state) }
    private fun updateStory(next: StoryProgress) {
        if (next == state.story) return
        state = state.copy(story = next)
        animationTick++
        store.write(state)
    }
    fun revealStory(id: String) = updateStory(StoryEngine.reveal(state.story, id))
    fun decideStory(choice: Int) = updateStory(StoryEngine.decide(state.story, choice))
    fun nextChapter() = updateStory(StoryEngine.next(state.story))
    fun replayStory() = updateStory(StoryEngine.replay(state.story))
    fun dismissEvent() { state = state.copy(event = null); store.write(state) }
}
