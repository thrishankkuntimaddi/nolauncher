package app.nolauncher.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import app.nolauncher.data.Gesture
import kotlin.math.abs

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    focusOn: Boolean,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAllApps: () -> Unit,
    onFocus: () -> Unit,
    onSettings: () -> Unit,
    onGesture: (Gesture) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        val t = 72.dp.toPx()
                        if (abs(dy) >= abs(dx)) {
                            if (dy < -t) onAllApps() else if (dy > t) onGesture(Gesture.SwipeDown)
                        } else {
                            if (dx > t) onGesture(Gesture.SwipeRight) else if (dx < -t) onGesture(Gesture.SwipeLeft)
                        }
                    },
                ) { change, drag ->
                    change.consume()
                    dx += drag.x
                    dy += drag.y
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onGesture(Gesture.DoubleTap) },
                    onLongPress = { onSettings() },
                )
            }
            .systemBarsPadding(),
    ) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (apps.isEmpty()) {
                Txt(
                    "Swipe up for your apps.\nLong-press one to keep it here.",
                    color = Palette.Muted,
                    size = 15.sp,
                    align = TextAlign.Center,
                )
            }
            apps.forEach { app ->
                AppLabel(app.label, 30.sp, onLongClick = { onMenu(app) }) { onOpen(app) }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = BarSpacing,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchPill(Modifier.weight(1f), onSearch)
            RoundButton(Glyph.Moon, active = focusOn, onClick = onFocus)
        }
    }
}
