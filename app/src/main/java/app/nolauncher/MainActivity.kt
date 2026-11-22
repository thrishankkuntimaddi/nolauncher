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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.nolauncher.data.AppEntry
import app.nolauncher.data.FocusMode
import app.nolauncher.data.GroupSeeder
import app.nolauncher.data.Limits
import app.nolauncher.data.NotifApp
import app.nolauncher.data.NotifListener
import app.nolauncher.data.Prefs
import app.nolauncher.service.LimitService
import app.nolauncher.ui.AppChecklist
import app.nolauncher.ui.AppMenu
import app.nolauncher.ui.BlockScreen
import app.nolauncher.ui.BottomBar
import app.nolauncher.ui.DrawerScreen
import app.nolauncher.ui.GroupEditScreen
import app.nolauncher.ui.GroupsScreen
import app.nolauncher.ui.HiddenAppsScreen
import app.nolauncher.ui.HomeScreen
import app.nolauncher.ui.LimitPicker
import app.nolauncher.ui.LimitsScreen
import app.nolauncher.ui.LocalIcons
import app.nolauncher.ui.NotificationsScreen
import app.nolauncher.ui.OnboardingScreen
import app.nolauncher.ui.Palette
import app.nolauncher.ui.PermissionItem
import app.nolauncher.ui.PermissionsScreen
import app.nolauncher.ui.RenameDialog
import app.nolauncher.ui.SettingItem
import app.nolauncher.ui.SettingsScreen
import app.nolauncher.ui.SidePanels
import app.nolauncher.ui.filterApps
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private enum class Screen {
    Onboarding, Setup, Home, Drawer, Notifications, Blocked,
    Settings, Pick, Groups, GroupEdit, Limits, Permissions, Hidden,
}

/** The ordered app lists you can customise. */
private enum class ListKind { Home, Dock, Left, Right }

private const val SETUP_STEPS = 6
private const val LAYOUT_VERSION = 2

class MainActivity : ComponentActivity() {

    private val app get() = application as NoLauncherApp
    private val limits: Limits get() = app.limits
    private lateinit var focus: FocusMode

    private var screen by mutableStateOf(Screen.Home)
    private var setupStep by mutableIntStateOf(0)
    private var pickKind by mutableStateOf(ListKind.Home)
    private var pickReturn by mutableStateOf(Screen.Settings)

    private var query by mutableStateOf("")
    private var drawerCollapsed by mutableStateOf(false)
    private var focusSignal by mutableIntStateOf(0)
    private var homeReset by mutableIntStateOf(0)

    private var focusOn by mutableStateOf(false)
    private var focusAccess by mutableStateOf(false)
    private var isDefault by mutableStateOf(true)
    private var notifAccess by mutableStateOf(false)
    private var limitsEnforced by mutableStateOf(false)

    private var blockedPkg by mutableStateOf<String?>(null)
    private var editingGroup by mutableStateOf<String?>(null)
    private var limitFor by mutableStateOf<AppEntry?>(null)

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
        setContent { CompositionLocalProvider(LocalIcons provides app.icons) { Root() } }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
        if (notifAccess) NotifListener.rebind(this)
        limits.rollover()
    }

    private fun refreshPermissions() {
        focusOn = focus.isOn
        focusAccess = focus.hasAccess
        isDefault = checkDefault()
        notifAccess = NotifListener.hasAccess(this)
        limitsEnforced = LimitService.isEnabled(this)
    }

    override fun onStop() {
        super.onStop()
        // Coming back to the phone should always land on the calm home screen.
        if (screen == Screen.Drawer || screen == Screen.Notifications) screen = Screen.Home
        homeReset++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (handleBlocked(intent)) return
        // Home button pressed while we're already in front.
        if (screen != Screen.Onboarding && screen != Screen.Setup) screen = Screen.Home
        homeReset++
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
        val dockKeys by prefs.dock.flow.collectAsState()
        val leftKeys by prefs.leftPanel.flow.collectAsState()
        val rightKeys by prefs.rightPanel.flow.collectAsState()
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
        fun resolve(keys: List<String>) = keys.mapNotNull { byKey[it] }.filter { it.key !in hidden }
        val homeApps = remember(byKey, pinned, hidden) { resolve(pinned) }
        val dockApps = remember(byKey, dockKeys, hidden) { resolve(dockKeys) }
        val leftApps = remember(byKey, leftKeys, hidden) { resolve(leftKeys) }
        val rightApps = remember(byKey, rightKeys, hidden) { resolve(rightKeys) }
        val groupPages = remember(groups, byKey, hidden) {
            groups.map { g -> g to resolve(g.keys).sortedBy { it.label.lowercase() } }
        }
        val originalLabels = remember(rawApps) { rawApps.associate { it.key to it.label } }
        val notifRows = remember(notifs, all) {
            val byPkg = all.associateBy { it.packageName to it.user }
            notifs.map { n -> (byPkg[n.packageName to n.user]?.label ?: labelFor(n.packageName)) to n }
        }

        // Automatic layout on first run, and once more whenever it's improved.
        LaunchedEffect(rawApps) {
            if (prefs.layoutVersion < LAYOUT_VERSION && rawApps.isNotEmpty()) {
                val ctx = this@MainActivity
                prefs.setGroups(GroupSeeder.groups(ctx, rawApps))
                limits.seed(GroupSeeder.limits(ctx, rawApps))
                if (prefs.dock.flow.value.isEmpty()) prefs.dock.set(GroupSeeder.dock(rawApps))
                if (prefs.leftPanel.flow.value.isEmpty()) prefs.leftPanel.set(GroupSeeder.leftPanel(rawApps))
                if (prefs.rightPanel.flow.value.isEmpty()) prefs.rightPanel.set(GroupSeeder.rightPanel(rawApps))
                prefs.layoutVersion = LAYOUT_VERSION
            }
        }

        var menuFor by remember { mutableStateOf<AppEntry?>(null) }
        var renaming by remember { mutableStateOf<AppEntry?>(null) }

        // ── Side panels: progress -1 (right panel) … 0 (closed) … +1 (left panel)
        val scope = rememberCoroutineScope()
        val panel = remember { Animatable(0f) }
        val panelDrag: (Float) -> Unit = { f ->
            scope.launch {
                val v = panel.value
                val nv = v + f
                panel.snapTo(
                    when {
                        v > 0f -> nv.coerceIn(0f, 1f)
                        v < 0f -> nv.coerceIn(-1f, 0f)
                        else -> nv.coerceIn(-1f, 1f)
                    }
                )
            }
        }
        val panelSettle: () -> Unit = {
            scope.launch {
                val v = panel.value
                panel.animateTo(if (abs(v) > 0.3f) sign(v) else 0f, spring(1f, Spring.StiffnessMediumLow))
            }
        }
        val panelClose: () -> Unit = { scope.launch { panel.animateTo(0f, spring(1f, Spring.StiffnessMediumLow)) } }
        LaunchedEffect(homeReset) { panel.snapTo(0f) }

        // ── Search state is shared by the bar and the drawer.
        val keyboard = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        LaunchedEffect(screen) {
            if (screen != Screen.Drawer) {
                query = ""
                drawerCollapsed = false
                keyboard?.hide()
                focusManager.clearFocus()
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
        val openSettings: () -> Unit = { panelClose(); screen = Screen.Settings }
        fun editList(kind: ListKind, returnTo: Screen) {
            pickKind = kind; pickReturn = returnTo; screen = Screen.Pick
        }

        // Back never leaves the launcher; it just steps toward home.
        BackHandler {
            screen = when (screen) {
                Screen.Hidden, Screen.Groups, Screen.Limits, Screen.Permissions -> Screen.Settings
                Screen.Pick -> pickReturn
                Screen.GroupEdit -> Screen.Groups
                Screen.Setup -> when {
                    setupStep > 0 -> { setupStep--; Screen.Setup }
                    prefs.onboarded -> Screen.Settings
                    else -> Screen.Onboarding
                }
                Screen.Onboarding -> Screen.Onboarding
                else -> Screen.Home
            }
        }
        BackHandler(enabled = panel.value != 0f) { panelClose() }

        // ── Shared lists for the editors.
        fun listFor(kind: ListKind) = when (kind) {
            ListKind.Home -> pinned
            ListKind.Dock -> dockKeys
            ListKind.Left -> leftKeys
            ListKind.Right -> rightKeys
        }
        fun toggle(kind: ListKind, a: AppEntry) {
            val ok = when (kind) {
                ListKind.Home -> prefs.setPinned(a.key, a.key !in pinned)
                ListKind.Dock -> prefs.dock.toggle(a.key)
                ListKind.Left -> prefs.leftPanel.toggle(a.key)
                ListKind.Right -> prefs.rightPanel.toggle(a.key)
            }
            if (!ok) toast("That's the maximum")
        }
        fun maxFor(kind: ListKind) = when (kind) {
            ListKind.Home -> Prefs.MAX_PINNED
            ListKind.Dock -> Prefs.MAX_DOCK
            else -> Prefs.MAX_PANEL
        }
        val listText = mapOf(
            ListKind.Home to ("Home" to "Up to 7 names on your home screen."),
            ListKind.Dock to ("Dock" to "Small icons beside search. Scroll the dock sideways to see them all."),
            ListKind.Left to ("Swipe right" to "These slide in from the left when you swipe right on home."),
            ListKind.Right to ("Swipe left" to "These slide in from the right when you swipe left on home."),
        )
        @Composable
        fun ListEditor(kind: ListKind, step: String?, cta: String, onDone: () -> Unit) {
            val (title, sub) = listText.getValue(kind)
            AppChecklist(
                title = title, subtitle = sub, apps = visible, selected = listFor(kind), max = maxFor(kind),
                showIcons = kind != ListKind.Home, step = step, cta = cta,
                onToggle = { toggle(kind, it) }, onDone = onDone,
            )
        }

        val permissionItems = listOf(
            PermissionItem("Default home", "So the home button always brings you here", isDefault, ::requestDefaultHome),
            PermissionItem("Time limits", "Warns you and closes apps when their time is up", limitsEnforced, ::requestLimitEnforcement),
            PermissionItem("Notification names", "Swipe down to see who's waiting — names only", notifAccess, ::requestNotifAccess),
            PermissionItem("Focus", "The moon in the dock turns on Do Not Disturb", focusAccess) {
                safeStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            },
        )

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
                        setupStep = 0
                        screen = Screen.Setup
                    })

                    Screen.Setup -> {
                        val step = "Step ${setupStep + 1} of $SETUP_STEPS"
                        val next: () -> Unit = {
                            if (setupStep < SETUP_STEPS - 1) setupStep++ else {
                                prefs.onboarded = true
                                screen = Screen.Home
                            }
                        }
                        when (setupStep) {
                            0 -> ListEditor(ListKind.Home, step, "Next", next)
                            1 -> ListEditor(ListKind.Dock, step, "Next", next)
                            2 -> ListEditor(ListKind.Left, step, "Next", next)
                            3 -> ListEditor(ListKind.Right, step, "Next", next)
                            4 -> LimitsScreen(
                                apps = visible.distinctBy { it.packageName }.filterNot { isProtected(it.packageName) },
                                limits = limitMap, pending = pendingMap, usage = usage, enforced = limitsEnforced,
                                onEnable = ::requestLimitEnforcement, onPick = { limitFor = it },
                                step = step, cta = "Next", onCta = next,
                            )
                            else -> PermissionsScreen(permissionItems, step, "Finish", ::openOwnAppInfo, next)
                        }
                    }

                    Screen.Home -> HomeScreen(
                        apps = homeApps,
                        onOpen = open,
                        onMenu = showMenu,
                        onAllApps = { drawerCollapsed = false; screen = Screen.Drawer },
                        onNotifications = { screen = Screen.Notifications },
                        onSettings = openSettings,
                        onPanelDrag = panelDrag,
                        onPanelDragEnd = panelSettle,
                    )

                    Screen.Drawer -> DrawerScreen(
                        apps = visible,
                        groups = groupPages,
                        query = query,
                        onOpen = open,
                        onMenu = showMenu,
                        onClose = { screen = Screen.Home },
                        onScrolled = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            if (query.isEmpty()) drawerCollapsed = true
                        },
                        onEditGroup = { g -> editingGroup = g.id; screen = Screen.GroupEdit },
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
                        listOf(
                            SettingItem("Home apps", "${homeApps.size} of ${Prefs.MAX_PINNED}") { editList(ListKind.Home, Screen.Settings) },
                            SettingItem("Dock", "${dockApps.size} quick apps beside search") { editList(ListKind.Dock, Screen.Settings) },
                            SettingItem("Swipe right panel", "${leftApps.size} apps") { editList(ListKind.Left, Screen.Settings) },
                            SettingItem("Swipe left panel", "${rightApps.size} apps") { editList(ListKind.Right, Screen.Settings) },
                            SettingItem("Groups", "${groups.size} groups in All apps") { screen = Screen.Groups },
                            SettingItem(
                                "Time limits",
                                when {
                                    limitMap.isEmpty() -> "None"
                                    !limitsEnforced -> "${limitMap.size} apps · not enforced yet"
                                    else -> "${limitMap.size} apps"
                                },
                            ) { screen = Screen.Limits },
                            SettingItem("Permissions", "${permissionItems.count { it.on }} of ${permissionItems.size} on") {
                                screen = Screen.Permissions
                            },
                            SettingItem("Hidden apps", if (hidden.isEmpty()) "None" else "${hidden.size} hidden") { screen = Screen.Hidden },
                            SettingItem("Run setup again", "Walk through every step") { setupStep = 0; screen = Screen.Setup },
                            SettingItem("Phone settings", null) { safeStart(Intent(Settings.ACTION_SETTINGS)) },
                        )
                    )

                    Screen.Pick -> ListEditor(pickKind, null, "Done") { screen = pickReturn }

                    Screen.Permissions -> PermissionsScreen(permissionItems, null, "Done", ::openOwnAppInfo) {
                        screen = Screen.Settings
                    }

                    Screen.Groups -> GroupsScreen(
                        groups = groups,
                        onOpen = { g -> editingGroup = g.id; screen = Screen.GroupEdit },
                        onMoveUp = { g -> prefs.moveGroupUp(g.id) },
                        onNew = {
                            val g = prefs.addGroup("New group")
                            editingGroup = g.id
                            screen = Screen.GroupEdit
                        },
                        onResort = {
                            prefs.setGroups(GroupSeeder.groups(this@MainActivity, rawApps))
                            toast("Sorted")
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

            // ── The bar: a dock on home, a search field in All apps (it shrinks back to the dock as you scroll).
            AnimatedVisibility(
                visible = screen == Screen.Home || screen == Screen.Drawer,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(120)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                BottomBar(
                    expanded = screen == Screen.Drawer && !drawerCollapsed,
                    query = query,
                    onQuery = { query = it },
                    focusSignal = focusSignal,
                    onGo = {
                        filterApps(visible, query).firstOrNull()?.let(open)
                            ?: if (query.isNotBlank()) webSearch(query) else Unit
                    },
                    onSearchTap = {
                        panelClose()
                        drawerCollapsed = false
                        screen = Screen.Drawer
                        focusSignal++
                    },
                    dock = dockApps,
                    focusOn = focusOn,
                    onFocus = ::toggleFocus,
                    onOpen = open,
                    onEditDock = { editList(ListKind.Dock, screen) },
                    onSettings = openSettings,
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                )
            }

            if (screen == Screen.Home) {
                SidePanels(
                    progress = panel.value,
                    left = leftApps,
                    right = rightApps,
                    onDrag = panelDrag,
                    onDragEnd = panelSettle,
                    onClose = panelClose,
                    onOpen = open,
                    onMenu = showMenu,
                    onEdit = { leftSide ->
                        panelClose()
                        editList(if (leftSide) ListKind.Left else ListKind.Right, Screen.Home)
                    },
                )
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
                            Limits.Change.Applied -> if (!limitsEnforced) toast("Turn on Time limits in Settings → Permissions")
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
            toast("Already on"); return
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
        if (LimitService.isEnabled(this)) {
            toast("Already on"); return
        }
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
        if (checkDefault()) {
            toast("Already your home app"); return
        }
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
