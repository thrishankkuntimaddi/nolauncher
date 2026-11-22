package app.nolauncher

import android.app.SearchManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.nolauncher.data.AppEntry
import app.nolauncher.data.FocusMode
import app.nolauncher.data.GroupSeeder
import app.nolauncher.data.Limits
import app.nolauncher.data.NotifApp
import app.nolauncher.data.NotifListener
import app.nolauncher.data.Prefs
import app.nolauncher.service.LimitService
import app.nolauncher.ui.AppMenu
import app.nolauncher.ui.BlockScreen
import app.nolauncher.ui.DrawerScreen
import app.nolauncher.ui.GroupEditScreen
import app.nolauncher.ui.GroupsScreen
import app.nolauncher.ui.HiddenAppsScreen
import app.nolauncher.ui.HomePager
import app.nolauncher.ui.LimitPicker
import app.nolauncher.ui.LimitsScreen
import app.nolauncher.ui.NotificationsScreen
import app.nolauncher.ui.OnboardingScreen
import app.nolauncher.ui.Palette
import app.nolauncher.ui.PickHomeScreen
import app.nolauncher.ui.RenameDialog
import app.nolauncher.ui.SettingsScreen

private enum class Screen {
    Onboarding, PickHome, Home, Drawer, Notifications, Blocked,
    Settings, Groups, GroupEdit, Limits, Hidden,
}

class MainActivity : ComponentActivity() {

    private val app get() = application as NoLauncherApp
    private val limits: Limits get() = app.limits
    private lateinit var focus: FocusMode

    private var screen by mutableStateOf(Screen.Home)
    private var searchOnOpen by mutableStateOf(false)
    private var focusOn by mutableStateOf(false)
    private var focusAccess by mutableStateOf(false)
    private var isDefault by mutableStateOf(true)
    private var notifAccess by mutableStateOf(false)
    private var limitsEnforced by mutableStateOf(false)
    private var homeReset by mutableIntStateOf(0)
    private var blockedPkg by mutableStateOf<String?>(null)
    private var editingGroup by mutableStateOf<String?>(null)
    private var limitFor by mutableStateOf<AppEntry?>(null)
    private var limitReturn by mutableStateOf<AppEntry?>(null)

    private val roleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { isDefault = checkDefault() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        focus = FocusMode(this)
        screen = if (app.prefs.onboarded) Screen.Home else Screen.Onboarding
        handleBlocked(intent)
        setContent { Root() }
    }

    override fun onResume() {
        super.onResume()
        focusOn = focus.isOn
        focusAccess = focus.hasAccess
        isDefault = checkDefault()
        notifAccess = NotifListener.hasAccess(this)
        if (notifAccess) NotifListener.rebind(this)
        limitsEnforced = LimitService.isEnabled(this)
        limits.rollover()
    }

    override fun onStop() {
        super.onStop()
        // Coming back to the phone should always land on the calm home screen.
        if (screen == Screen.Drawer || screen == Screen.Notifications) screen = Screen.Home
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (handleBlocked(intent)) return
        // Home button pressed while we're already in front.
        if (screen != Screen.Onboarding && screen != Screen.PickHome) {
            screen = Screen.Home
            homeReset++
        }
    }

    private fun handleBlocked(intent: Intent?): Boolean {
        val pkg = intent?.getStringExtra(EXTRA_BLOCKED) ?: return false
        intent.removeExtra(EXTRA_BLOCKED)
        if (!app.prefs.onboarded) return false
        blockedPkg = pkg
        screen = Screen.Blocked
        return true
    }

    @Composable
    private fun Root() {
        val prefs = app.prefs
        val rawApps by app.repo.apps.collectAsState()
        val pinned by prefs.pinned.collectAsState()
        val hidden by prefs.hidden.collectAsState()
        val names by prefs.renames.collectAsState()
        val groups by prefs.groups.collectAsState()
        val notifs by NotifListener.apps.collectAsState()
        val limitMap by limits.limits.collectAsState()
        val pendingMap by limits.pending.collectAsState()
        val usage by limits.usage.collectAsState()

        val all = remember(rawApps, names) {
            rawApps.map { a -> names[a.key]?.let { a.copy(label = it) } ?: a }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
        val visible = remember(all, hidden) { all.filter { it.key !in hidden } }
        val byKey = remember(all) { all.associateBy { it.key } }
        val homeApps = remember(byKey, pinned) { pinned.mapNotNull { byKey[it] } }
        val groupPages = remember(groups, byKey, hidden) {
            groups.map { g ->
                g to g.keys.filter { it !in hidden }.mapNotNull { byKey[it] }.sortedBy { it.label.lowercase() }
            }
        }
        val originalLabels = remember(rawApps) { rawApps.associate { it.key to it.label } }
        val notifRows = remember(notifs, all) {
            val byPkg = all.associateBy { it.packageName to it.user }
            notifs.map { n -> (byPkg[n.packageName to n.user]?.label ?: labelFor(n.packageName)) to n }
        }

        // First run (or upgrade): sort apps into groups and suggest limits for social apps.
        LaunchedEffect(rawApps) {
            if (!prefs.groupsSeeded && rawApps.isNotEmpty()) {
                val (seedGroups, seedLimits) = GroupSeeder.seed(this@MainActivity, rawApps)
                if (prefs.groups.value.isEmpty()) prefs.setGroups(seedGroups)
                limits.seed(seedLimits)
                prefs.groupsSeeded = true
            }
        }

        var menuFor by remember { mutableStateOf<AppEntry?>(null) }
        var renaming by remember { mutableStateOf<AppEntry?>(null) }

        // Back never leaves the launcher; it just steps toward home.
        BackHandler {
            screen = when (screen) {
                Screen.Hidden, Screen.Groups, Screen.Limits -> Screen.Settings
                Screen.GroupEdit -> Screen.Groups
                Screen.PickHome -> if (prefs.onboarded) Screen.Settings else Screen.Onboarding
                Screen.Onboarding -> Screen.Onboarding
                else -> Screen.Home
            }
        }

        val open: (AppEntry) -> Unit = { a ->
            if (limits.isBlocked(a.packageName)) {
                blockedPkg = a.packageName
                screen = Screen.Blocked
            } else if (!app.repo.launch(a)) {
                toast("Couldn't open ${a.label}")
            }
        }
        val showMenu: (AppEntry) -> Unit = { menuFor = it }

        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    when {
                        targetState == Screen.Notifications ->
                            (slideInVertically(tween(260)) { -it / 8 } + fadeIn(tween(220))) togetherWith fadeOut(tween(140))
                        initialState == Screen.Notifications ->
                            fadeIn(tween(220)) togetherWith (slideOutVertically(tween(200)) { -it / 8 } + fadeOut(tween(160)))
                        targetState == Screen.Drawer ->
                            (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 10 }) togetherWith fadeOut(tween(140))
                        else -> fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    }
                },
                label = "screen",
            ) { s ->
                when (s) {
                    Screen.Onboarding -> OnboardingScreen(onStart = {
                        if (!checkDefault()) requestDefaultHome()
                        screen = Screen.PickHome
                    })

                    Screen.PickHome -> PickHomeScreen(
                        apps = visible,
                        pinned = pinned,
                        onToggle = { a ->
                            val isPinned = a.key in pinned
                            if (!prefs.setPinned(a.key, !isPinned)) toast("Up to ${Prefs.MAX_PINNED} apps")
                        },
                        onDone = {
                            val wasOnboarded = prefs.onboarded
                            prefs.onboarded = true
                            screen = if (wasOnboarded) Screen.Settings else Screen.Home
                        },
                    )

                    Screen.Home -> HomePager(
                        home = homeApps,
                        groups = groupPages,
                        focusOn = focusOn,
                        resetSignal = homeReset,
                        onOpen = open,
                        onMenu = showMenu,
                        onSearch = { searchOnOpen = true; screen = Screen.Drawer },
                        onAllApps = { searchOnOpen = false; screen = Screen.Drawer },
                        onNotifications = { screen = Screen.Notifications },
                        onFocus = ::toggleFocus,
                        onSettings = { screen = Screen.Settings },
                        onEditGroup = { g -> editingGroup = g.id; screen = Screen.GroupEdit },
                    )

                    Screen.Drawer -> DrawerScreen(
                        apps = visible,
                        autoFocusSearch = searchOnOpen,
                        focusOn = focusOn,
                        onOpen = open,
                        onMenu = showMenu,
                        onClose = { screen = Screen.Home },
                        onFocus = ::toggleFocus,
                        onSettings = { screen = Screen.Settings },
                        onWebSearch = ::webSearch,
                    )

                    Screen.Notifications -> NotificationsScreen(
                        hasAccess = notifAccess,
                        items = notifRows,
                        onOpen = ::openFromNotification,
                        onClear = { NotifListener.dismiss(it.keys) },
                        onClearAll = { NotifListener.dismissAll() },
                        onGrantAccess = ::requestNotifAccess,
                        onAppInfo = ::openOwnAppInfo,
                        onClose = { screen = Screen.Home },
                    )

                    Screen.Blocked -> {
                        val pkg = blockedPkg.orEmpty()
                        BlockScreen(
                            label = all.firstOrNull { it.packageName == pkg }?.label ?: labelFor(pkg),
                            usedMinutes = ((usage[pkg] ?: 0L) / 60_000).toInt(),
                            limitMinutes = limitMap[pkg] ?: 0,
                            canExtend = limits.canExtend(pkg),
                            onHome = { screen = Screen.Home; homeReset++ },
                            onExtend = {
                                limits.extend(pkg)
                                screen = Screen.Home
                                all.firstOrNull { it.packageName == pkg }?.let { app.repo.launch(it) }
                                    ?: packageManager.getLaunchIntentForPackage(pkg)?.let { safeStart(it) }
                            },
                        )
                    }

                    Screen.Settings -> SettingsScreen(
                        pinnedCount = homeApps.size,
                        groupCount = groups.size,
                        limitCount = limitMap.size,
                        limitsEnforced = limitsEnforced,
                        hiddenCount = hidden.size,
                        focusAccess = focusAccess,
                        focusOn = focusOn,
                        notifAccess = notifAccess,
                        isDefault = isDefault,
                        onHomeApps = { screen = Screen.PickHome },
                        onGroups = { screen = Screen.Groups },
                        onLimits = { screen = Screen.Limits },
                        onHiddenApps = { screen = Screen.Hidden },
                        onFocus = ::toggleFocus,
                        onNotifAccess = ::requestNotifAccess,
                        onMakeDefault = ::requestDefaultHome,
                        onSystemSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) },
                    )

                    Screen.Groups -> GroupsScreen(
                        groups = groups,
                        onOpen = { g -> editingGroup = g.id; screen = Screen.GroupEdit },
                        onMoveUp = { g -> prefs.moveGroupUp(g.id) },
                        onNew = {
                            val g = prefs.addGroup("New group")
                            editingGroup = g.id
                            screen = Screen.GroupEdit
                        },
                    )

                    Screen.GroupEdit -> {
                        val g = groups.firstOrNull { it.id == editingGroup }
                        if (g == null) {
                            LaunchedEffect(Unit) { screen = Screen.Groups }
                        } else {
                            GroupEditScreen(
                                group = g,
                                apps = visible,
                                onRename = { n -> prefs.updateGroup(g.id) { it.copy(name = n) } },
                                onToggle = { a ->
                                    prefs.updateGroup(g.id) {
                                        it.copy(keys = if (a.key in it.keys) it.keys - a.key else it.keys + a.key)
                                    }
                                },
                                onDelete = { prefs.deleteGroup(g.id); screen = Screen.Groups },
                                onDone = { screen = Screen.Groups },
                            )
                        }
                    }

                    Screen.Limits -> LimitsScreen(
                        apps = visible.distinctBy { it.packageName }.filterNot { isProtected(it.packageName) },
                        limits = limitMap,
                        pending = pendingMap,
                        usage = usage,
                        enforced = limitsEnforced,
                        onEnable = ::requestLimitEnforcement,
                        onPick = { limitFor = it },
                    )

                    Screen.Hidden -> HiddenAppsScreen(
                        apps = all.filter { it.key in hidden },
                        onUnhide = { prefs.setHidden(it.key, false) },
                    )
                }
            }

            AnimatedVisibility(menuFor != null, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
                // Keep the last value around during the exit animation.
                val a = remember { mutableStateOf(menuFor) }.apply { if (menuFor != null) value = menuFor }.value
                    ?: return@AnimatedVisibility
                AppMenu(
                    app = a,
                    pinned = a.key in pinned,
                    homeFull = pinned.size >= Prefs.MAX_PINNED,
                    onDismiss = { menuFor = null },
                    onTogglePin = { prefs.setPinned(a.key, a.key !in pinned); menuFor = null },
                    onRename = { menuFor = null; renaming = a },
                    onHide = { prefs.setHidden(a.key, true); menuFor = null; toast("${a.label} hidden") },
                    onInfo = { menuFor = null; app.repo.openDetails(a) },
                    onUninstall = { menuFor = null; uninstall(a) },
                    limitText = limitMap[a.packageName]?.let { "$it min" } ?: "none",
                    onLimit = {
                        menuFor = null
                        if (isProtected(a.packageName)) toast("Settings can't be limited") else limitFor = a
                    },
                )
            }

            limitFor?.let { a ->
                LimitPicker(
                    label = a.label,
                    usedMinutes = ((usage[a.packageName] ?: 0L) / 60_000).toInt(),
                    current = limitMap[a.packageName],
                    pending = pendingMap[a.packageName],
                    onDismiss = { limitFor = null },
                    onPick = { m ->
                        when (limits.setLimit(a.packageName, m)) {
                            Limits.Change.Tomorrow -> toast("Starts tomorrow")
                            Limits.Change.Applied -> if (!limitsEnforced) toast("Turn on enforcement in Settings → Time limits")
                            Limits.Change.NoChange -> Unit
                        }
                        limitFor = null
                    },
                )
            }

            renaming?.let { a ->
                RenameDialog(
                    app = a,
                    originalLabel = originalLabels[a.key] ?: a.label,
                    onDismiss = { renaming = null },
                    onSave = { name ->
                        val original = originalLabels[a.key]
                        prefs.rename(a.key, name?.takeIf { it.isNotBlank() && it.trim() != original })
                        renaming = null
                    },
                )
            }
        }
    }

    // ── System integrations ──────────────────────────────────────────────

    /** Never let a limit lock you out of Settings (that's where limits are turned off). */
    private fun isProtected(pkg: String): Boolean =
        pkg == packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName

    private fun labelFor(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun openFromNotification(n: NotifApp) {
        if (limits.isBlocked(n.packageName)) {
            blockedPkg = n.packageName; screen = Screen.Blocked; return
        }
        val entry = app.repo.apps.value.firstOrNull { it.packageName == n.packageName && it.user == n.user }
        val ok = entry?.let { app.repo.launch(it) }
            ?: packageManager.getLaunchIntentForPackage(n.packageName)?.let { safeStart(it, quiet = true) }
            ?: false
        if (!ok) toast("Can't open this app directly")
    }

    private fun requestNotifAccess() {
        if (NotifListener.hasAccess(this)) {
            toast("Already allowed"); return
        }
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, NotifListener.component(this).flattenToString())
        } else null
        if (detail == null || !safeStart(detail, quiet = true)) {
            safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun requestLimitEnforcement() {
        toast("Find “NoLauncher time limits” and turn it on")
        safeStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openOwnAppInfo() {
        safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    private fun toggleFocus() {
        if (!focus.hasAccess) {
            toast("Allow access so Focus can silence notifications")
            safeStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        if (focus.set(!focus.isOn)) focusOn = focus.isOn
        toast(if (focusOn) "Focus on" else "Focus off")
    }

    private fun checkDefault(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == packageName
    }

    private fun requestDefaultHome() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                    .onSuccess { return }
            }
        }
        safeStart(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun uninstall(a: AppEntry) {
        safeStart(
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", a.packageName, null))
                .putExtra(Intent.EXTRA_USER, a.user)
        )
    }

    private fun webSearch(query: String) {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        if (!safeStart(search, quiet = true)) {
            safeStart(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
        }
    }

    private fun safeStart(intent: Intent, quiet: Boolean = false): Boolean = runCatching {
        startActivity(intent)
    }.onFailure { if (!quiet) toast("Not available on this phone") }.isSuccess

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_BLOCKED = "app.nolauncher.BLOCKED"
    }
}
