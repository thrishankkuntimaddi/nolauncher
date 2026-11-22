package app.nolauncher.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate

/**
 * Daily time budgets per app (by package).
 *
 * Restrictive by design:
 *  - tightening a limit applies immediately,
 *  - loosening or removing one only takes effect tomorrow,
 *  - one 15-minute "study / work" extension per app per day.
 */
class Limits(context: Context) {
    private val sp = context.getSharedPreferences("nolauncher_limits", Context.MODE_PRIVATE)

    private val _limits = MutableStateFlow(readMap(K_LIMITS) { (it as Number).toInt() })
    val limits: StateFlow<Map<String, Int>> = _limits.asStateFlow()

    /** pkg → minutes (0 = remove) that will apply from [pendingDay]. */
    private val _pending = MutableStateFlow(readMap(K_PENDING) { (it as Number).toInt() })
    val pending: StateFlow<Map<String, Int>> = _pending.asStateFlow()

    private val _usage = MutableStateFlow(readMap(K_USAGE) { (it as Number).toLong() })
    val usage: StateFlow<Map<String, Long>> = _usage.asStateFlow()

    private val _extra = MutableStateFlow(readMap(K_EXTRA) { (it as Number).toLong() })

    private var day = sp.getLong(K_DAY, today())
    private var unsaved = 0

    init { rollover() }

    private fun today() = LocalDate.now().toEpochDay()

    @Synchronized
    fun rollover() {
        val t = today()
        if (t == day) return
        day = t
        _usage.value = emptyMap()
        _extra.value = emptyMap()
        if (_pending.value.isNotEmpty()) {
            var m = _limits.value
            _pending.value.forEach { (pkg, min) -> m = if (min <= 0) m - pkg else m + (pkg to min) }
            _limits.value = m
            _pending.value = emptyMap()
        }
        saveAll()
    }

    fun limitFor(pkg: String): Int? { rollover(); return _limits.value[pkg] }
    fun usedMs(pkg: String): Long = _usage.value[pkg] ?: 0L
    fun remainingMs(pkg: String): Long? =
        limitFor(pkg)?.let { it * 60_000L + (_extra.value[pkg] ?: 0L) - usedMs(pkg) }
    fun isBlocked(pkg: String): Boolean = (remainingMs(pkg) ?: 1L) <= 0L
    fun canExtend(pkg: String): Boolean { rollover(); return pkg !in _extra.value }

    @Synchronized
    fun extend(pkg: String): Boolean {
        if (!canExtend(pkg)) return false
        _extra.value = _extra.value + (pkg to EXTENSION_MS)
        saveAll()
        return true
    }

    @Synchronized
    fun addUsage(pkg: String, ms: Long) {
        rollover()
        _usage.value = _usage.value + (pkg to usedMs(pkg) + ms)
        if (++unsaved >= 5) flush()
    }

    @Synchronized
    fun flush() {
        unsaved = 0
        sp.edit { putString(K_USAGE, JSONObject(_usage.value).toString()); putLong(K_DAY, day) }
    }

    enum class Change { Applied, Tomorrow, NoChange }

    @Synchronized
    fun setLimit(pkg: String, minutes: Int?): Change {
        rollover()
        val cur = _limits.value[pkg]
        val next = minutes?.takeIf { it > 0 }
        if (next == cur) {
            if (pkg in _pending.value) { _pending.value = _pending.value - pkg; saveAll() }
            return Change.NoChange
        }
        val tighter = next != null && (cur == null || next < cur)
        return if (tighter) {
            _limits.value = _limits.value + (pkg to next!!)
            _pending.value = _pending.value - pkg
            saveAll()
            Change.Applied
        } else {
            _pending.value = _pending.value + (pkg to (next ?: 0))
            saveAll()
            Change.Tomorrow
        }
    }

    /** First-run defaults; only fills apps that have no limit yet. */
    @Synchronized
    fun seed(defaults: Map<String, Int>) {
        _limits.value = defaults + _limits.value
        saveAll()
    }

    private fun saveAll() = sp.edit {
        unsaved = 0
        putLong(K_DAY, day)
        putString(K_LIMITS, JSONObject(_limits.value).toString())
        putString(K_PENDING, JSONObject(_pending.value).toString())
        putString(K_USAGE, JSONObject(_usage.value).toString())
        putString(K_EXTRA, JSONObject(_extra.value).toString())
    }

    private fun <T> readMap(key: String, conv: (Any) -> T): Map<String, T> = runCatching {
        val o = JSONObject(sp.getString(key, "{}")!!)
        o.keys().asSequence().associateWith { conv(o.get(it)) }
    }.getOrDefault(emptyMap())

    companion object {
        const val EXTENSION_MS = 15 * 60_000L
        val CHOICES = listOf(5, 10, 15, 20, 30, 45, 60, 90)
        private const val K_LIMITS = "limits"
        private const val K_PENDING = "pending"
        private const val K_USAGE = "usage"
        private const val K_EXTRA = "extra"
        private const val K_DAY = "day"
    }
}
