package app.nolauncher.data

import android.app.NotificationManager
import android.content.Context

/** "Focus" = Android's Do Not Disturb (priority only). Needs the user to grant policy access once. */
class FocusMode(context: Context) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    val hasAccess: Boolean get() = nm.isNotificationPolicyAccessGranted

    val isOn: Boolean
        get() = when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL,
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> false
            else -> true
        }

    fun set(on: Boolean): Boolean = runCatching {
        nm.setInterruptionFilter(
            if (on) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }.isSuccess
}
