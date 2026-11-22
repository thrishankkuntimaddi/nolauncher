package app.nolauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import kotlinx.coroutines.delay

val BAR_HEIGHT = 52.dp
val BAR_SPACE = BAR_HEIGHT + 32.dp

/**
 * Two shapes, one bar:
 *  - collapsed: a search circle on the left + a pill of small monochrome icons (Focus, quick apps, Settings)
 *    that scrolls sideways;
 *  - expanded: the circle stretches into a full "Search apps" field and the pill slides away.
 */
@Composable
fun BottomBar(
    expanded: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    focusSignal: Int,
    onGo: () -> Unit,
    onSearchTap: () -> Unit,
    dock: List<AppEntry>,
    focusOn: Boolean,
    onFocus: () -> Unit,
    onOpen: (AppEntry) -> Unit,
    onEditDock: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val raw by animateFloatAsState(
        if (expanded) 1f else 0f,
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        label = "bar",
    )
    // The spring can overshoot slightly past 0..1; sizes and paddings must never go negative.
    val t = raw.coerceIn(0f, 1f)
    val focus = androidx.compose.runtime.remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusSignal) {
        if (focusSignal > 0 && expanded) {
            delay(180)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }

    BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).height(BAR_HEIGHT).clipToBounds(),
    ) {
        val gap = 10.dp
        val full = maxWidth
        val searchW = BAR_HEIGHT + (full - BAR_HEIGHT) * t

        // Search: circle ↔ field
        Row(
            Modifier
                .width(searchW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Palette.Surface)
                .then(if (!expanded) Modifier.quietClick(onLongClick = onSettings, onClick = onSearchTap) else Modifier)
                .padding(start = 17.dp + 1.dp * t, end = 14.dp * t),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphIcon(Glyph.Search, if (expanded) Palette.Muted else Palette.Ink, 18.dp)
            if (expanded) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.weight(1f).graphicsLayer { alpha = ((t - 0.5f) * 2f).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) Txt("Search apps", color = Palette.Muted, size = 15.sp, maxLines = 1)
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(Palette.Ink),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onGo() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
        }

        // Dock pill: slides out to the right as the search field grows.
        if (t < 0.7f) {
            val dockW = full - BAR_HEIGHT - gap
            Box(
                Modifier
                    .offset(x = BAR_HEIGHT + gap + (full - BAR_HEIGHT) * t)
                    .width(dockW)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = (1f - t / 0.7f).coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Surface),
            ) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(start = 6.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item(key = "focus") {
                            DockSlot(active = focusOn, onClick = onFocus, onLongClick = onEditDock) {
                                GlyphIcon(Glyph.Moon, if (focusOn) Palette.Bg else Palette.Ink, 18.dp)
                            }
                        }
                        items(dock, key = { it.key }) { app ->
                            DockSlot(onClick = { onOpen(app) }, onLongClick = onEditDock) { AppIcon(app, 22.dp) }
                        }
                    }
                    // Settings stays pinned at the end so it's always reachable.
                    Box(Modifier.width(1.dp).height(22.dp).background(Palette.Faint))
                    Box(Modifier.padding(horizontal = 5.dp)) {
                        DockSlot(onClick = onSettings, onLongClick = onEditDock) {
                            GlyphIcon(Glyph.Gear, Palette.Ink, 19.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockSlot(
    active: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (active) Palette.Ink else androidx.compose.ui.graphics.Color.Transparent)
            .quietClick(onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
