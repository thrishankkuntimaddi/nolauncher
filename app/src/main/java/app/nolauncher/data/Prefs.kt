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

    var groupsSeeded: Boolean
        get() = sp.getBoolean(K_GROUPS_SEEDED, false)
        set(value) = sp.edit { putBoolean(K_GROUPS_SEEDED, value) }

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

    fun setHidden(key: String, hide: Boolean) {
        val next = if (hide) _hidden.value + key else _hidden.value - key
        _hidden.value = next
        sp.edit { putStringSet(K_HIDDEN, next) }
        if (hide) setPinned(key, false)
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
    }
}
