package app.nolauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AppGroup(val id: String, val name: String, val keys: List<String>) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("keys", JSONArray(keys))

    companion object {
        fun newId(): String = UUID.randomUUID().toString().take(8)
        fun listToJson(groups: List<AppGroup>) = JSONArray(groups.map { it.toJson() }).toString()
        fun listFromJson(s: String?): List<AppGroup> = runCatching {
            val arr = JSONArray(s ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val k = o.getJSONArray("keys")
                AppGroup(o.getString("id"), o.getString("name"), (0 until k.length()).map { k.getString(it) })
            }
        }.getOrDefault(emptyList())
    }
}

/** Automatic layout: sorts installed apps into groups, suggests limits, and fills the dock and side panels. */
object GroupSeeder {
    private class Rule(
        val name: String,
        val packages: Set<String>,
        val prefixes: List<String> = emptyList(),
        val words: List<String> = emptyList(),
        val categories: Set<Int> = emptySet(),
    )

    // Order matters twice: first matching rule wins, and groups appear in this order.
    private val rules = listOf(
        Rule(
            "Messaging",
            setOf(
                "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging", "com.android.mms",
                "com.samsung.android.messaging", "org.telegram.messenger", "org.thoughtcrime.securesms",
                "com.google.android.gm", "com.microsoft.office.outlook", "com.facebook.orca", "com.Slack",
                "com.microsoft.teams", "com.google.android.apps.dynamite", "jp.naver.line.android",
            ),
            words = listOf("whatsapp", "messages", "messaging", "telegram", "signal", "gmail", "mail", "outlook",
                "slack", "teams", "messenger", "sms", "chat"),
        ),
        Rule(
            "Phone",
            setOf(
                "com.android.contacts", "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
                "com.google.android.contacts", "com.samsung.android.app.contacts", "com.truecaller",
                "com.google.android.apps.tachyon",
            ),
            words = listOf("phone", "dialer", "contacts", "truecaller", "call"),
        ),
        Rule(
            "AI",
            setOf(
                "com.openai.chatgpt", "com.anthropic.claude", "ai.x.grok", "com.google.android.apps.bard",
                "ai.perplexity.app.android", "com.microsoft.copilot", "com.deepseek.chat",
            ),
            words = listOf("chatgpt", "claude", "grok", "gemini", "copilot", "perplexity", "deepseek"),
        ),
        Rule(
            "Social",
            setOf(
                "com.twitter.android", "com.reddit.frontpage", "com.pinterest", "com.discord", "com.linkedin.android",
                "com.instagram.android", "com.instagram.barcelona", "com.facebook.katana", "com.snapchat.android",
                "com.zhiliaoapp.musically", "com.quora.android", "com.tumblr", "org.joinmastodon.android",
                "xyz.blueskyweb.app", "in.mohalla.sharechat",
            ),
            words = listOf("twitter", "reddit", "pinterest", "discord", "instagram", "facebook", "snapchat", "linkedin",
                "threads", "tiktok", "quora", "bluesky", "mastodon"),
            categories = setOf(ApplicationInfo.CATEGORY_SOCIAL),
        ),
        Rule(
            "Watch & Listen",
            setOf(
                "com.google.android.youtube", "com.google.android.apps.youtube.music", "com.netflix.mediaclient",
                "com.spotify.music", "com.bsbportal.music", "tv.accedo.airtel.wynk", "org.videolan.vlc",
                "com.android.VideoPlayer", "com.android.bbkmusic", "com.utorrent.client", "in.startv.hotstar",
                "com.amazon.avod.thirdpartyclient", "com.jio.media.ondemand", "com.graymatrix.did",
                "com.google.android.apps.youtube.creator", "com.oceanwing.soundcore", "com.jio.jiosaavn",
            ),
            words = listOf("youtube", "netflix", "spotify", "music", "video", "player", "prime video", "hotstar",
                "podcast", "radio", "wynk", "xstream", "saavn", "gaana", "movies", "tv"),
            categories = setOf(ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_AUDIO),
        ),
        Rule(
            "Finance",
            setOf(
                "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm", "com.msf.angelmobile",
                "in.org.npci.upiapp", "com.iexceed.ib.digitalbankingprod", "com.google.android.apps.walletnfcrel",
                "icici.lombard.ghi", "com.sbi.lotusintouch", "com.sbi.SBIFreedomPlus", "com.csam.icici.bank.imobile",
                "com.snapwork.hdfc", "com.axis.mobile", "com.zerodha.kite3", "com.nextbillion.groww", "in.upstox.pro",
                "com.dreamplug.androidapp", "com.mobikwik_new", "com.kotak811mobilebankingapp",
            ),
            words = listOf("bank", "pay", "upi", "wallet", "money", "finance", "trade", "trading", "invest", "stock",
                "mutual", "credit", "loan", "bhim", "kite", "groww", "demat", "angel", "yono", "cred", "insurance",
                "expense", "budget", "lombard"),
        ),
        Rule(
            "Shopping & Food",
            setOf(
                "com.application.zomato", "com.application.zomato.district", "in.swiggy.android",
                "in.swiggy.android.instamart", "com.zeptoconsumerapp", "com.grofers.customerapp",
                "com.flipkart.android", "com.myntra.android", "com.ril.ajio", "in.amazon.mShop.android.shopping",
                "com.bigbasket.mobileapp", "com.meesho.supply", "com.nykaa.app", "com.jio.jiomart",
            ),
            words = listOf("zomato", "swiggy", "zepto", "blinkit", "instamart", "flipkart", "myntra", "ajio", "amazon",
                "shopping", "shop", "store", "mart", "food", "grocery", "district", "meesho", "nykaa"),
        ),
        Rule(
            "Travel",
            setOf(
                "com.google.android.apps.maps", "com.rapido.passenger", "com.app.abhibus", "com.apsrtc.online",
                "com.comuto", "com.whereismytrain.android", "org.cris.aikyam", "com.ttdapp", "com.royalenfield.reprime",
                "com.ubercab", "com.olacabs.customer", "cris.org.in.prs.ima", "com.makemytrip", "com.goibibo",
                "com.redbus.android", "com.ixigo.train.ixitrain",
            ),
            words = listOf("maps", "uber", "ola", "rapido", "bus", "train", "rail", "irctc", "flight", "travel", "trip",
                "metro", "cab", "ride", "royal enfield", "blablacar"),
            categories = setOf(ApplicationInfo.CATEGORY_MAPS),
        ),
        Rule(
            "Work",
            setOf(
                "com.google.android.apps.docs", "com.google.android.apps.docs.editors.docs",
                "com.google.android.apps.docs.editors.sheets", "com.google.android.apps.docs.editors.slides",
                "com.microsoft.office.word", "com.microsoft.office.excel", "com.microsoft.office.powerpoint",
                "com.microsoft.office.officehubrow", "com.adobe.scan.android", "naukriApp.appModules.login",
                "com.duolingo", "com.notion.id", "com.todoist", "com.google.android.keep", "com.camerasideas.instashot",
            ),
            words = listOf("docs", "sheets", "slides", "word", "excel", "powerpoint", "office", "drive", "scan",
                "naukri", "notion", "todoist", "duolingo", "learn", "study", "course", "editor", "studio", "jobs"),
            categories = setOf(ApplicationInfo.CATEGORY_PRODUCTIVITY),
        ),
        Rule(
            "Tools",
            setOf(
                "com.android.camera", "com.android.deskclock", "com.android.filemanager", "com.vivo.calculator",
                "com.vivo.compass", "com.vivo.notes", "com.vivo.soundrecorder", "com.vivo.weather", "com.vivo.gallery",
                "com.google.android.apps.photos", "com.google.android.apps.nbu.files", "com.google.android.calendar",
                "com.android.chrome", "com.vivo.browser", "com.google.android.googlequicksearchbox",
                "com.google.android.calculator", "com.google.android.deskclock", "com.google.android.GoogleCamera",
            ),
            words = listOf("camera", "clock", "calculator", "calendar", "files", "file manager", "notes", "recorder",
                "weather", "compass", "gallery", "photos", "album", "chrome", "browser", "google", "flashlight",
                "translate", "scanner"),
            categories = setOf(ApplicationInfo.CATEGORY_IMAGE),
        ),
        Rule(
            "Utilities",
            setOf(
                "com.android.settings", "com.android.vending", "com.vivo.appstore", "com.bbk.theme", "com.bbk.cloud",
                "com.vivo.easyshare", "com.vivo.imanager", "com.vivo.feedback", "com.vivo.vhome", "com.vivo.exhealth",
                "com.iqoo.website", "com.android.stk", "com.google.android.apps.safetyhub",
                "com.google.android.apps.subscriptions.red", "com.azure.authenticator", "com.digilocker.android",
                "in.gov.uidai.pehchaan", "com.myairtelapp", "com.apspdcl.consumerapp", "com.xiaomi.smarthome",
                "com.amazon.dee.app", "com.android.bbk.lockscreen3", "com.vivo.simplelauncher",
            ),
            words = listOf("settings", "play store", "app store", "theme", "cloud", "share", "manager", "authenticator",
                "digilocker", "aadhaar", "airtel", "jio", "vi ", "electricity", "power", "bill", "home", "alexa",
                "security", "safety", "backup", "sim", "health"),
        ),
        Rule("Games", setOf("com.vivo.game"), words = listOf("game", "games", "chess", "puzzle"),
            categories = setOf(ApplicationInfo.CATEGORY_GAME)),
    )
    private const val OTHER = "Other"

    /** Suggested daily minutes for the usual time sinks. Anything else in Social gets [SOCIAL_DEFAULT]. */
    private val knownLimits = mapOf(
        "com.twitter.android" to 10,
        "com.reddit.frontpage" to 5,
        "com.google.android.youtube" to 10,
        "com.instagram.android" to 10,
        "com.facebook.katana" to 10,
        "com.snapchat.android" to 10,
        "com.zhiliaoapp.musically" to 10,
        "com.pinterest" to 10,
        "com.netflix.mediaclient" to 30,
    )
    private const val SOCIAL_DEFAULT = 15

    private fun matches(label: String, word: String) =
        Regex("(^|[^a-z])" + Regex.escape(word)).containsMatchIn(label)

    fun groupOf(context: Context, app: AppEntry): String {
        val label = app.label.lowercase()
        val category = runCatching {
            context.packageManager.getApplicationInfo(app.packageName, 0).category
        }.getOrDefault(-1)
        return (rules.firstOrNull { app.packageName in it.packages }
            ?: rules.firstOrNull { r -> r.words.any { matches(label, it) } }
            ?: rules.firstOrNull { category in it.categories })?.name ?: OTHER
    }

    fun groups(context: Context, apps: List<AppEntry>): List<AppGroup> {
        val byGroup = apps.groupBy { groupOf(context, it) }
        return (rules.map { it.name } + OTHER).mapNotNull { name ->
            byGroup[name]?.takeIf { it.isNotEmpty() }?.let { list ->
                AppGroup(AppGroup.newId(), name, list.sortedBy { it.label.lowercase() }.map { it.key })
            }
        }
    }

    fun limits(context: Context, apps: List<AppEntry>): Map<String, Int> {
        val installed = apps.map { it.packageName }.toSet()
        val social = apps.filter { groupOf(context, it) == "Social" }.map { it.packageName }
        return social.associateWith { knownLimits[it] ?: SOCIAL_DEFAULT } + knownLimits.filterKeys { it in installed }
    }

    /** Picks the first installed app from each preference list. */
    private fun pick(apps: List<AppEntry>, vararg prefs: List<String>, max: Int): List<String> {
        val byPkg = apps.associateBy { it.packageName }
        return prefs.mapNotNull { options -> options.firstNotNullOfOrNull { byPkg[it] }?.key }.distinct().take(max)
    }

    fun dock(apps: List<AppEntry>) = pick(
        apps,
        listOf("com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera"),
        listOf("com.android.chrome", "com.vivo.browser"),
        listOf("com.google.android.apps.maps"),
        listOf("com.google.android.calendar", "com.bbk.calendar"),
        listOf("com.vivo.calculator", "com.google.android.calculator"),
        listOf("com.vivo.notes", "com.google.android.keep"),
        listOf("com.google.android.apps.photos", "com.vivo.gallery"),
        max = 8,
    )

    /** Swipe right → people. */
    fun leftPanel(apps: List<AppEntry>) = pick(
        apps,
        listOf("com.android.contacts", "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer"),
        listOf("com.whatsapp"),
        listOf("com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging"),
        listOf("com.google.android.gm"),
        listOf("org.telegram.messenger"),
        listOf("com.microsoft.office.outlook"),
        listOf("com.truecaller"),
        max = Prefs.MAX_PANEL,
    )

    /** Swipe left → money. */
    fun rightPanel(apps: List<AppEntry>) = pick(
        apps,
        listOf("com.phonepe.app"),
        listOf("com.google.android.apps.nbu.paisa.user"),
        listOf("net.one97.paytm"),
        listOf("in.org.npci.upiapp"),
        listOf("com.iexceed.ib.digitalbankingprod"),
        listOf("com.msf.angelmobile"),
        listOf("com.google.android.apps.walletnfcrel"),
        max = Prefs.MAX_PANEL,
    )
}
