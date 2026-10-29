package ru.absurd.carrotcourt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlin.math.sin

val Ink = Color(0xFF141222)
val Panel = Color(0xFF242036)
val Muted = Color(0xFFABA0BD)
val Cream = Color(0xFFFFE9C6)
val Orange = Color(0xFFFFAC61)
val Green = Color(0xFFA6DF83)
val Lilac = Color(0xFFC1ABFF)
val Red = Color(0xFFFF8A87)
val PixelFont = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(ru.absurd.carrotcourt.R.font.press_start))

private fun DrawScope.block(x: Number, y: Number, w: Number, h: Number, color: Color) =
    drawRect(color, Offset(x.toFloat(), y.toFloat()), Size(w.toFloat(), h.toFloat()))

private val carrot = listOf(
    "......gg.g.......", ".....gGGgGg......", "......gGGg.......", ".......gg........",
    "...ooooOoooo.....", "..oOOOOOOOOOo....", "..oOOOOOOOOOo....", "...oOOOOOOOo.....",
    "...oOOOOOOOo.....", "....oOOOOOo......", "....oOOOOOo......", ".....oOOOo.......",
    ".....oOOOo.......", "......oOo........", "......oo........."
)
private val broccoli = listOf(
    ".....gggg........", "...ggGGGGgg......", "..gGGGGGGGGg.....", ".gGGgGGGGgGGg....",
    ".gGGGGGGGGGGg....", "..ggGGGGGGgg.....", "....gggggg.......", ".....gLLg........",
    "....gLLLLg.......", "....gLLLLg.......", "...gLLLLLLg......", "...gggggggg......"
)
private val mayo = listOf(
    ".....rrrrrr......", ".....rRRRRr......", "....rrrrrrrr.....", "....wWWWWWWw.....",
    "...wWWWWWWWWw....", "...wWWWWWWWWw....", "...wWWWWWWWWw....", "...wWWWWWWWWw....",
    "...wWWWWWWWWw....", "...wWWWWWWWWw....", "...wrrrrrrrrw....", "...wrrrrrrrrw....",
    "...wWWWWWWWWw....", "....wwwwwwww....."
)
private val potato = listOf(
    "....hhhhhhh......", "..hhHHHHHHHhh....", "..hHHHHHHHHHh....", ".hHHHHHHHHHHHh...",
    ".hHHHHHHHHHHHh...", ".hHHHHHHHHHHHh...", "..hHHHHHHHHHh....", "..hHHHHHHHHHh....",
    "...hhhhhhhhh.....", "...kkkkkkkkk.....", "..kkkkWWWkkkk....", "..kkkkWWWkkkk...."
)
private val palette = mapOf('g' to Color(0xFF38734C), 'G' to Color(0xFF75BE61), 'L' to Color(0xFFB4D580),
    'o' to Color(0xFFB75636), 'O' to Color(0xFFFFA151), 'w' to Color(0xFFC5B6AC), 'W' to Color(0xFFFFEED2),
    'r' to Color(0xFF934757), 'R' to Color(0xFFEA7D7F), 'h' to Color(0xFF906342), 'H' to Color(0xFFD8AF71), 'k' to Ink)

private fun DrawScope.sprite(pattern: List<String>, x: Float, y: Float, pixel: Float, blink: Boolean, emotion: Int, eyesY: Int) {
    pattern.forEachIndexed { row, line -> line.forEachIndexed { col, c ->
        palette[c]?.let { block(x + col * pixel, y + row * pixel, pixel, pixel, it) }
    } }
    val ey = y + eyesY * pixel
    block(x + 5 * pixel, ey, pixel, if (blink) pixel / 2 else pixel * 1.5f, Ink)
    block(x + 9 * pixel, ey, pixel, if (blink) pixel / 2 else pixel * 1.5f, Ink)
    if (emotion == 1) {
        block(x + 6 * pixel, ey + 3 * pixel, 3 * pixel, pixel, Ink)
        block(x + 5 * pixel, ey + 2 * pixel, pixel, pixel, Ink)
        block(x + 9 * pixel, ey + 2 * pixel, pixel, pixel, Ink)
    } else if (emotion == 2) {
        block(x + 7 * pixel, ey + 3 * pixel, pixel * 2, pixel * 2, Ink)
    } else block(x + 7 * pixel, ey + 3 * pixel, 2 * pixel, pixel / 2, Ink)
}

@Composable
fun PixelCourt(tick: Int, closed: Boolean, guilty: Boolean, speaker: String, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "court breath")
    val idle by infinite.animateFloat(0f, 6.28318f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "idle")
    val reaction = remember { Animatable(0f) }
    var blinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(3100); blinking = true; delay(140); blinking = false } }
    LaunchedEffect(tick) { if (tick > 0) { reaction.snapTo(1f); reaction.animateTo(0f, tween(900)) } }
    Canvas(modifier.fillMaxWidth().aspectRatio(1.65f).clipToBounds().semantics {
        contentDescription = "Пиксельный зал суда. Сверху судья Картофель, слева прокурор Брокколи, справа адвокат Майонез, в центре Морковка."
    }) {
        scale(size.width / 360f, size.height / 218f, pivot = Offset.Zero) {
            block(0, 0, 360, 218, Color(0xFF30283F))
            // Stone wall and warm courtroom panels.
            for (row in 0..4) for (col in 0..9) {
                block(col * 44 + if (row % 2 == 0) 0 else -22, row * 25, 42, 23, Color(0xFF352D45))
            }
            block(13, 0, 8, 150, Color(0xFF51405A)); block(339, 0, 8, 150, Color(0xFF51405A))
            block(30, 0, 45, 73, Color(0xFF783F54)); block(34, 0, 4, 68, Color(0xFFAB626D))
            block(285, 0, 45, 73, Color(0xFF783F54)); block(322, 0, 4, 68, Color(0xFFAB626D))
            block(45, 25, 16, 3, Orange); block(51, 19, 3, 24, Orange)
            block(298, 25, 16, 3, Orange); block(304, 19, 3, 24, Orange)
            // Frame around the judge, little crown.
            block(136, 5, 88, 80, Color(0xFF9E7754)); block(140, 9, 80, 72, Ink)
            block(174, 12, 12, 3, Orange); block(171, 9, 3, 6, Orange); block(186, 9, 3, 6, Orange)
            sprite(potato, 157f, 23f, 3.2f, blinking, 0, 4)
            block(108, 69, 144, 12, Color(0xFFA57555)); block(115, 81, 130, 27, Color(0xFF684838))
            block(122, 86, 116, 17, Color(0xFF80543E)); block(170, 86, 20, 12, Color(0xFFD2AA67))
            block(235, 59, 5, 12, Orange); block(229, 54, 16, 7, Color(0xFFC89765))
            // Floor perspective, square tile highlights.
            block(0, 144, 360, 74, Color(0xFF292334))
            for (r in 0..3) for (c in 0..8) block(c * 46 + (r % 2) * 23, 150 + r * 19, 43, 16, Color(0xFF332A3B))
            val hop = -kotlin.math.abs(sin(idle * 2)) * reaction.value * 12
            val shake = sin(idle * 22) * reaction.value * 4
            // Lawyers, behind their lecterns.
            sprite(broccoli, 33f + if (speaker.contains("Брокколи")) shake else 0f, 94f + sin(idle) * 1.2f, 3.7f, blinking, 2, 6)
            sprite(mayo, 269f, 91f + if (speaker.contains("Майонез")) hop else 0f, 3.5f, blinking, 1, 5)
            listOf(24, 267).forEach { x ->
                block(x, 146, 70, 8, Color(0xFFB98259)); block(x + 5, 154, 60, 42, Color(0xFF714A3D))
                block(x + 11, 160, 48, 28, Color(0xFF865642)); block(x + 16, 165, 3, 18, Color(0xFFA57555))
            }
            block(138, 192, 85, 7, Color(0xFF1C1929))
            sprite(carrot, 145f + shake, 110f + hop + sin(idle) * 2, 4.6f, blinking,
                if (closed) { if (guilty) 2 else 1 } else if (speaker.contains("Брокколи")) 2 else 0, 6)
            block(140, 171, 9, 3, Color(0xFFB75636)); block(213, 171, 9, 3, Color(0xFFB75636))
            block(163, 183 + hop, 6, 8, Color(0xFFB75636)); block(194, 183 + hop, 6, 8, Color(0xFFB75636))
            // Foreground rail.
            block(0, 207, 360, 5, Color(0xFFB3825C)); block(0, 212, 360, 6, Color(0xFF5B3E36))
            for (x in 8..350 step 28) block(x, 198, 5, 20, Color(0xFF916347))
            // Sparkling dust.
            for (i in 0..7) block((i * 47 + 17) % 350, 13 + ((i * 31) % 130), 2, 2, Cream.copy(alpha = 0.15f + (sin(idle + i) + 1) * 0.1f))
        }
    }
}

@Composable
fun VerdictConfetti(tick: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(tick) { if (tick > 0) { progress.snapTo(0f); progress.animateTo(1f, tween(2200, easing = LinearEasing)) } }
    Canvas(modifier) {
        if (progress.value < 1f) repeat(65) { i ->
            val x = ((i * 83 % 997) / 997f * size.width + sin(progress.value * 10 + i) * 20)
            val y = (progress.value * 1.5f - (i % 7) * 0.08f) * size.height
            drawRect(listOf(Orange, Green, Lilac, Cream)[i % 4].copy(alpha = 1 - progress.value), Offset(x, y), Size(7f + i % 4, 12f))
        }
    }
}
