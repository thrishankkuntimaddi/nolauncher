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
import app.nolauncher.data.AppEntry
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
    hiddenCount: Int,
    focusAccess: Boolean,
    focusOn: Boolean,
    isDefault: Boolean,
    onHomeApps: () -> Unit,
    onHiddenApps: () -> Unit,
    onFocus: () -> Unit,
    onMakeDefault: () -> Unit,
    onSystemSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
        Header("Settings")
        SettingRow("Home apps", "$pinnedCount of ${Prefs.MAX_PINNED}", onHomeApps)
        SettingRow("Hidden apps", if (hiddenCount == 0) "None" else "$hiddenCount hidden", onHiddenApps)
        SettingRow(
            "Focus",
            when {
                !focusAccess -> "Tap to allow Do Not Disturb access"
                focusOn -> "On — only priority interruptions"
                else -> "Off"
            },
            onFocus,
        )
        if (!isDefault) SettingRow("Make default home", "So the home button brings you here", onMakeDefault)
        SettingRow("Phone settings", null, onSystemSettings)

        Spacer(Modifier.height(36.dp))
        Txt(
            "Swipe up — all apps\nSwipe down — notifications\nLong-press an app — options\nLong-press home — settings",
            color = Palette.Faint,
            size = 14.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
        )
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
                Row(
                    Modifier.fillMaxWidth().quietClick { onToggle(app) }.padding(horizontal = 28.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Txt(
                        app.label, size = 22.sp, weight = FontWeight.SemiBold, maxLines = 1,
                        color = if (idx >= 0) Palette.Ink else Palette.Muted,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .then(
                                if (idx >= 0) Modifier.background(Palette.Ink)
                                else Modifier.border(1.5.dp, Palette.Faint, CircleShape)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (idx >= 0) Txt("${idx + 1}", color = Palette.Bg, size = 13.sp, weight = FontWeight.Bold)
                    }
                }
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
