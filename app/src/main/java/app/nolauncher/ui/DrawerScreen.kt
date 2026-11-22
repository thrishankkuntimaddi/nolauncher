package app.nolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import kotlinx.coroutines.delay

@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    autoFocusSearch: Boolean,
    focusOn: Boolean,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onClose: () -> Unit,
    onFocus: () -> Unit,
    onSettings: () -> Unit,
    onWebSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) { filterApps(apps, query) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val close by rememberUpdatedState(onClose)

    LaunchedEffect(query) { listState.scrollToItem(0) }
    LaunchedEffect(Unit) {
        if (autoFocusSearch) {
            delay(120)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    // Scrolling the list puts the keyboard away.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) { keyboard?.hide(); focusManager.clearFocus() }
        }
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

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            WheelList(
                apps = results,
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(pullToClose),
                onOpen = onOpen,
                onMenu = onMenu,
            )
            if (results.isEmpty() && query.isNotBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt("Nothing called “$query”", color = Palette.Muted, size = 15.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    AppLabel("Search the web", 22.sp) { onWebSearch(query) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = BarSpacing,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Surface)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlyphIcon(Glyph.Search, Palette.Muted, 18.dp)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Txt("Search apps", color = Palette.Muted, size = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Ink, fontSize = 15.sp),
                        cursorBrush = SolidColor(Palette.Ink),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Go,
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            results.firstOrNull()?.let(onOpen) ?: if (query.isNotBlank()) onWebSearch(query) else Unit
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                }
            }
            RoundButton(Glyph.Moon, active = focusOn, onClick = onFocus)
            RoundButton(Glyph.Sliders, onClick = onSettings)
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
