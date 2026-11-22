package app.nolauncher.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry

/**
 * Just your essentials, centred.
 *  swipe up → all apps · swipe down → who's waiting
 *  swipe right/left → side panels (they follow your finger)
 *  long-press empty space → settings
 */
@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onAllApps: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onPanelDrag: (dxFraction: Float) -> Unit,
    onPanelDragEnd: () -> Unit,
) {
    val up by rememberUpdatedState(onAllApps)
    val down by rememberUpdatedState(onNotifications)
    val settings by rememberUpdatedState(onSettings)
    val drag by rememberUpdatedState(onPanelDrag)
    val dragEnd by rememberUpdatedState(onPanelDragEnd)
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val panelPx = with(LocalDensity.current) { panelWidth(screenW).toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = {
                        val t = 72.dp.toPx()
                        if (dy < -t) up() else if (dy > t) down()
                    },
                ) { _, d -> dy += d }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragEnd() },
                    onDragCancel = { dragEnd() },
                ) { change, dx -> change.consume(); drag(dx / panelPx) }
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { settings() }) }
            .systemBarsPadding()
            .padding(bottom = BAR_SPACE),
    ) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (apps.isEmpty()) {
                Txt(
                    "Swipe up for your apps.\nLong-press one to keep it here.",
                    color = Palette.Muted, size = 15.sp, align = TextAlign.Center,
                )
            }
            apps.forEach { app ->
                AppLabel(app.label, 30.sp, onLongClick = { onMenu(app) }) { onOpen(app) }
            }
        }
    }
}
