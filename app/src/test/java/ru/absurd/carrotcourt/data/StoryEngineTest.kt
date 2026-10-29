package ru.absurd.carrotcourt.data

import org.junit.Assert.*
import org.junit.Test

class StoryEngineTest {
    private fun investigate(state: StoryProgress): StoryProgress = listOf("witness", "defense", "charge")
        .fold(state) { progress, id -> StoryEngine.reveal(progress, id) }
    private fun play(decisions: List<Int>, initial: StoryProgress = StoryProgress()): StoryProgress {
        var state = initial
        decisions.forEachIndexed { i, choice ->
            state = StoryEngine.decide(investigate(state), choice)
            if (i < decisions.lastIndex) state = StoryEngine.next(state)
        }
        return state
    }
    @Test fun cannotSkipInvestigationOrCountDecisionTwice() {
        val start = StoryProgress()
        assertEquals(start, StoryEngine.decide(start, 0))
        assertEquals(start, StoryEngine.next(start))
        val partlyRead = StoryEngine.reveal(start, "charge")
        assertEquals(partlyRead, StoryEngine.decide(partlyRead, 1))
        val investigated = investigate(start)
        assertTrue(investigated.ready)
        val closed = StoryEngine.decide(investigated, 0)
        assertEquals(closed, StoryEngine.decide(closed, 1))
        assertEquals(closed, StoryEngine.reveal(closed, "witness"))
        val next = StoryEngine.next(closed)
        assertEquals(1, next.chapter)
        assertTrue(next.seen.isEmpty())
        assertNull(next.dialogue)
        assertFalse(next.ready)
    }
    @Test fun allThirtyTwoDecisionPathsReachOneValidEnding() {
        val endings = mutableSetOf<String>()
        for (mask in 0..31) {
            val state = play(List(5) { (mask shr it) and 1 })
            assertTrue(state.completed)
            assertEquals(5, state.decisions.size)
            assertEquals(1, state.playthroughs)
            assertEquals(state, StoryEngine.next(state))
            assertEquals(state, StoryEngine.decide(state, 0))
            assertEquals(state, StoryEngine.reveal(state, "charge"))
            endings += StoryCatalog.ending(state).id
        }
        assertEquals(StoryCatalog.endings.map { it.id }.toSet(), endings)
    }
    @Test fun replayKeepsDiscoveredEndingsAndPlaythroughCount() {
        val first = play(List(5) { 0 })
        assertEquals("crunchy_constitution", StoryCatalog.ending(first).id)
        val replay = StoryEngine.replay(first)
        assertEquals(0, replay.chapter)
        assertFalse(replay.completed)
        assertTrue(replay.decisions.isEmpty())
        assertEquals(first.endings, replay.endings)
        val second = play(List(5) { 1 }, replay)
        assertEquals(2, second.playthroughs)
        assertEquals(2, second.endings.size)
        assertEquals("strict_soup", StoryCatalog.ending(second).id)
    }
    @Test fun closingDocumentsPreventsBestEndingEvenAfterFourFairDecisions() {
        assertEquals("compromise_soup", StoryCatalog.ending(play(listOf(0, 0, 0, 0, 1))).id)
    }
    @Test fun freeCourtLeavesStoryUntouched() {
        val story = play(listOf(0, 1, 0, 1, 0))
        val state = CourtState(story = story)
        val engine = CourtEngine(kotlin.random.Random(5))
        assertEquals(story, engine.act(state, Action.VERDICT).story)
        assertEquals(story, engine.act(state, Action.NEW).story)
    }
}
