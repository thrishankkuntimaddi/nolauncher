package app.nolauncher.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import app.nolauncher.MainActivity
import app.nolauncher.NoLauncherApp
import app.nolauncher.data.Limits

/**
 * Watches which app is in front. For apps with a daily limit it counts screen-on time,
 * shows a quiet "1 min left" pill near the end, and sends you home when time is up.
 * It reads only the package name of the foreground window — never screen content.
 */
class LimitService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var limits: Limits
    private lateinit var power: PowerManager
    private lateinit var keyguard: KeyguardManager
    private lateinit var wm: WindowManager

    private var current: String? = null
    private var lastTick = 0L
    private var pill: TextView? = null
    private var nudgedAt5: String? = null
    private var pillHideAt = 0L
    private val activityCache = HashMap<String, Boolean>()
    private var transparent = emptySet<String>()

    private val tick = object : Runnable {
        override fun run() {
            val pkg = current ?: return
            val now = SystemClock.elapsedRealtime()
            val delta = (now - lastTick).coerceIn(0, 2_000)
            lastTick = now
            if (power.isInteractive && !keyguard.isKeyguardLocked) limits.addUsage(pkg, delta)

            val remaining = limits.remainingMs(pkg)
            when {
                remaining == null -> { hidePill(); return }
                remaining <= 0 -> { block(pkg); return }
                remaining <= 60_000 -> showPill("${(remaining + 999) / 1000}s left · ${label(pkg)}")
                remaining <= 5 * 60_000 && nudgedAt5 != pkg -> {
                    nudgedAt5 = pkg
                    showPill("5 min left · ${label(pkg)}")
                    pillHideAt = now + 4_000
                }
                pill != null && pillHideAt in 1..now -> hidePill()
            }
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onServiceConnected() {
        val app = application as NoLauncherApp
        limits = app.limits
        power = getSystemService(PowerManager::class.java)
        keyguard = getSystemService(KeyguardManager::class.java)
        wm = getSystemService(WindowManager::class.java)
        transparent = buildSet {
            add("com.android.systemui")
            getSystemService(InputMethodManager::class.java)?.inputMethodList?.forEach { add(it.packageName) }
        }
        running = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in transparent) return
        // Only real activity switches count; ignore dialogs, toasts and popups.
        if (pkg != packageName && !isActivity(pkg, event.className?.toString())) return
        onForeground(pkg)
    }

    private fun onForeground(pkg: String) {
        if (pkg == current) return
        handler.removeCallbacks(tick)
        hidePill()
        limits.flush()
        current = pkg
        nudgedAt5 = null
        lastTick = SystemClock.elapsedRealtime()
        if (limits.limitFor(pkg) == null) return
        if (limits.isBlocked(pkg)) block(pkg) else handler.post(tick)
    }

    private fun block(pkg: String) {
        handler.removeCallbacks(tick)
        hidePill()
        limits.flush()
        current = null
        performGlobalAction(GLOBAL_ACTION_HOME)
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(MainActivity.EXTRA_BLOCKED, pkg)
            )
        }
    }

    private fun isActivity(pkg: String, cls: String?): Boolean {
        if (cls.isNullOrEmpty()) return false
        return activityCache.getOrPut("$pkg/$cls") {
            runCatching { packageManager.getActivityInfo(ComponentName(pkg, cls), 0) }.isSuccess
        }
    }

    private fun label(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("")

    private fun showPill(text: String) {
        val view = pill ?: TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            val padH = (18 * resources.displayMetrics.density).toInt()
            val padV = (9 * resources.displayMetrics.density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                cornerRadius = 100f * resources.displayMetrics.density
                setColor(Color.argb(235, 20, 20, 22))
            }
        }.also { v ->
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (56 * resources.displayMetrics.density).toInt()
            }
            if (runCatching { wm.addView(v, lp) }.isSuccess) pill = v
        }
        view.text = text
        pillHideAt = 0L
    }

    private fun hidePill() {
        pill?.let { runCatching { wm.removeView(it) } }
        pill = null
        pillHideAt = 0L
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        hidePill()
        if (::limits.isInitialized) limits.flush()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var running = false
            private set

        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, LimitService::class.java)
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            return list.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
