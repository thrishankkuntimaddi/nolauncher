package app.nolauncher.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny persisted state: which apps live on home, which are hidden, custom names. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("nolauncher", Context.MODE_PRIVATE)
    private val names = context.getSharedPreferences("nolauncher_names", Context.MODE_PRIVATE)

    private val _pinned = MutableStateFlow(
        sp.getString(K_PINNED, "").orEmpty().split('\n').filter { it.isNotBlank() }
    )
    val pinned: StateFlow<List<String>> = _pinned.asStateFlow()

    private val _hidden = MutableStateFlow(sp.getStringSet(K_HIDDEN, emptySet()).orEmpty().toSet())
    val hidden: StateFlow<Set<String>> = _hidden.asStateFlow()

    private val _renames = MutableStateFlow(
        names.all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
    )
    val renames: StateFlow<Map<String, String>> = _renames.asStateFlow()

    private val _groups = MutableStateFlow(AppGroup.listFromJson(sp.getString(K_GROUPS, null)))
    val groups: StateFlow<List<AppGroup>> = _groups.asStateFlow()

    /** Bumped when the automatic layout (groups, dock, panels) is improved, so it re-runs once. */
    var layoutVersion: Int
        get() = sp.getInt(K_LAYOUT_VERSION, if (sp.getBoolean(K_GROUPS_SEEDED, false)) 1 else 0)
        set(value) = sp.edit { putInt(K_LAYOUT_VERSION, value) }

    /** Small monochrome icons in the bottom pill. */
    val dock = KeyList(K_DOCK, MAX_DOCK)
    /** Panel that slides in from the left (swipe right on home). */
    val leftPanel = KeyList(K_LEFT, MAX_PANEL)
    /** Panel that slides in from the right (swipe left on home). */
    val rightPanel = KeyList(K_RIGHT, MAX_PANEL)

    inner class KeyList(private val prefKey: String, val max: Int) {
        private val _flow = MutableStateFlow(
            sp.getString(prefKey, "").orEmpty().split('\n').filter { it.isNotBlank() }
        )
        val flow: StateFlow<List<String>> = _flow.asStateFlow()

        fun set(keys: List<String>) {
            val next = keys.distinct().take(max)
            _flow.value = next
            sp.edit { putString(prefKey, next.joinToString("\n")) }
        }

        /** Returns false if the list is full. */
        fun toggle(key: String): Boolean {
            val cur = _flow.value
            if (key in cur) { set(cur - key); return true }
            if (cur.size >= max) return false
            set(cur + key)
            return true
        }

        fun remove(key: String) { if (key in _flow.value) set(_flow.value - key) }
    }

    fun setGroups(groups: List<AppGroup>) {
        _groups.value = groups
        sp.edit { putString(K_GROUPS, AppGroup.listToJson(groups)) }
    }

    fun updateGroup(id: String, f: (AppGroup) -> AppGroup) =
        setGroups(_groups.value.map { if (it.id == id) f(it) else it })

    fun addGroup(name: String): AppGroup =
        AppGroup(AppGroup.newId(), name, emptyList()).also { setGroups(_groups.value + it) }

    fun deleteGroup(id: String) = setGroups(_groups.value.filterNot { it.id == id })

    fun moveGroupUp(id: String) {
        val l = _groups.value.toMutableList()
        val i = l.indexOfFirst { it.id == id }
        if (i > 0) { l.add(i - 1, l.removeAt(i)); setGroups(l) }
    }

    var onboarded: Boolean
        get() = sp.getBoolean(K_ONBOARDED, false)
        set(value) = sp.edit { putBoolean(K_ONBOARDED, value) }

    /** Returns false if the home screen is already full. */
    fun setPinned(key: String, pin: Boolean): Boolean {
        val cur = _pinned.value
        val next = when {
            pin && key in cur -> cur
            pin && cur.size >= MAX_PINNED -> return false
            pin -> cur + key
            else -> cur - key
        }
        _pinned.value = next
        sp.edit { putString(K_PINNED, next.joinToString("\n")) }
        return true
    }

    /** Same apps, new order (drag to rearrange). */
    fun setPinnedOrder(keys: List<String>) {
        val next = keys.distinct().filter { it in _pinned.value }.take(MAX_PINNED)
        _pinned.value = next
        sp.edit { putString(K_PINNED, next.joinToString("\n")) }
    }

    fun setHidden(key: String, hide: Boolean) {
        val next = if (hide) _hidden.value + key else _hidden.value - key
        _hidden.value = next
        sp.edit { putStringSet(K_HIDDEN, next) }
        if (hide) {
            setPinned(key, false)
            dock.remove(key); leftPanel.remove(key); rightPanel.remove(key)
        }
    }

    fun rename(key: String, name: String?) {
        val clean = name?.trim().orEmpty()
        names.edit { if (clean.isEmpty()) remove(key) else putString(key, clean) }
        _renames.value = if (clean.isEmpty()) _renames.value - key else _renames.value + (key to clean)
    }

    companion object {
        const val MAX_PINNED = 7
        private const val K_PINNED = "pinned"
        private const val K_HIDDEN = "hidden"
        private const val K_ONBOARDED = "onboarded"
        private const val K_GROUPS = "groups"
        private const val K_GROUPS_SEEDED = "groups_seeded"
        private const val K_LAYOUT_VERSION = "layout_version"
        private const val K_DOCK = "dock"
        private const val K_LEFT = "panel_left"
        private const val K_RIGHT = "panel_right"
        const val MAX_DOCK = 12
        const val MAX_PANEL = 10
    }
}
