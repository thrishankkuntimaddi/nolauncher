package app.nolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import app.nolauncher.data.AppEntry
import app.nolauncher.data.IconLoader

val LocalIcons = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

/** The app's own logo, in monochrome. */
@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val loader = LocalIcons.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bmp by produceState(loader.peek(app, px), app.key, px) { value = loader.load(app, px) }
    val b = bmp
    if (b == null) Spacer(modifier.size(size))
    else Image(b, contentDescription = null, modifier = modifier.size(size).graphicsLayer { this.alpha = alpha })
}
