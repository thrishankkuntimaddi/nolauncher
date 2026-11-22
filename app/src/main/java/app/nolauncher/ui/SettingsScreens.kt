package app.nolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import app.nolauncher.data.AppEntry
import app.nolauncher.data.AppGroup
import app.nolauncher.data.Prefs

@Composable
private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 20.dp)) {
        Txt(title, size = 34.sp, weight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Txt(subtitle, color = Palette.Muted, size = 15.sp)
        }
    }
}

@Composable
private fun SettingRow(title: String, detail: String?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().quietClick(onClick = onClick).padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        Txt(title, size = 22.sp, weight = FontWeight.SemiBold)
        if (!detail.isNullOrEmpty()) Txt(detail, color = Palette.Muted, size = 14.sp)
    }
}

@Composable
fun SettingsScreen(
    pinnedCount: Int,
    groupCount: Int,
    limitCount: Int,
    limitsEnforced: Boolean,
    hiddenCount: Int,
    focusAccess: Boolean,
    focusOn: Boolean,
    notifAccess: Boolean,
    isDefault: Boolean,
    onHomeApps: () -> Unit,
    onGroups: () -> Unit,
    onLimits: () -> Unit,
    onHiddenApps: () -> Unit,
    onFocus: () -> Unit,
    onNotifAccess: () -> Unit,
    onMakeDefault: () -> Unit,
    onSystemSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
        Header("Settings")
        SettingRow("Home apps", "$pinnedCount of ${Prefs.MAX_PINNED}", onHomeApps)
        SettingRow("Groups", if (groupCount == 0) "None — swipe left/right does nothing" else "$groupCount groups · swipe left or right", onGroups)
        SettingRow(
            "Time limits",
            when {
                limitCount == 0 -> "None"
                !limitsEnforced -> "$limitCount apps · not enforced — tap to turn on"
                else -> "$limitCount apps"
            },
            onLimits,
        )
        SettingRow(
            "Notification names",
            if (notifAccess) "On — see who's waiting, never the content" else "Tap to allow notification access",
            onNotifAccess,
        )
        SettingRow(
            "Focus",
            when {
                !focusAccess -> "Tap to allow Do Not Disturb access"
                focusOn -> "On — only priority interruptions"
                else -> "Off"
            },
            onFocus,
        )
        SettingRow("Hidden apps", if (hiddenCount == 0) "None" else "$hiddenCount hidden", onHiddenApps)
        if (!isDefault) SettingRow("Make default home", "So the home button brings you here", onMakeDefault)
        SettingRow("Phone settings", null, onSystemSettings)

        Spacer(Modifier.height(28.dp))
        Txt(
            "Swipe left / right — your groups, round in a circle\nSwipe up — all apps\nSwipe down — who's waiting\n" +
                "Long-press an app — options & time limit\nLong-press empty space — settings / edit group",
            color = Palette.Faint,
            size = 14.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun GroupsScreen(
    groups: List<AppGroup>,
    onOpen: (AppGroup) -> Unit,
    onMoveUp: (AppGroup) -> Unit,
    onNew: () -> Unit,
) {
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            item { Header("Groups", "Swipe left or right on home to visit them. Hold a group to move it earlier.") }
            items(groups, key = { it.id }) { g ->
                Column(
                    Modifier.fillMaxWidth().quietClick(onLongClick = { onMoveUp(g) }) { onOpen(g) }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Txt(g.name, size = 22.sp, weight = FontWeight.SemiBold)
                    Txt("${g.keys.size} apps", color = Palette.Muted, size = 14.sp)
                }
            }
        }
        PrimaryButton(
            "New group",
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            onNew,
        )
    }
}

@Composable
fun GroupEditScreen(
    group: AppGroup,
    apps: List<AppEntry>,
    onRename: (String) -> Unit,
    onToggle: (AppEntry) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    var name by remember(group.id) { mutableStateOf(group.name) }
    // Members first, then everything else, so the group reads at a glance.
    val ordered = remember(apps, group.keys) {
        val inGroup = group.keys.toSet()
        apps.sortedBy { if (it.key in inGroup) 0 else 1 }
    }
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            item {
                Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 20.dp)) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it; if (it.isNotBlank()) onRename(it.trim()) },
                        singleLine = true,
                        textStyle = TextStyle(color = Palette.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(Palette.Ink),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Txt("Tap the name to rename. ${group.keys.size} apps.", color = Palette.Muted, size = 15.sp)
                }
            }
            items(ordered, key = { it.key }) { app ->
                Check(app.label, app.key in group.keys, null) { onToggle(app) }
            }
            item {
                Txt(
                    "Delete group", color = Palette.Muted, size = 16.sp,
                    modifier = Modifier.quietClick(onClick = onDelete).padding(horizontal = 28.dp, vertical = 24.dp),
                )
            }
        }
        PrimaryButton(
            "Done",
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            onDone,
        )
    }
}

@Composable
fun LimitsScreen(
    apps: List<AppEntry>,
    limits: Map<String, Int>,
    pending: Map<String, Int>,
    usage: Map<String, Long>,
    enforced: Boolean,
    onEnable: () -> Unit,
    onPick: (AppEntry) -> Unit,
) {
    val limited = apps.filter { it.packageName in limits }
    val rest = apps.filter { it.packageName !in limits }
    LazyColumn(Modifier.fillMaxSize().systemBarsPadding(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item { Header("Time limits", "Daily minutes per app. When time's up, the app closes and you come home.") }
        if (!enforced) {
            item {
                Column(
                    Modifier.fillMaxWidth().quietClick(onClick = onEnable).padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Txt("Turn on enforcement", size = 22.sp, weight = FontWeight.SemiBold)
                    Txt(
                        "Accessibility → NoLauncher time limits → On. It only sees which app is open. " +
                            "Greyed out? App info → ⋮ → Allow restricted settings.",
                        color = Palette.Muted, size = 14.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        items(limited, key = { "l" + it.key }) { app ->
            val used = ((usage[app.packageName] ?: 0L) / 60_000).toInt()
            val p = pending[app.packageName]
            val detail = "$used / ${limits[app.packageName]} min today" +
                (p?.let { if (it <= 0) " · no limit from tomorrow" else " · $it min from tomorrow" } ?: "")
            Column(Modifier.fillMaxWidth().quietClick { onPick(app) }.padding(horizontal = 28.dp, vertical = 10.dp)) {
                Txt(app.label, size = 22.sp, weight = FontWeight.SemiBold, maxLines = 1)
                Txt(detail, color = Palette.Muted, size = 14.sp)
            }
        }
        if (rest.isNotEmpty()) {
            item {
                Txt(
                    "No limit", color = Palette.Faint, size = 14.sp,
                    modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 6.dp),
                )
            }
        }
        items(rest, key = { "r" + it.key }) { app ->
            Txt(
                app.label, size = 20.sp, color = Palette.Muted, maxLines = 1,
                modifier = Modifier.fillMaxWidth().quietClick { onPick(app) }.padding(horizontal = 28.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun Check(label: String, on: Boolean, number: Int?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().quietClick(onClick = onClick).padding(horizontal = 28.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Txt(
            label, size = 22.sp, weight = FontWeight.SemiBold, maxLines = 1,
            color = if (on) Palette.Ink else Palette.Muted,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .then(if (on) Modifier.background(Palette.Ink) else Modifier.border(1.5.dp, Palette.Faint, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (on && number != null) Txt("$number", color = Palette.Bg, size = 13.sp, weight = FontWeight.Bold)
        }
    }
}

@Composable
fun PickHomeScreen(
    apps: List<AppEntry>,
    pinned: List<String>,
    onToggle: (AppEntry) -> Unit,
    onDone: () -> Unit,
) {
    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            item {
                Header(
                    "Your essentials",
                    "Choose up to ${Prefs.MAX_PINNED}. Everything else is one swipe away.  ${pinned.size}/${Prefs.MAX_PINNED}",
                )
            }
            items(apps, key = { it.key }) { app ->
                val idx = pinned.indexOf(app.key)
                Check(app.label, idx >= 0, idx + 1) { onToggle(app) }
            }
        }
        PrimaryButton(
            "Done",
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            onDone,
        )
    }
}

@Composable
fun HiddenAppsScreen(apps: List<AppEntry>, onUnhide: (AppEntry) -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Header("Hidden apps", if (apps.isEmpty()) "Nothing hidden." else "Tap a name to bring it back.")
        LazyColumn {
            items(apps, key = { it.key }) { app ->
                Txt(
                    app.label, size = 22.sp, weight = FontWeight.SemiBold, maxLines = 1,
                    modifier = Modifier.fillMaxWidth().quietClick { onUnhide(app) }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                )
            }
        }
    }
}
