package app.nolauncher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    focusOn: Boolean,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAllApps: () -> Unit,
    onPullShade: () -> Unit,
    onFocus: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -80.dp.toPx() -> onAllApps()
                            total > 80.dp.toPx() -> onPullShade()
                        }
                    },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onSettings() }) }
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
