package app.nolauncher.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import kotlin.math.abs

/**
 * An alphabetical list of names that curves away like a drum: rows near the
 * centre are upright and bright, rows near the edges tilt back, shrink and fade.
 */
@Composable
fun WheelList(
    apps: List<AppEntry>,
    state: LazyListState,
    modifier: Modifier = Modifier,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val edge = maxHeight * 0.30f
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = edge, bottom = edge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(apps, key = { _, a -> a.key }) { index, app ->
                AppLabel(
                    text = app.label,
                    size = 28.sp,
                    onLongClick = { onMenu(app) },
                    onClick = { onOpen(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .graphicsLayer {
                            // Read scroll position in the draw phase only → no recomposition while scrolling.
                            val info = state.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@graphicsLayer
                            val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                            val center = info.viewportStartOffset + half
                            val d = ((item.offset + item.size / 2f - center) / half).coerceIn(-1f, 1f)
                            val ad = abs(d)
                            rotationX = -d * 58f
                            val s = 1f - ad * ad * 0.22f
                            scaleX = s
                            scaleY = s
                            alpha = (1f - ad * ad * 0.9f).coerceIn(0f, 1f)
                            translationY = -d * ad * size.height * 0.35f
                            cameraDistance = 14f * density
                        },
                )
            }
        }
    }
}
