package app.nolauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWork: Boolean,
)

/** Loads launchable activities for every user profile and keeps them fresh. Labels only — no icons. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(p: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        scope.launch {
            mutex.withLock {
                runCatching { load() }.onSuccess { _apps.value = it }
            }
        }
    }

    private fun load(): List<AppEntry> {
        val self = context.packageName
        val me = Process.myUserHandle()
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .filter { it.applicationInfo.packageName != self }
                .map { info ->
                    AppEntry(
                        key = "${info.componentName.flattenToShortString()}#$serial",
                        label = info.label?.toString()?.trim().orEmpty().ifEmpty { info.componentName.packageName },
                        packageName = info.componentName.packageName,
                        component = info.componentName,
                        user = user,
                        isWork = user != me,
                    )
                }
        }
    }

    fun launch(app: AppEntry): Boolean = runCatching {
        launcherApps.startMainActivity(app.component, app.user, null, null)
    }.isSuccess

    fun openDetails(app: AppEntry): Boolean = runCatching {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }.isSuccess
}
