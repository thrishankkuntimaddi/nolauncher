package app.nolauncher

import android.annotation.SuppressLint
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.nolauncher.data.AppEntry
import app.nolauncher.data.FocusMode
import app.nolauncher.data.Prefs
import app.nolauncher.ui.AppMenu
import app.nolauncher.ui.DrawerScreen
import app.nolauncher.ui.HiddenAppsScreen
import app.nolauncher.ui.HomeScreen
import app.nolauncher.ui.OnboardingScreen
import app.nolauncher.ui.Palette
import app.nolauncher.ui.PickHomeScreen
import app.nolauncher.ui.RenameDialog
import app.nolauncher.ui.SettingsScreen

private enum class Screen { Onboarding, PickHome, Home, Drawer, Settings, Hidden }

class MainActivity : ComponentActivity() {

    private val app get() = application as NoLauncherApp
    private lateinit var focus: FocusMode

    private var screen by mutableStateOf(Screen.Home)
    private var searchOnOpen by mutableStateOf(false)
    private var focusOn by mutableStateOf(false)
    private var focusAccess by mutableStateOf(false)
    private var isDefault by mutableStateOf(true)

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
        setContent { Root() }
    }

    override fun onResume() {
        super.onResume()
        focusOn = focus.isOn
        focusAccess = focus.hasAccess
        isDefault = checkDefault()
    }

    override fun onStop() {
        super.onStop()
        // Coming back to the phone should always land on the calm home screen.
        if (screen == Screen.Drawer) screen = Screen.Home
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while we're already in front.
        if (screen != Screen.Onboarding && screen != Screen.PickHome) screen = Screen.Home
    }

    @Composable
    private fun Root() {
        val prefs = app.prefs
        val rawApps by app.repo.apps.collectAsState()
        val pinned by prefs.pinned.collectAsState()
        val hidden by prefs.hidden.collectAsState()
        val names by prefs.renames.collectAsState()

        val all = remember(rawApps, names) {
            rawApps.map { a -> names[a.key]?.let { a.copy(label = it) } ?: a }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
        val visible = remember(all, hidden) { all.filter { it.key !in hidden } }
        val homeApps = remember(all, pinned) {
            val byKey = all.associateBy { it.key }
            pinned.mapNotNull { byKey[it] }
        }
        val originalLabels = remember(rawApps) { rawApps.associate { it.key to it.label } }

        var menuFor by remember { mutableStateOf<AppEntry?>(null) }
        var renaming by remember { mutableStateOf<AppEntry?>(null) }

        // Back never leaves the launcher; it just steps toward home.
        BackHandler {
            screen = when (screen) {
                Screen.Hidden -> Screen.Settings
                Screen.PickHome -> if (prefs.onboarded) Screen.Settings else Screen.Onboarding
                else -> if (screen == Screen.Onboarding) Screen.Onboarding else Screen.Home
            }
        }

        val open: (AppEntry) -> Unit = { a ->
            if (!app.repo.launch(a)) toast("Couldn't open ${a.label}")
        }
        val showMenu: (AppEntry) -> Unit = { menuFor = it }

        Box(Modifier.fillMaxSize().background(Palette.Bg)) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (targetState == Screen.Drawer) {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 10 }) togetherWith fadeOut(tween(140))
                    } else {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
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

                    Screen.Home -> HomeScreen(
                        apps = homeApps,
                        focusOn = focusOn,
                        onOpen = open,
                        onMenu = showMenu,
                        onSearch = { searchOnOpen = true; screen = Screen.Drawer },
                        onAllApps = { searchOnOpen = false; screen = Screen.Drawer },
                        onPullShade = ::expandNotificationShade,
                        onFocus = ::toggleFocus,
                        onSettings = { screen = Screen.Settings },
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

                    Screen.Settings -> SettingsScreen(
                        pinnedCount = homeApps.size,
                        hiddenCount = hidden.size,
                        focusAccess = focusAccess,
                        focusOn = focusOn,
                        isDefault = isDefault,
                        onHomeApps = { screen = Screen.PickHome },
                        onHiddenApps = { screen = Screen.Hidden },
                        onFocus = ::toggleFocus,
                        onMakeDefault = ::requestDefaultHome,
                        onSystemSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) },
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

    private fun toggleFocus() {
        if (!focus.hasAccess) {
            toast("Allow access so Focus can silence notifications")
            safeStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        if (focus.set(!focus.isOn)) focusOn = focus.isOn
        toast(if (focusOn) "Focus on" else "Focus off")
    }

    @SuppressLint("WrongConstant")
    private fun expandNotificationShade() {
        runCatching {
            val sbm = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sbm)
        }
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
}
