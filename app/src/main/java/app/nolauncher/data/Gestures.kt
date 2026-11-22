package app.nolauncher.data

enum class Gesture(val prefKey: String, val title: String, val default: String) {
    SwipeDown("g_down", "Swipe down", GestureAction.NOTIFICATIONS),
    SwipeRight("g_right", "Swipe right", GestureAction.UNSET),
    SwipeLeft("g_left", "Swipe left", GestureAction.NOTIFICATIONS),
    DoubleTap("g_double", "Double-tap", GestureAction.NONE),
}

/** Gesture actions are stored as plain strings: "none", "notifications", "search", "apps" or "app:<key>". */
object GestureAction {
    const val UNSET = "unset"          // first use asks which app to open
    const val NONE = "none"
    const val NOTIFICATIONS = "notifications"
    const val SEARCH = "search"
    const val ALL_APPS = "apps"
    const val SHADE = "shade"          // Android's own notification shade (shows full content)
    private const val APP = "app:"

    fun app(key: String) = APP + key
    fun appKey(action: String): String? = action.takeIf { it.startsWith(APP) }?.removePrefix(APP)

    fun describe(action: String, appLabel: (String) -> String?): String = when (action) {
        UNSET, NONE -> "Nothing"
        NOTIFICATIONS -> "Notifications"
        SEARCH -> "Search"
        ALL_APPS -> "All apps"
        SHADE -> "Notification shade"
        else -> appKey(action)?.let(appLabel) ?: "App not installed"
    }
}
