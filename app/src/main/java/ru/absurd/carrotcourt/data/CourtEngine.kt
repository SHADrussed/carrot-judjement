package ru.absurd.carrotcourt.data

import kotlin.random.Random

enum class Action { ACCUSE, DEFEND, WITNESS, VERDICT, NEW }
data class Dialogue(val speaker: String, val text: String)
data class CaseRecord(val number: Int, val text: String, val guilty: Boolean, val secret: Boolean)
data class CourtState(
    val dialogue: Dialogue = Dialogue("Судья Картофель", "Встать, суд пророс! Морковка обвиняется в подозрительном хрусте. Стороны, начинайте этот беспредел."),
    val closed: Boolean = false,
    val completed: Int = 0,
    val guilty: Int = 0,
    val acquitted: Int = 0,
    val found: Set<String> = emptySet(),
    val history: List<CaseRecord> = emptyList(),
    val event: Secret? = null,
    val sound: Boolean = true
)

class CourtEngine(private val random: Random = Random.Default) {
    private val previous = mutableMapOf<String, Int>()
    private fun <T> pick(key: String, list: List<T>): T {
        val last = previous[key]
        val index = if (last == null || list.size < 2) random.nextInt(list.size) else {
            val next = random.nextInt(list.size - 1)
            if (next >= last) next + 1 else next
        }
        previous[key] = index
        return list[index]
    }
    fun act(state: CourtState, action: Action): CourtState {
        if (action == Action.NEW) return state.copy(closed = false, event = null,
            dialogue = Dialogue("Судья Картофель", "Дело №${state.completed + 1}. Новый бред принят к производству. Подсудимая, перестаньте фотосинтезировать на судью!"))
        if (state.closed) return state
        if (action == Action.VERDICT) {
            val verdict = if (random.nextInt(100) < 12) pick("secret", AbsurdCatalog.secretVerdicts)
                else pick("verdict", AbsurdCatalog.verdicts)
            val record = CaseRecord(state.completed + 1, verdict.text, verdict.guilty, verdict.secret)
            return state.copy(dialogue = Dialogue("Судья Картофель • приговор", verdict.text), closed = true,
                completed = state.completed + 1, guilty = state.guilty + if (verdict.guilty) 1 else 0,
                acquitted = state.acquitted + if (verdict.guilty) 0 else 1,
                found = if (verdict.secret) state.found + verdict.id else state.found,
                history = (listOf(record) + state.history).take(50), event = null)
        }
        val dialogue = when (action) {
            Action.ACCUSE -> Dialogue("Прокурор Брокколи", pick("accuse", AbsurdCatalog.accusations))
            Action.DEFEND -> Dialogue("Адвокат Майонез", pick("defend", AbsurdCatalog.defenses))
            else -> pick("witness", AbsurdCatalog.witnesses).let { Dialogue("Свидетель: ${it.name}", it.text) }
        }
        val event = if (random.nextInt(100) < 20) pick("event", AbsurdCatalog.events) else null
        return state.copy(dialogue = dialogue, event = event, found = if (event != null) state.found + event.id else state.found)
    }
}
