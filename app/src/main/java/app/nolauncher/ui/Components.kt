package app.nolauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Bg = Color.Black
    val Ink = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8A8A8E)
    val Faint = Color(0xFF4A4A4E)
    val Surface = Color(0xFF1C1C1E)
}

@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    color: Color = Palette.Ink,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    family: FontFamily = FontFamily.Default,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            textAlign = align,
            fontFamily = family,
            letterSpacing = if (size.value >= 24) (-0.3).sp else 0.sp,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Press-to-dim modifier: no ripples, no color, just the text fading slightly. */
@Composable
fun Modifier.quietClick(onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (pressed) 0.4f else 1f, tween(120), label = "press")
    return this
        .graphicsLayer { this.alpha = alpha }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

@Composable
fun AppLabel(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Txt(
        text = text,
        size = size,
        weight = FontWeight.SemiBold,
        align = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .quietClick(onLongClick, onClick)
            .padding(horizontal = 28.dp, vertical = 6.dp),
    )
}

enum class Glyph { Search, Moon, Sliders, Gear, Dots, Chevron }

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val stroke = 1.8.dp.toPx()
        when (glyph) {
            Glyph.Search -> {
                drawCircle(color, radius = w * 0.3f, center = Offset(w * 0.43f, w * 0.43f), style = Stroke(stroke))
                drawLine(color, Offset(w * 0.65f, w * 0.65f), Offset(w * 0.9f, w * 0.9f), stroke, StrokeCap.Round)
            }
            Glyph.Moon -> {
                val outer = Path().apply { addOval(Rect(Offset(w * 0.5f, w * 0.5f), w * 0.4f)) }
                val bite = Path().apply { addOval(Rect(Offset(w * 0.74f, w * 0.3f), w * 0.33f)) }
                drawPath(Path().apply { op(outer, bite, PathOperation.Difference) }, color)
            }
            Glyph.Chevron -> {
                drawLine(color, Offset(w * 0.38f, w * 0.22f), Offset(w * 0.66f, w * 0.5f), stroke * 1.2f, StrokeCap.Round)
                drawLine(color, Offset(w * 0.66f, w * 0.5f), Offset(w * 0.38f, w * 0.78f), stroke * 1.2f, StrokeCap.Round)
            }
            Glyph.Dots -> {
                for (i in 0..1) for (j in 0..1) {
                    drawCircle(color, w * 0.11f, Offset(w * (0.32f + 0.36f * i), w * (0.32f + 0.36f * j)))
                }
            }
            Glyph.Gear -> {
                val c = Offset(w / 2, w / 2)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val dx = kotlin.math.cos(a).toFloat()
                    val dy = kotlin.math.sin(a).toFloat()
                    drawLine(
                        color, Offset(c.x + dx * w * 0.30f, c.y + dy * w * 0.30f),
                        Offset(c.x + dx * w * 0.44f, c.y + dy * w * 0.44f), stroke * 1.6f, StrokeCap.Round,
                    )
                }
                drawCircle(color, w * 0.30f, c, style = Stroke(stroke))
                drawCircle(color, w * 0.11f, c, style = Stroke(stroke))
            }
            Glyph.Sliders -> {
                listOf(0.32f to 0.34f, 0.68f to 0.66f).forEach { (y, knob) ->
                    drawLine(color, Offset(w * 0.1f, w * y), Offset(w * 0.9f, w * y), stroke, StrokeCap.Round)
                    drawCircle(Palette.Surface, w * 0.12f, Offset(w * knob, w * y))
                    drawCircle(color, w * 0.12f, Offset(w * knob, w * y), style = Stroke(stroke))
                }
            }
        }
    }
}

@Composable
fun RoundButton(glyph: Glyph, active: Boolean = false, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (active) Palette.Ink else Palette.Surface, label = "bg")
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bg)
            .quietClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, if (active) Palette.Bg else Palette.Ink)
    }
}

/** The non-editable "Search apps" pill shown on the home screen. */
@Composable
fun SearchPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Surface)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphIcon(Glyph.Search, Palette.Muted, 18.dp)
        Spacer(Modifier.width(10.dp))
        Txt("Search apps", color = Palette.Muted, size = 15.sp)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Ink)
            .quietClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Txt(text, color = Palette.Bg, size = 17.sp, weight = FontWeight.Medium)
    }
}

val BarSpacing = Arrangement.spacedBy(10.dp)
