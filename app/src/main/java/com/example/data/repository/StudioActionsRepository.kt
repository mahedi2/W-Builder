package com.example.data.repository

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Xml
import com.example.widget.WBuilderNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

data class ShortcutPresetInfo(
    val label: String,
    val uri: String
)

data class ExportedActivityInfo(
    val packageName: String,
    val className: String,
    val label: String
)

data class RssFeedSnapshot(
    val sourceUrl: String,
    val title: String,
    val description: String,
    val rawSnippet: String,
    val fetchedAtMillis: Long
)

class StudioActionsRepository(private val context: Context) {

    private val globalsPrefs by lazy {
        context.getSharedPreferences("wbuilder_global_switches", Context.MODE_PRIVATE)
    }

    private val feedPrefs by lazy {
        context.getSharedPreferences("wbuilder_feeds", Context.MODE_PRIVATE)
    }

    private val downloadedImagesDir: File
        get() = File(context.cacheDir, "wbuilder_downloaded_images").apply { mkdirs() }

    private val _globalSwitches = MutableStateFlow(loadGlobalSwitchesMap())
    val globalSwitches: StateFlow<Map<String, Boolean>> = _globalSwitches.asStateFlow()

    private fun loadGlobalSwitchesMap(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        globalsPrefs.all.forEach { (k, v) ->
            if (v is Boolean) result[k] = v
        }
        return result
    }

    fun getGlobalSwitch(name: String): Boolean {
        val key = name.trim().ifBlank { "global_1" }
        return globalsPrefs.getBoolean(key, false)
    }

    fun toggleGlobalSwitch(name: String): Boolean {
        val key = name.trim().ifBlank { "global_1" }
        val next = !globalsPrefs.getBoolean(key, false)
        globalsPrefs.edit().putBoolean(key, next).apply()
        _globalSwitches.value = loadGlobalSwitchesMap()
        return next
    }

    fun getLatestRssSnapshot(): RssFeedSnapshot {
        return RssFeedSnapshot(
            sourceUrl = feedPrefs.getString("source_url", "").orEmpty(),
            title = feedPrefs.getString("title", "Feed Ready").orEmpty(),
            description = feedPrefs.getString("description", "Tap to sync RSS / XML").orEmpty(),
            rawSnippet = feedPrefs.getString("raw_snippet", "").orEmpty(),
            fetchedAtMillis = feedPrefs.getLong("fetched_at", 0L)
        )
    }

    suspend fun forceUpdateRssOrTextXml(urlInput: String): Result<RssFeedSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = urlInput.trim().ifBlank {
                feedPrefs.getString("source_url", "").orEmpty()
                    .ifBlank { "https://hnrss.org/frontpage" }
            }
            val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "WBuilderAndroid/1.0")
            }
            val code = conn.responseCode
            check(code in 200..299) { "HTTP $code from $targetUrl" }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val (parsedTitle, parsedDesc) = parseRssOrText(body)
            val snap = RssFeedSnapshot(
                sourceUrl = targetUrl,
                title = parsedTitle.ifBlank { "Synced ($targetUrl)" },
                description = parsedDesc.ifBlank { body.take(140).replace('\n', ' ').trim() },
                rawSnippet = body.take(400),
                fetchedAtMillis = System.currentTimeMillis()
            )
            feedPrefs.edit()
                .putString("source_url", snap.sourceUrl)
                .putString("title", snap.title)
                .putString("description", snap.description)
                .putString("raw_snippet", snap.rawSnippet)
                .putLong("fetched_at", snap.fetchedAtMillis)
                .apply()
            snap
        }
    }

    private fun parseRssOrText(raw: String): Pair<String, String> {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("<")) {
            val lines = trimmed.lines().filter { it.isNotBlank() }
            return (lines.firstOrNull().orEmpty().take(90)) to (lines.drop(1).joinToString(" ").take(160))
        }
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(trimmed))
            var insideItem = false
            var currentTag = ""
            var title = ""
            var desc = ""
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name.orEmpty().lowercase()
                        if (currentTag == "item" || currentTag == "entry") {
                            insideItem = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val txt = parser.text?.trim().orEmpty()
                        if (txt.isNotBlank()) {
                            if (insideItem) {
                                if (currentTag == "title" && title.isBlank()) title = txt
                                if ((currentTag == "description" || currentTag == "summary") && desc.isBlank()) {
                                    desc = txt.replace(Regex("<[^>]*>"), "").trim()
                                }
                            } else if (currentTag == "title" && title.isBlank()) {
                                title = txt
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.orEmpty().lowercase()
                        if ((name == "item" || name == "entry") && title.isNotBlank()) {
                            break
                        }
                        currentTag = ""
                    }
                }
                parser.next()
            }
            title.take(120) to desc.take(200)
        }.getOrDefault("" to "")
    }

    suspend fun forceUpdateDownloadedImages(urls: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val distinctUrls = urls.map { it.trim() }
                .filter { it.startsWith("http://") || it.startsWith("https://") }
                .distinct()
            if (distinctUrls.isEmpty()) {
                // Touch cache marker so local downloaded assets refresh cleanly
                downloadedImagesDir.listFiles()?.forEach { it.setLastModified(System.currentTimeMillis()) }
                return@runCatching downloadedImagesDir.listFiles()?.size ?: 0
            }
            var updatedCount = 0
            distinctUrls.forEach { imageUrl ->
                runCatching {
                    val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000
                        readTimeout = 8000
                        useCaches = false
                        setRequestProperty("Cache-Control", "no-cache")
                    }
                    if (conn.responseCode in 200..299) {
                        val file = File(downloadedImagesDir, "${imageUrl.hashCode()}.img")
                        conn.inputStream.use { input ->
                            FileOutputStream(file).use { out -> input.copyTo(out) }
                        }
                        updatedCount++
                    }
                    conn.disconnect()
                }
            }
            updatedCount
        }
    }

    fun launchShortcutUri(uriString: String): Boolean {
        val clean = uriString.trim()
        if (clean.isEmpty()) return false
        return runCatching {
            val intent = if (clean.startsWith("intent:", ignoreCase = true) || clean.contains("#Intent")) {
                Intent.parseUri(clean, Intent.URI_INTENT_SCHEME)
            } else if (clean.startsWith("settings://", ignoreCase = true)) {
                val action = clean.removePrefix("settings://")
                Intent(action)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(clean))
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun launchExportedActivity(packageName: String, className: String): Boolean {
        if (packageName.isBlank() || className.isBlank()) return false
        return runCatching {
            val intent = Intent().apply {
                component = ComponentName(packageName.trim(), className.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun adjustStreamVolume(stream: Int, deltaSteps: Int, showUi: Boolean): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return false
        return runCatching {
            val cur = am.getStreamVolume(stream)
            val max = am.getStreamMaxVolume(stream)
            val target = (cur + deltaSteps).coerceIn(0, max)
            val flags = if (showUi) android.media.AudioManager.FLAG_SHOW_UI else 0
            am.setStreamVolume(stream, target, flags)
            true
        }.getOrDefault(false)
    }

    fun openNotificationOrSettings(index: Int): String {
        if (WBuilderNotificationListener.isConnected()) {
            if (WBuilderNotificationListener.openNotificationAt(index)) {
                return "Opened active notification."
            }
        }
        return if (openNotificationAccessOrHistory()) {
            "Opened notification panel / settings."
        } else {
            "No active notification available."
        }
    }

    fun dismissNotification(index: Int): String {
        if (WBuilderNotificationListener.isConnected()) {
            if (WBuilderNotificationListener.dismissNotificationAt(index)) {
                return "Dismissed notification."
            }
            return "No clearable notification at slot ${index + 1}."
        }
        openNotificationAccessOrHistory()
        return "Enable Notification Access for W builder to dismiss notifications."
    }

    fun dismissAllNotifications(): String {
        if (WBuilderNotificationListener.isConnected()) {
            WBuilderNotificationListener.dismissAll()
            return "Dismissed all clearable notifications."
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancelAll()
        return "Cleared studio notifications (enable Notification Access for all apps)."
    }

    private fun openNotificationAccessOrHistory(): Boolean {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
            add(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
        for (intent in candidates) {
            val ok = runCatching {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    fun getBuiltinShortcutPresets(): List<ShortcutPresetInfo> = listOf(
        ShortcutPresetInfo("System Settings", "settings://${Settings.ACTION_SETTINGS}"),
        ShortcutPresetInfo("Wi-Fi Settings", "settings://${Settings.ACTION_WIFI_SETTINGS}"),
        ShortcutPresetInfo("Bluetooth Settings", "settings://${Settings.ACTION_BLUETOOTH_SETTINGS}"),
        ShortcutPresetInfo("Battery Usage", "settings://${Intent.ACTION_POWER_USAGE_SUMMARY}"),
        ShortcutPresetInfo("Display Settings", "settings://${Settings.ACTION_DISPLAY_SETTINGS}"),
        ShortcutPresetInfo("Storage Settings", "settings://${Settings.ACTION_INTERNAL_STORAGE_SETTINGS}"),
        ShortcutPresetInfo("Dialer / Phone", "tel:"),
        ShortcutPresetInfo("Web Search", "https://www.google.com"),
        ShortcutPresetInfo("Notification Access", "settings://${Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS}")
    )

    suspend fun queryExportedActivities(): List<ExportedActivityInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val act = info.activityInfo ?: return@mapNotNull null
                if (!act.exported) return@mapNotNull null
                val label = info.loadLabel(pm).toString().ifBlank { act.name.substringAfterLast('.') }
                ExportedActivityInfo(
                    packageName = act.packageName,
                    className = act.name,
                    label = label
                )
            }
            .distinctBy { "${it.packageName}/${it.className}" }
            .sortedBy { it.label.lowercase() }
    }
}
