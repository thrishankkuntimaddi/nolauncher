package app.nolauncher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    val t by rememberInfiniteTransition(label = "drift").animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Box(Modifier.fillMaxSize()) {
        // Slow, smoky drifting light.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            listOf(
                Triple(Color(0xFF5C6070), 0.0f, 0.75f),
                Triple(Color(0xFF3A3D4A), 2.1f, 0.9f),
                Triple(Color(0xFF6E6A78), 4.2f, 0.6f),
            ).forEach { (c, phase, r) ->
                val center = Offset(
                    w * (0.5f + 0.32f * cos(t + phase)),
                    h * (0.45f + 0.22f * sin(t * 0.8f + phase)),
                )
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.55f), Color.Transparent), center, w * r),
                    radius = w * r,
                    center = center,
                )
            }
        }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Txt("NoLauncher", size = 42.sp, family = FontFamily.Serif, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Txt(
                "No icons. No badges. Just names.",
                color = Palette.Ink.copy(alpha = 0.6f), size = 16.sp, align = TextAlign.Center,
            )
        }
        PrimaryButton(
            "Get started",
            Modifier.align(Alignment.BottomCenter).systemBarsPadding().fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            onStart,
        )
    }
}

