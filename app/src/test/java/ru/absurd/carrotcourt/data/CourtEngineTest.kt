package ru.absurd.carrotcourt.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class CourtEngineTest {
    @Test fun catalogMeetsRequirementsAndHasUniqueStableIds() {
        assertTrue(AbsurdCatalog.accusations.distinct().size >= 50)
        assertTrue(AbsurdCatalog.defenses.distinct().size >= 50)
        assertTrue(AbsurdCatalog.witnesses.distinct().size >= 30)
        assertTrue(AbsurdCatalog.verdicts.size >= 40)
        assertTrue(AbsurdCatalog.events.size >= 10)
        assertEquals(9, AbsurdCatalog.witnesses.map { it.name }.distinct().size)
        assertEquals(AbsurdCatalog.secrets.size, AbsurdCatalog.secrets.map { it.id }.distinct().size)
    }
    @Test fun closedCaseCannotBeCountedOrChangedTwice() {
        val engine = CourtEngine(Random(42))
        val closed = engine.act(CourtState(), Action.VERDICT)
        assertTrue(closed.closed)
        assertEquals(1, closed.completed)
        assertEquals(1, closed.guilty + closed.acquitted)
        listOf(Action.VERDICT, Action.ACCUSE, Action.DEFEND, Action.WITNESS).forEach {
            assertEquals(closed, engine.act(closed, it))
        }
        val fresh = engine.act(closed, Action.NEW)
        assertFalse(fresh.closed)
        assertEquals(closed.completed, fresh.completed)
        assertEquals(closed.history, fresh.history)
        assertEquals(2, engine.act(fresh, Action.VERDICT).completed)
    }
    @Test fun newCaseDoesNotCountUnfinishedTrial() {
        val engine = CourtEngine(Random(9))
        val state = engine.act(engine.act(CourtState(), Action.ACCUSE), Action.NEW)
        assertEquals(0, state.completed)
        assertTrue(state.history.isEmpty())
    }
    @Test fun longPlayKeepsCountersConsistentAndSecretsUnique() {
        val engine = CourtEngine(Random(2026))
        var state = CourtState()
        repeat(2000) {
            state = engine.act(state, Action.NEW)
            state = engine.act(state, Action.WITNESS)
            state = engine.act(state, Action.VERDICT)
        }
        assertEquals(2000, state.completed)
        assertEquals(state.completed, state.guilty + state.acquitted)
        assertEquals(50, state.history.size)
        assertEquals(2000, state.history.first().number)
        assertEquals(AbsurdCatalog.secrets.map { it.id }.toSet(), state.found)
        assertTrue(state.guilty > 0)
        assertTrue(state.acquitted > 0)
    }
    @Test fun consecutiveStatementsDoNotRepeat() {
        val engine = CourtEngine(Random(8))
        for (action in listOf(Action.ACCUSE, Action.DEFEND, Action.WITNESS)) {
            var state = CourtState()
            repeat(200) {
                val next = engine.act(state, action)
                assertNotEquals(state.dialogue.text, next.dialogue.text)
                state = next
            }
        }
    }
}
