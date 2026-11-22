package app.nolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Time's up. One way out (home) and one narrow door (a single 15-minute extension per day,
 * only for study or work, after a 10-second pause).
 */
@Composable
fun BlockScreen(
    label: String,
    usedMinutes: Int,
    limitMinutes: Int,
    canExtend: Boolean,
    onHome: () -> Unit,
    onExtend: (reason: String) -> Unit,
) {
    var asking by remember { mutableStateOf(false) }
    var wait by remember { mutableIntStateOf(10) }
    LaunchedEffect(asking) {
        if (asking) { wait = 10; while (wait > 0) { delay(1_000); wait-- } }
    }

    Box(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 32.dp)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Txt(label, size = 36.sp, weight = FontWeight.Bold, align = TextAlign.Center, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            if (!asking) {
                Txt("Time's up for today.", color = Palette.Ink.copy(alpha = 0.8f), size = 18.sp)
                Spacer(Modifier.height(6.dp))
                Txt(
                    "$usedMinutes of $limitMinutes min used · resets at midnight",
                    color = Palette.Muted, size = 14.sp, align = TextAlign.Center,
                )
            } else if (wait > 0) {
                Txt("Take a breath.", color = Palette.Ink.copy(alpha = 0.8f), size = 18.sp)
                Spacer(Modifier.height(6.dp))
                Txt("Is this really for study or work?  $wait", color = Palette.Muted, size = 14.sp)
            } else {
                Txt("What is it for?", color = Palette.Ink.copy(alpha = 0.8f), size = 18.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Study", "Work").forEach { r ->
                        Txt(
                            r, size = 24.sp, weight = FontWeight.SemiBold,
                            modifier = Modifier.quietClick { onExtend(r) }.padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Txt("15 minutes, once today.", color = Palette.Faint, size = 13.sp)
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!asking) {
                if (canExtend) {
                    Txt(
                        "I need it for study or work",
                        color = Palette.Muted, size = 14.sp,
                        modifier = Modifier.quietClick { asking = true }.padding(12.dp),
                    )
                } else {
                    Txt("Extension already used today.", color = Palette.Faint, size = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }
            PrimaryButton("Go home", Modifier.fillMaxWidth(), onHome)
        }
    }
}
