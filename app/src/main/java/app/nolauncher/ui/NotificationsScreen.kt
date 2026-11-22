package app.nolauncher.ui

import android.text.format.DateUtils
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.NotifApp
import kotlinx.coroutines.delay

/** Names only. Who's waiting, how many, how long ago — never what they said. */
@Composable
fun NotificationsScreen(
    hasAccess: Boolean,
    items: List<Pair<String, NotifApp>>,
    onOpen: (NotifApp) -> Unit,
    onClear: (NotifApp) -> Unit,
    onClearAll: () -> Unit,
    onGrantAccess: () -> Unit,
    onAppInfo: () -> Unit,
    onClose: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }

    // When the list can't scroll any further up, keep pulling up to go home.
    val close by rememberUpdatedState(onClose)
    val swipeUpToClose = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f) {
                    pulled += -available.y
                    if (pulled > 160f) { pulled = 0f; close() }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Swipe right or swipe up to go back home.
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        val t = 72.dp.toPx()
                        if (dx > t || dy < -t) onClose()
                    },
                ) { _, d -> dx += d.x; dy += d.y }
            }
            .systemBarsPadding(),
    ) {
        when {
            !hasAccess -> Column(
                Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt("Notifications", size = 34.sp, weight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Txt(
                    "See which apps are waiting — just their names, nothing else. " +
                        "NoLauncher needs notification access to know.",
                    color = Palette.Muted, size = 15.sp, align = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                PrimaryButton("Allow access", Modifier.fillMaxWidth(), onGrantAccess)
                Spacer(Modifier.height(22.dp))
                Txt(
                    "Switch greyed out? Open App info, tap ⋮ in the corner, choose " +
                        "“Allow restricted settings”, then try again.",
                    color = Palette.Faint, size = 13.sp, align = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Txt(
                    "App info", color = Palette.Muted, size = 15.sp, weight = FontWeight.SemiBold,
                    modifier = Modifier.quietClick(onClick = onAppInfo).padding(12.dp),
                )
            }

            items.isEmpty() -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt("All quiet.", size = 30.sp, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Txt("Nothing is waiting for you.", color = Palette.Muted, size = 15.sp)
            }

            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().nestedScroll(swipeUpToClose),
                    contentPadding = PaddingValues(top = 32.dp, bottom = 120.dp),
                ) {
                    item {
                        Txt(
                            "Waiting", size = 34.sp, weight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 28.dp).padding(bottom = 16.dp),
                        )
                    }
                    items(items, key = { (_, n) -> n.packageName + n.user.hashCode() }) { (label, n) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .quietClick(onLongClick = { onClear(n) }) { onOpen(n) }
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Txt(
                                label, size = 26.sp, weight = FontWeight.SemiBold, maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Txt(
                                "${n.count} · " + DateUtils.getRelativeTimeSpanString(
                                    n.latest, now, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
                                ),
                                color = Palette.Muted, size = 14.sp,
                            )
                        }
                    }
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt("Tap to open · hold to clear", color = Palette.Faint, size = 13.sp)
                    Txt(
                        "Clear all", size = 16.sp, weight = FontWeight.SemiBold,
                        modifier = Modifier.quietClick(onClick = onClearAll).padding(8.dp),
                    )
                }
            }
        }
    }
}
