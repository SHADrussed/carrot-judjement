package ru.absurd.carrotcourt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.absurd.carrotcourt.data.*

@Composable
fun StoryScreen(story: StoryProgress, tick: Int, reveal: (String) -> Unit, decide: (Int) -> Unit, next: () -> Unit, replay: () -> Unit) {
    var showNotebook by rememberSaveable { mutableStateOf(false) }
    var showIntro by rememberSaveable(story.chapter) { mutableStateOf(false) }
    val chapter = StoryCatalog.chapters[story.chapter]
    PageTitle("ДЕЛО О ПРОПАВШЕМ\nПОЛОВНИКЕ", "Сюжетная кампания • пять глав • три финала")
    LinearProgressIndicator(progress = { story.decisions.size / 5f }, modifier = Modifier.fillMaxWidth(), color = Orange, trackColor = Panel)
    if (story.completed) {
        val ending = StoryCatalog.ending(story)
        Text("РАССЛЕДОВАНИЕ ЗАВЕРШЕНО", color = Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        PixelCourt(tick, true, false, "Морковка")
        PixelPanel(Orange) {
            Text(ending.title.uppercase(), fontFamily = PixelFont, fontSize = 15.sp, lineHeight = 23.sp, color = Orange)
            Text(ending.text, style = MaterialTheme.typography.bodyLarge)
            Text("Решений в защиту справедливости: ${story.fairness}/5", fontFamily = FontFamily.Monospace, color = Green, fontSize = 12.sp)
        }
        PixelPanel(Lilac) {
            Text("ВАШ ПОСЛЕДНИЙ ПРИГОВОР", color = Lilac, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(story.dialogue?.text.orEmpty(), style = MaterialTheme.typography.bodyLarge)
        }
        Text("Открыто финалов: ${story.endings.size}/3. Все найденные финалы сохранятся при новом прохождении.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        StoryCatalog.endings.forEach { entry ->
            if (entry.id in story.endings) PixelPanel(Green) {
                Text("★ ${entry.title}", color = Green, fontFamily = FontFamily.Monospace)
                Text(entry.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        PixelButton("Пройти историю заново", "↻", Orange, filled = true, onClick = replay)
        return
    }
    Text("ГЛАВА ${story.chapter + 1}/5 • ${chapter.location.uppercase()}", fontSize = 11.sp, color = Orange, fontFamily = FontFamily.Monospace)
    Text(chapter.title, fontFamily = PixelFont, color = Cream, fontSize = 17.sp, lineHeight = 26.sp)
    PixelCourt(tick, story.decided, story.decisions.lastOrNull() == 1, story.dialogue?.speaker.orEmpty())
    if (story.dialogue == null || showIntro) PixelPanel(Lilac) {
        Text(if (story.chapter > 0 && story.decisions.getOrNull(story.chapter - 1) == 1) chapter.consequenceIntro + "\n\n" + chapter.introduction else chapter.introduction,
            style = MaterialTheme.typography.bodyLarge)
    }
    if (story.dialogue != null) {
        TextButton(onClick = { showIntro = !showIntro }) { Text(if (showIntro) "Скрыть завязку главы ↑" else "Вспомнить завязку главы ↓", color = Muted) }
        PixelPanel(if (story.decided) Orange else Lilac) {
            Text(story.dialogue.speaker.uppercase(), fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = if (story.decided) Orange else Lilac)
            Text(story.dialogue.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (!story.decided) {
        Text("ИЗУЧЕНО ${story.seen.size}/3 • выслушайте все стороны", fontSize = 11.sp, color = Muted, fontFamily = FontFamily.Monospace)
        chapter.evidence.forEach { evidence ->
            val label = when (evidence.id) { "charge" -> "Версия обвинения"; "defense" -> "Доводы защиты"; else -> "Допросить свидетеля" }
            PixelButton(label, if (evidence.id in story.seen) "✓" else "?", if (evidence.id in story.seen) Green else Lilac) { reveal(evidence.id) }
        }
        if (story.ready) {
            Text("УЛИКИ СОБРАНЫ. КАК РЕШИТ СУД?", color = Orange, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            chapter.choices.forEachIndexed { i, choice ->
                PixelButton(choice.label, if (i == 0) "A" else "Б", if (i == 0) Orange else Lilac, filled = i == 0) { decide(i) }
            }
            Text("Решение изменит следующую главу и финал. После оглашения его нельзя отменить.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        } else Text("Для решения нужны все три показания. Их можно читать в любом порядке.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    } else PixelButton("Следующая глава", ">", Orange, filled = true, onClick = next)
    TextButton(onClick = { showNotebook = !showNotebook }) { Text(if (showNotebook) "Закрыть блокнот улик ↑" else "Открыть блокнот улик ↓", color = Green) }
    if (showNotebook) {
        val entries = StoryCatalog.chapters.take(story.chapter).flatMap { it.evidence } + chapter.evidence.filter { it.id in story.seen }
        if (entries.isEmpty()) Text("Пока пусто. Начните с показаний.", color = Muted)
        entries.forEach { evidence -> PixelPanel(Green) {
            Text(evidence.speaker, color = Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(evidence.clue, style = MaterialTheme.typography.bodyLarge)
        } }
    }
    Text("История сохраняется автоматически. Свободные дела в разделе «Суд» не меняют сюжет.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
}

private data class Achievement(val title: String, val description: String, val unlocked: Boolean)

@Composable
fun Achievements(state: CourtState) {
    val awards = listOf(
        Achievement("Первый хруст", "Вынести первый приговор в свободном суде.", state.completed >= 1),
        Achievement("Судья со стажем", "Рассмотреть 10 свободных дел.", state.completed >= 10),
        Achievement("Овощной верховный суд", "Рассмотреть 50 свободных дел.", state.completed >= 50),
        Achievement("Что это сейчас было?", "Найти первое редкое событие или приговор.", state.found.isNotEmpty()),
        Achievement("Доктор абсурдных наук", "Собрать все 16 секретов свободного суда.", state.found.size >= AbsurdCatalog.secrets.size),
        Achievement("Дело сдвинулось с грядки", "Принять первое решение в сюжетной кампании.", state.story.decisions.isNotEmpty() || state.story.playthroughs > 0),
        Achievement("Дочитать мелкий шрифт", "Завершить сюжетную кампанию.", state.story.playthroughs > 0),
        Achievement("Хрустящая конституция", "Открыть финал, в котором город доверяет суду.", "crunchy_constitution" in state.story.endings),
        Achievement("Все оттенки борща", "Открыть все три сюжетных финала.", state.story.endings.size == 3)
    )
    Text("НАГРАДЫ: ${awards.count { it.unlocked }}/${awards.size}", color = Orange, fontFamily = PixelFont, fontSize = 12.sp)
    awards.forEach { award -> PixelPanel(if (award.unlocked) Green else Muted) {
        Text((if (award.unlocked) "★ " else "◇ ") + award.title, color = if (award.unlocked) Green else Muted, fontFamily = FontFamily.Monospace)
        Text(award.description, style = MaterialTheme.typography.bodyLarge)
    } }
}
