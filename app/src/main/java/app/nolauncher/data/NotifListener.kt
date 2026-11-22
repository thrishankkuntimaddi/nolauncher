package app.nolauncher.data

import android.annotation.SuppressLint
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One row on the notifications screen: which app, how many, how recent. Never the content. */
data class NotifApp(
    val packageName: String,
    val user: UserHandle,
    val count: Int,
    val latest: Long,
    val keys: List<String>,
)

/**
 * Watches the notification shade so the launcher can list *which apps* are waiting on you.
 * Ongoing notifications (music, downloads, navigation) are ignored.
 */
class NotifListener : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        publish()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        _apps.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        _apps.value = active
            .filter { it.packageName != packageName && !it.isOngoing && it.isClearable }
            .groupBy { it.packageName to it.user }
            .map { (k, list) ->
                val real = list.count { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
                NotifApp(k.first, k.second, real.coerceAtLeast(1), list.maxOf { it.postTime }, list.map { it.key })
            }
            .sortedByDescending { it.latest }
    }

    companion object {
        private val _apps = MutableStateFlow<List<NotifApp>>(emptyList())
        val apps: StateFlow<List<NotifApp>> = _apps.asStateFlow()

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: NotifListener? = null

        fun hasAccess(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

        fun component(context: Context) = ComponentName(context, NotifListener::class.java)

        fun dismiss(keys: List<String>) {
            instance?.let { s -> runCatching { s.cancelNotifications(keys.toTypedArray()) } }
        }

        fun dismissAll() {
            instance?.let { s -> runCatching { s.cancelAllNotifications() } }
        }

        fun rebind(context: Context) {
            runCatching { requestRebind(component(context)) }
        }
    }
}
