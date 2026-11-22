package app.nolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import app.nolauncher.data.AppGroup

private const val LOOP = 10_000

/**
 * Home is page 0 of a circle: swipe left/right through your groups and you come back round to home.
 * Swipe up = all apps, swipe down = who's waiting (notification names).
 */
@Composable
fun HomePager(
    home: List<AppEntry>,
    groups: List<Pair<AppGroup, List<AppEntry>>>,
    focusOn: Boolean,
    resetSignal: Int,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onSearch: () -> Unit,
    onAllApps: () -> Unit,
    onNotifications: () -> Unit,
    onFocus: () -> Unit,
    onSettings: () -> Unit,
    onEditGroup: (AppGroup) -> Unit,
) {
    val up by rememberUpdatedState(onAllApps)
    val down by rememberUpdatedState(onNotifications)
    val n = 1 + groups.size

    key(n) {
        val start = remember { if (n > 1) (LOOP / 2) - (LOOP / 2) % n else 0 }
        val pager = rememberPagerState(initialPage = start) { if (n > 1) LOOP else 1 }

        // Home button → glide back to the home page by the shortest way round.
        LaunchedEffect(resetSignal) {
            if (n > 1 && pager.currentPage % n != 0) {
                val cur = pager.currentPage
                val off = cur % n
                pager.animateScrollToPage(if (off <= n / 2) cur - off else cur + (n - off))
            }
        }

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
                .systemBarsPadding(),
        ) {
            Column(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    val idx = page % n
                    if (idx == 0) {
                        Page(
                            title = null,
                            apps = home,
                            empty = "Swipe up for your apps.\nLong-press one to keep it here.",
                            onOpen = onOpen, onMenu = onMenu, onHoldEmpty = onSettings,
                        )
                    } else {
                        val (group, apps) = groups[idx - 1]
                        Page(
                            title = group.name,
                            apps = apps,
                            empty = "Empty.\nHold here to add apps.",
                            onOpen = onOpen, onMenu = onMenu, onHoldEmpty = { onEditGroup(group) },
                        )
                    }
                }

                if (n > 1) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    ) {
                        val cur = pager.currentPage % n
                        repeat(n) { i ->
                            Box(
                                Modifier.size(if (i == cur) 6.dp else 5.dp).clip(CircleShape)
                                    .background(if (i == cur) Palette.Ink else Palette.Faint)
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = BarSpacing,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchPill(Modifier.weight(1f), onSearch)
                    RoundButton(Glyph.Moon, active = focusOn, onClick = onFocus)
                }
            }
        }
    }
}

@Composable
private fun Page(
    title: String?,
    apps: List<AppEntry>,
    empty: String,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onHoldEmpty: () -> Unit,
) {
    val hold by rememberUpdatedState(onHoldEmpty)
    val scroll = rememberScrollState()
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { hold() }) },
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                // Only scroll when a group is too long to fit, so swipe up/down keeps working.
                .verticalScroll(scroll, enabled = scroll.maxValue > 0)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (title != null) {
                Txt(title, color = Palette.Muted, size = 14.sp, weight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
            }
            if (apps.isEmpty()) Txt(empty, color = Palette.Muted, size = 15.sp, align = TextAlign.Center)
            apps.forEach { app ->
                AppLabel(app.label, 30.sp, onLongClick = { onMenu(app) }) { onOpen(app) }
            }
        }
    }
}
