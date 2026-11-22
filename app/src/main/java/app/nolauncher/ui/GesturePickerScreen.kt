package app.nolauncher.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry
import app.nolauncher.data.Gesture
import app.nolauncher.data.GestureAction

@Composable
fun GesturePickerScreen(
    gesture: Gesture,
    current: String,
    apps: List<AppEntry>,
    onPick: (String) -> Unit,
) {
    val actions = listOf(
        GestureAction.NONE to "Nothing",
        GestureAction.NOTIFICATIONS to "Notifications",
        GestureAction.SEARCH to "Search",
        GestureAction.ALL_APPS to "All apps",
        GestureAction.SHADE to "Notification shade",
    )
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(top = 32.dp, bottom = 40.dp),
    ) {
        item {
            Txt(
                "${gesture.title} opens…", size = 34.sp, weight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp).padding(bottom = 16.dp),
            )
        }
        items(actions, key = { it.first }) { (action, label) ->
            Option(label, action == current || (current == GestureAction.UNSET && action == GestureAction.NONE)) {
                onPick(action)
            }
        }
        item {
            Txt(
                "Open an app", color = Palette.Faint, size = 14.sp,
                modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 6.dp),
            )
        }
        items(apps, key = { it.key }) { app ->
            val action = GestureAction.app(app.key)
            Option(app.label, action == current) { onPick(action) }
        }
    }
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Txt(
        label,
        size = 22.sp,
        weight = FontWeight.SemiBold,
        color = if (selected) Palette.Ink else Palette.Muted,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().quietClick(onClick = onClick).padding(horizontal = 28.dp, vertical = 11.dp),
    )
}
