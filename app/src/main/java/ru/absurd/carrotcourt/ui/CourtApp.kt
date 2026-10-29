package ru.absurd.carrotcourt.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.absurd.carrotcourt.CourtViewModel
import ru.absurd.carrotcourt.data.*

private val CourtColors = darkColorScheme(primary = Orange, background = Ink, surface = Panel, onSurface = Cream, onBackground = Cream)

@Composable
fun CourtApp(vm: CourtViewModel = viewModel()) {
    val context = LocalContext.current
    val state = vm.state
    var screen by rememberSaveable { mutableIntStateOf(0) }
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tones?.release() } }
    BackHandler(screen != 0) { screen = 0 }
    val act: (Action) -> Unit = { action ->
        if (!state.closed || action == Action.NEW) {
            vm.act(action)
            if (state.sound) runCatching { tones?.startTone(if (action == Action.VERDICT) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP, if (action == Action.VERDICT) 220 else 65) }
            if (action == Action.VERDICT) {
                val vibrator = context.getSystemService(Vibrator::class.java)
                runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 60, 140), -1)) }
            }
        }
    }
    MaterialTheme(colorScheme = CourtColors, typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, lineHeight = 22.sp))) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Green))
                    Text(" ОВОЩНОЙ ТРИБУНАЛ", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::toggleSound, shape = RectangleShape, contentPadding = PaddingValues(8.dp)) {
                        Text(if (state.sound) "ЗВУК: ВКЛ" else "ЗВУК: ВЫКЛ", color = if (state.sound) Green else Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val pageScroll = key(screen) { rememberScrollState() }
                    Column(Modifier.widthIn(max = 560.dp).align(Alignment.TopCenter).fillMaxSize().verticalScroll(pageScroll).padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (screen) {
                            0 -> CourtScreen(state, vm.animationTick, act, vm::dismissEvent)
                            1 -> ArchiveScreen(state)
                            else -> GuideScreen(state)
                        }
                    }
                    if (screen == 0 && state.closed) VerdictConfetti(vm.animationTick, Modifier.matchParentSize())
                }
                Row(Modifier.fillMaxWidth().background(Panel).border(1.dp, Color(0xFF40364F)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("СУД", "АРХИВ", "АБСУРД").forEachIndexed { i, label ->
                        TextButton(onClick = { screen = i }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RectangleShape,
                            colors = ButtonDefaults.textButtonColors(containerColor = if (screen == i) Color(0xFF3A2C37) else Color.Transparent)) {
                            Text((if (screen == i) "▪ " else "") + label, fontFamily = PixelFont, fontSize = 9.sp, color = if (screen == i) Orange else Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourtScreen(state: CourtState, tick: Int, act: (Action) -> Unit, dismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("СУД НАД\nМОРКОВКОЙ", fontFamily = PixelFont, fontSize = 23.sp, lineHeight = 34.sp, color = Cream)
        Text("Закон суров. Но он немного овощ.", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
    Column(Modifier.border(2.dp, Color(0xFF65506B))) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ДЕЛО №${(if (state.closed) state.completed else state.completed + 1).toString().padStart(3, '0')}", fontFamily = PixelFont, fontSize = 9.sp, color = Orange)
            Text(if (state.closed) "● ЗАКРЫТО" else "● ИДЁТ ЗАСЕДАНИЕ", color = if (state.closed) Lilac else Green, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        PixelCourt(tick, state.closed, state.history.firstOrNull()?.guilty ?: false, state.dialogue.speaker)
        Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("БРОККОЛИ", color = Green, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("↑ ПОДСУДИМАЯ", color = Orange, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("МАЙОНЕЗ", color = Lilac, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
    PixelPanel(accent = if (state.closed) Orange else Lilac) {
        Text(state.dialogue.speaker.uppercase(), color = if (state.closed) Orange else Lilac, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(state.dialogue.text, style = MaterialTheme.typography.bodyLarge, color = Cream, modifier = Modifier.heightIn(min = 66.dp))
        if (state.closed) Text(if (state.history.firstOrNull()?.guilty == true) "ВИНОВНА • молоток сказал своё" else "ОПРАВДАНА • хрустите свободно", color = if (state.history.firstOrNull()?.guilty == true) Red else Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
    if (state.event != null) PixelPanel(accent = Green) {
        Text("★ СЕКРЕТ: ${state.event.title}", color = Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(state.event.text, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = dismiss) { Text("Внесено в справочник. Понятно!", color = Green, fontSize = 12.sp) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PixelButton("Обвинить", "!", Red, Modifier.weight(1f), !state.closed) { act(Action.ACCUSE) }
            PixelButton("Защитить", "+", Green, Modifier.weight(1f), !state.closed) { act(Action.DEFEND) }
        }
        PixelButton("Допросить свидетеля", "?", Lilac, enabled = !state.closed) { act(Action.WITNESS) }
        PixelButton("Вынести приговор", "▣", Orange, enabled = !state.closed, filled = true) { act(Action.VERDICT) }
        PixelButton("Новый бред", "↻", Muted) { act(Action.NEW) }
    }
    Text(if (state.closed) "Дело в архиве. Жмите «Новый бред» — грядка ждёт." else "Выбирай сторону. Здесь даже у майонеза есть аргументы.", fontSize = 11.sp, lineHeight = 16.sp, color = Muted, fontFamily = FontFamily.Monospace)
}

@Composable
private fun PixelButton(text: String, icon: String, accent: Color, modifier: Modifier = Modifier, enabled: Boolean = true, filled: Boolean = false, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RectangleShape,
        border = BorderStroke(2.dp, if (enabled) accent else Color(0xFF423A4D)),
        colors = ButtonDefaults.buttonColors(containerColor = if (filled) accent else Panel, contentColor = if (filled) Ink else accent,
            disabledContainerColor = Panel, disabledContentColor = Color(0xFF82768E)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(icon, fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.padding(end = 10.dp))
        Text(text, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun PixelPanel(accent: Color = Lilac, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Panel).border(2.dp, accent.copy(alpha = .65f)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(title, fontFamily = PixelFont, fontSize = 20.sp, lineHeight = 30.sp, color = Cream)
    Text(subtitle, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
private fun ArchiveScreen(state: CourtState) {
    PageTitle("АРХИВ ДЕЛ", "Всё, что вы наговорили, уже стало гарниром.")
    PixelPanel(Orange) {
        Stat("Рассмотрено дел", state.completed, Orange)
        HorizontalDivider(color = Muted.copy(alpha = .2f))
        Stat("Виновна", state.guilty, Red)
        Stat("Оправдана", state.acquitted, Green)
        Stat("Тайных приговоров", state.found.count { it.startsWith("verdict_") }, Lilac)
    }
    Text("ПОСЛЕДНИЕ ДЕЛА / ${state.history.size}", fontFamily = FontFamily.Monospace, color = Muted, fontSize = 12.sp)
    if (state.history.isEmpty()) PixelPanel {
        Text("Пока ни одной судимости. Даже подозрительно.", style = MaterialTheme.typography.bodyLarge)
        Text("Вынесите первый приговор в зале суда.", color = Muted, fontSize = 12.sp)
    }
    state.history.forEach { record ->
        PixelPanel(if (record.guilty) Red else Green) {
            Text("ДЕЛО №${record.number} • ${if (record.guilty) "ВИНОВНА" else "ОПРАВДАНА"}${if (record.secret) " ★" else ""}", color = if (record.guilty) Red else Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(record.text, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Text("На устройстве хранятся последние 50 дел. Общая статистика учитывает все завершённые дела.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
}

@Composable
private fun Stat(label: String, value: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Text(value.toString(), color = color, fontFamily = PixelFont, fontSize = 20.sp)
    }
}

@Composable
private fun GuideScreen(state: CourtState) {
    PageTitle("СПРАВОЧНИК\nАБСУРДА", "Редкие находки из юридического огорода.")
    PixelPanel(Green) {
        Stat("Найдено секретов", state.found.size, Green)
        LinearProgressIndicator(progress = { state.found.size / AbsurdCatalog.secrets.size.toFloat() }, modifier = Modifier.fillMaxWidth(), color = Green, trackColor = Ink)
        Text("${state.found.size} из ${AbsurdCatalog.secrets.size} • 12 событий и 4 тайных приговора", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
    Text("Обвиняйте, защищайте и допрашивайте: шанс события — 20%. При вынесении приговора шанс тайного решения — 12%.", color = Muted, fontSize = 12.sp, lineHeight = 19.sp)
    AbsurdCatalog.secrets.forEachIndexed { i, secret ->
        val found = secret.id in state.found
        PixelPanel(if (found) Orange else Color(0xFF574B69)) {
            Text(if (found) "★ ${secret.title}" else "? НЕИЗВЕСТНЫЙ АБСУРД #${i + 1}", color = if (found) Orange else Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text(if (found) secret.text else if (secret.id.startsWith("verdict_")) "Скрывается за ударом судейского молотка." else "Может случиться во время любого выступления.", style = MaterialTheme.typography.bodyLarge, color = if (found) Cream else Muted)
        }
    }
}
