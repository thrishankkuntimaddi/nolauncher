package app.nolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import kotlin.math.abs

/** Width of the strip where home's apps sit as small icons while a panel is open. */
val ICON_STRIP = 76.dp

/** One row on home and in the panels — shared so names line up with icons. */
val HOME_ROW = 48.dp

/** The panel takes everything except the icon strip (capped on tablets/foldables). */
fun panelWidth(screen: Dp): Dp = min(screen - ICON_STRIP, 520.dp)

/**
 * progress: 0 = closed, +1 = left panel fully open (came from a swipe right),
 * -1 = right panel fully open (came from a swipe left). Home slides aside and stays visible in the
 * remaining strip. Everything stays pure black: names only, no tint, no icons.
 */
@Composable
fun SidePanels(
    progress: Float,
    left: List<AppEntry>,
    right: List<AppEntry>,
    onDrag: (dxFraction: Float) -> Unit,
    onDragEnd: () -> Unit,
    onClose: () -> Unit,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onEdit: (leftSide: Boolean) -> Unit,
    homeCount: Int,
) {
    if (progress == 0f) return
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = panelWidth(maxWidth)
        val wPx = constraints.maxWidth * (w / maxWidth)
        val fromLeft = progress > 0
        val p = abs(progress)
        val dragMod = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(onDragEnd = { end() }, onDragCancel = { end() }) { change, dx ->
                change.consume(); drag(dx / wPx)
            }
        }

        val x = if (fromLeft) -w * (1f - p) else maxWidth - w * p
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .background(Color.Black)
                .then(dragMod)
                .pointerInput(fromLeft) { detectTapGestures(onLongPress = { onEdit(fromLeft) }) }
                // Same frame as home, so both columns share one centre line.
                .systemBarsPadding()
                .padding(bottom = BAR_SPACE),
        ) {
            val apps = if (fromLeft) left else right
            // Rows share home's grid: same height, and when the two lists differ by an odd
            // number of apps, nudge by half a row so every name sits level with an icon.
            val nudge = if ((homeCount - apps.size) % 2 != 0 && homeCount > 0) HOME_ROW / 2 else 0.dp
            Column(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = nudge)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (apps.isEmpty()) {
                    Txt(
                        "Nothing here yet.\nHold to choose apps.",
                        color = Palette.Muted, size = 15.sp,
                        align = TextAlign.Center,
                    )
                }
                apps.forEach { app ->
                    Box(Modifier.height(HOME_ROW), contentAlignment = Alignment.Center) {
                        AppLabel(app.label, 30.sp, onLongClick = { onMenu(app) }) { onOpen(app) }
                    }
                }
            }
        }
    }
}
