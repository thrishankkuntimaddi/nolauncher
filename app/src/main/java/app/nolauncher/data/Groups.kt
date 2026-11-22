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

/** First-run guesses: sorts installed apps into groups and suggests limits for social/video apps. */
object GroupSeeder {
    private class Rule(val name: String, val packages: Set<String>, val words: List<String>, val categories: Set<Int> = emptySet())

    private val rules = listOf(
        Rule(
            "Finance",
            setOf(
                "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm",
                "com.msf.angelmobile", "in.org.npci.upiapp", "com.sbi.lotusintouch", "com.sbi.SBIFreedomPlus",
                "com.csam.icici.bank.imobile", "com.snapwork.hdfc", "com.axis.mobile", "com.zerodha.kite3",
                "com.nextbillion.groww", "in.upstox.pro", "com.dreamplug.androidapp", "com.mobikwik_new",
                "com.kotak811mobilebankingapp", "com.bankofbaroda.mconnect", "com.infrasofttech.indianbank",
            ),
            listOf("bank", "pay", "upi", "wallet", "money", "finance", "trade", "trading", "invest", "stock",
                "mutual", "credit", "loan", "bhim", "kite", "groww", "demat", "angel", "yono", "cred"),
        ),
        Rule(
            "Social",
            setOf(
                "com.twitter.android", "com.reddit.frontpage", "com.pinterest", "com.discord",
                "com.instagram.android", "com.instagram.barcelona", "com.facebook.katana", "com.snapchat.android",
                "com.linkedin.android", "com.zhiliaoapp.musically", "com.google.android.youtube", "com.quora.android",
                "com.tumblr", "org.joinmastodon.android", "xyz.blueskyweb.app",
            ),
            listOf("twitter", "reddit", "pinterest", "discord", "instagram", "facebook", "snapchat", "linkedin",
                "threads", "youtube", "tiktok", "quora", "bluesky", "mastodon"),
            setOf(ApplicationInfo.CATEGORY_SOCIAL),
        ),
        Rule(
            "Messaging",
            setOf(
                "com.whatsapp", "com.whatsapp.w4b", "com.google.android.apps.messaging",
                "com.samsung.android.messaging", "org.telegram.messenger", "org.thoughtcrime.securesms",
                "com.google.android.gm", "com.facebook.orca", "com.Slack", "com.microsoft.teams",
            ),
            listOf("whatsapp", "messages", "messaging", "telegram", "signal", "gmail", "mail", "slack", "teams",
                "messenger", "sms", "outlook"),
        ),
        Rule(
            "Phone",
            setOf(
                "com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer",
                "com.google.android.contacts", "com.samsung.android.app.contacts", "com.android.contacts",
            ),
            listOf("phone", "dialer", "contacts", "call log"),
        ),
        Rule(
            "Tools",
            emptySet(),
            listOf("clock", "calculator", "calendar", "camera", "files", "file manager", "maps", "notes", "keep",
                "recorder", "weather", "settings", "drive", "translate", "gallery", "photos", "authenticator",
                "scanner", "compass", "flashlight", "docs", "sheets"),
            setOf(ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS),
        ),
    )

    /** Suggested daily minutes. Anything else that lands in Social gets [SOCIAL_DEFAULT]. */
    private val knownLimits = mapOf(
        "com.twitter.android" to 10,
        "com.reddit.frontpage" to 5,
        "com.google.android.youtube" to 10,
        "com.instagram.android" to 10,
        "com.facebook.katana" to 10,
        "com.snapchat.android" to 10,
        "com.zhiliaoapp.musically" to 10,
        "com.pinterest" to 10,
    )
    private const val SOCIAL_DEFAULT = 15

    fun seed(context: Context, apps: List<AppEntry>): Pair<List<AppGroup>, Map<String, Int>> {
        val pm = context.packageManager
        val buckets = rules.associate { it.name to mutableListOf<AppEntry>() }
        apps.forEach { app ->
            val label = app.label.lowercase()
            val category = runCatching { pm.getApplicationInfo(app.packageName, 0).category }.getOrDefault(-1)
            val rule = rules.firstOrNull { app.packageName in it.packages }
                ?: rules.firstOrNull { r -> r.words.any { w -> Regex("\\b" + Regex.escape(w)).containsMatchIn(label) } }
                ?: rules.firstOrNull { category in it.categories }
            rule?.let { buckets.getValue(it.name).add(app) }
        }
        val groups = rules.mapNotNull { r ->
            buckets.getValue(r.name).takeIf { it.isNotEmpty() }?.let { list ->
                AppGroup(AppGroup.newId(), r.name, list.sortedBy { it.label.lowercase() }.map { it.key })
            }
        }
        val social = buckets.getValue("Social").map { it.packageName }.toSet()
        val limits = social.associateWith { knownLimits[it] ?: SOCIAL_DEFAULT } +
            knownLimits.filterKeys { pkg -> apps.any { it.packageName == pkg } }
        return groups to limits
    }
}
