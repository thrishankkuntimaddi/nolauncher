package app.nolauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import app.nolauncher.data.AppGroup

private const val LOOP = 10_000

/**
 * All apps. Swipe left/right to walk through your groups (it loops).
 * Typing in the search bar shows matches across everything.
 */
@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    groups: List<Pair<AppGroup, List<AppEntry>>>,
    query: String,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onClose: () -> Unit,
    onScrolled: () -> Unit,
    onEditGroup: (AppGroup) -> Unit,
    onWebSearch: (String) -> Unit,
) {
    val scrolled by rememberUpdatedState(onScrolled)
    Box(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(bottom = BAR_SPACE)) {
        if (query.isNotBlank()) {
            val results = remember(apps, query) { filterApps(apps, query) }
            val state = rememberLazyListState()
            LaunchedEffect(query) { state.scrollToItem(0) }
            WheelPage(null, results, state, onOpen, onMenu, onClose, {}, scrolled)
            if (results.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("Nothing called “$query”", color = Palette.Muted, size = 15.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    AppLabel("Search the web", 22.sp) { onWebSearch(query) }
                }
            }
        } else {
            val n = 1 + groups.size
            key(n) {
                val start = remember { if (n > 1) (LOOP / 2) - (LOOP / 2) % n else 0 }
                val pager = rememberPagerState(initialPage = start) { if (n > 1) LOOP else 1 }
                HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val idx = page % n
                    val state = rememberLazyListState()
                    if (idx == 0) {
                        WheelPage(if (n > 1) "All apps" else null, apps, state, onOpen, onMenu, onClose, {}, scrolled)
                    } else {
                        val (g, list) = groups[idx - 1]
                        WheelPage(g.name, list, state, onOpen, onMenu, onClose, { onEditGroup(g) }, scrolled)
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelPage(
    title: String?,
    apps: List<AppEntry>,
    state: LazyListState,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onClose: () -> Unit,
    onTitleHold: () -> Unit,
    onScrolled: () -> Unit,
) {
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { if (it) onScrolled() }
    }
    // Pulling down past the top of the list closes the drawer.
    val pullToClose = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > 260f) { pulled = 0f; close() }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (title != null) {
            Txt(
                title, color = Palette.Muted, size = 14.sp, weight = FontWeight.Medium, align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().quietClick(onLongClick = onTitleHold) {}.padding(top = 14.dp, bottom = 4.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            WheelList(apps, state, Modifier.fillMaxSize().nestedScroll(pullToClose), onOpen, onMenu)
            if (apps.isEmpty() && title != null) {
                Txt("Empty. Hold the title to add apps.", color = Palette.Muted, size = 15.sp)
            }
        }
    }
}

/** Prefix matches first, then word-start matches, then anywhere; also matches initials ("gm" → Google Maps). */
fun filterApps(apps: List<AppEntry>, query: String): List<AppEntry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return apps
    fun rank(a: AppEntry): Int {
        val l = a.label.lowercase()
        val words = l.split(' ', '-', '.', '_').filter { it.isNotEmpty() }
        return when {
            l.startsWith(q) -> 0
            words.any { it.startsWith(q) } -> 1
            l.contains(q) -> 2
            words.joinToString("") { it.take(1) }.startsWith(q) -> 3
            else -> -1
        }
    }
    return apps.map { it to rank(it) }.filter { it.second >= 0 }.sortedBy { it.second }.map { it.first }
}
