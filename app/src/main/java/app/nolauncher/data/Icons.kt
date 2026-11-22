package app.nolauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monochrome app icons: the app's own logo, drained of colour.
 * Uses the themed-icon layer when the app ships one (Android 13+); otherwise turns
 * the logo into soft white tones so every icon matches the black UI.
 */
class IconLoader(context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val cache = LruCache<String, ImageBitmap>(160)

    fun peek(app: AppEntry, sizePx: Int): ImageBitmap? = cache.get("${app.key}@$sizePx")

    suspend fun load(app: AppEntry, sizePx: Int): ImageBitmap? {
        val id = "${app.key}@$sizePx"
        cache.get(id)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { render(app, sizePx) }.getOrNull()?.asImageBitmap()?.also { cache.put(id, it) }
        }
    }

    private fun render(app: AppEntry, size: Int): Bitmap? {
        val info = launcherApps.getActivityList(app.packageName, app.user)
            .firstOrNull { it.componentName == app.component } ?: return null
        val icon = info.getIcon(0) ?: return null
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        if (icon is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp with a 72dp visible window: scale so the window fills [size].
            val pad = size / 4
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                icon.monochrome?.mutate()?.let { mono ->
                    mono.setTint(android.graphics.Color.WHITE)
                    mono.setBounds(-pad, -pad, size + pad, size + pad)
                    mono.draw(canvas)
                    return bmp
                }
            }
            val fg: Drawable = icon.foreground ?: return null
            fg.setBounds(-pad, -pad, size + pad, size + pad)
            fg.draw(canvas)
        } else {
            icon.setBounds(0, 0, size, size)
            icon.draw(canvas)
        }
        toWhiteTones(bmp)
        return bmp
    }

    /**
     * Luminance → white with alpha.
     *  - Logos on transparency: brightness becomes opacity (dark-on-light logos are inverted).
     *  - Logos printed on a solid tile (most of the square is opaque): the tile colour is dropped and
     *    only what differs from it — the logo — stays visible.
     */
    private fun toWhiteTones(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val lum = FloatArray(px.size)
        var sum = 0.0
        var n = 0
        for (i in px.indices) {
            val c = px[i]
            if ((c ushr 24) < 24) continue
            val l = (0.299f * ((c shr 16) and 0xff) + 0.587f * ((c shr 8) and 0xff) + 0.114f * (c and 0xff)) / 255f
            lum[i] = l
            sum += l; n++
        }
        if (n == 0) return
        val mean = (sum / n).toFloat()
        val tile = n > px.size * 0.55f

        // Tile background = the most common brightness along the border.
        val bg = if (tile) {
            val hist = IntArray(32)
            fun add(i: Int) { if ((px[i] ushr 24) >= 24) hist[(lum[i] * 31).toInt()]++ }
            for (x in 0 until w) { add(x + (h / 6) * w); add(x + (h - 1 - h / 6) * w) }
            for (y in 0 until h) { add(w / 6 + y * w); add(w - 1 - w / 6 + y * w) }
            (hist.indices.maxBy { hist[it] } + 0.5f) / 32f
        } else 0f
        val invert = !tile && mean < 0.35f

        for (i in px.indices) {
            val a = px[i] ushr 24
            if (a == 0) continue
            val strength = if (tile) {
                ((kotlin.math.abs(lum[i] - bg) - 0.08f) * 3f).coerceIn(0f, 1f)
            } else {
                val l = if (invert) 1f - lum[i] else lum[i]
                0.35f + 0.65f * l
            }
            val outA = (a * strength).toInt().coerceIn(0, 255)
            px[i] = (outA shl 24) or 0xF2F2F2
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }
}
