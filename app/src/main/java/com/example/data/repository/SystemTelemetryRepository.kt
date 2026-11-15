package com.example.data.repository

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val startTimeMillis: Long,
    val formattedTime: String
)

data class LaunchableAppInfo(
    val packageName: String,
    val label: String
)

data class DeviceTelemetrySnapshot(
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val ramUsedPercent: Int = 50,
    val storageUsedPercent: Int = 45,
    val networkLabel: String = "Wi-Fi",
    val isNetworkConnected: Boolean = true,
    val nextAlarmText: String? = null,
    val calendarPermissionGranted: Boolean = false,
    val upcomingEvents: List<CalendarEventItem> = emptyList(),
    val isMusicPlaying: Boolean = false,
    val mediaTitle: String = "Media Ready",
    val mediaArtist: String = "Tap play to resume",
    val mediaVolumePercent: Int = 60,
    val isMediaMuted: Boolean = false,
    val ringerModeLabel: String = "Normal",
    val globalSwitches: Map<String, Boolean> = emptyMap(),
    val rssTitle: String = "",
    val rssDescription: String = "",
    val notifCount: Int = 0,
    val notifTitle: String = "",
    val notifText: String = ""
)

class SystemTelemetryRepository(private val context: Context) {

    companion object {
        @Volatile
        var isTorchEnabled: Boolean = false
    }

    fun toggleFlashlight(): Boolean {
        runCatching {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return isTorchEnabled
            val flashCameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return isTorchEnabled
            val next = !isTorchEnabled
            cm.setTorchMode(flashCameraId, next)
            isTorchEnabled = next
        }
        return isTorchEnabled
    }

    fun dispatchMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val now = SystemClock.uptimeMillis()
        runCatching {
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now + 10L, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    fun adjustMediaVolume(direction: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    fun toggleMediaMute(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_TOGGLE_MUTE,
                AudioManager.FLAG_SHOW_UI
            )
        }
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager.isStreamMute(AudioManager.STREAM_MUSIC))
    }

    fun toggleSilentVibrate(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Normal"
        val current = audioManager.ringerMode
        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        val applied = runCatching {
            audioManager.ringerMode = next
            audioManager.ringerMode
        }.getOrElse {
            // If DND policy blocks RINGER_MODE_SILENT, toggle between Vibrate and Normal cleanly
            runCatching {
                val fallback = if (current == AudioManager.RINGER_MODE_VIBRATE) {
                    AudioManager.RINGER_MODE_NORMAL
                } else {
                    AudioManager.RINGER_MODE_VIBRATE
                }
                audioManager.ringerMode = fallback
                fallback
            }.getOrDefault(current)
        }
        return ringerModeToLabel(applied)
    }

    private fun ringerModeToLabel(mode: Int): String = when (mode) {
        AudioManager.RINGER_MODE_SILENT -> "Silent"
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
        else -> "Normal"
    }

    fun openSystemAction(target: String): Boolean {
        val intent = when (target.uppercase()) {
            "WIFI" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            "BLUETOOTH" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "SOUND" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "DISPLAY" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "ALARM" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            "CAMERA" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val primaryOk = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!primaryOk && target.uppercase() == "WIFI") {
            return runCatching {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }.getOrDefault(false)
        }
        return primaryOk
    }

    suspend fun collectSnapshot(): DeviceTelemetrySnapshot = withContext(Dispatchers.IO) {
        val (batteryPct, charging) = readBatteryState()
        val ramPct = readRamUsedPercent()
        val storagePct = readStorageUsedPercent()
        val (netConnected, netLabel) = readNetworkStatus()
        val nextAlarm = readNextAlarmText()
        val hasCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val events = if (hasCalendar) readUpcomingCalendarEvents(limit = 5) else emptyList()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val isPlaying = audioManager?.isMusicActive == true
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 6
        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val volPct = ((curVol * 100f) / maxVol).toInt().coerceIn(0, 100)
        val isMuted = curVol == 0 ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) == true)
        val ringerLabel = ringerModeToLabel(audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL)
        val studioRepo = StudioActionsRepository(context)
        val rssSnap = studioRepo.getLatestRssSnapshot()
        val activeNotifs = com.example.widget.WBuilderNotificationListener.notifications.value

        DeviceTelemetrySnapshot(
            batteryPercent = batteryPct,
            isCharging = charging,
            ramUsedPercent = ramPct,
            storageUsedPercent = storagePct,
            networkLabel = netLabel,
            isNetworkConnected = netConnected,
            nextAlarmText = nextAlarm,
            calendarPermissionGranted = hasCalendar,
            upcomingEvents = events,
            isMusicPlaying = isPlaying,
            mediaTitle = if (isPlaying) "Playing Media" else "Media Ready",
            mediaArtist = if (isPlaying) "Tap controls to pause/skip" else "Tap ▶ to play/resume",
            mediaVolumePercent = volPct,
            isMediaMuted = isMuted,
            ringerModeLabel = ringerLabel,
            globalSwitches = studioRepo.globalSwitches.value,
            rssTitle = rssSnap.title,
            rssDescription = rssSnap.description,
            notifCount = activeNotifs.size,
            notifTitle = activeNotifs.firstOrNull()?.title ?: "No Notifications",
            notifText = activeNotifs.firstOrNull()?.text.orEmpty()
        )
    }

    suspend fun queryLaunchableApps(): List<LaunchableAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(pm).toString().ifBlank { pkg }
                LaunchableAppInfo(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun readBatteryState(): Pair<Int, Boolean> {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        return pct to isCharging
    }

    private fun readRamUsedPercent(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 0
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        if (info.totalMem <= 0L) return 0
        val used = info.totalMem - info.availMem
        return ((used * 100.0) / info.totalMem).toInt().coerceIn(0, 100)
    }

    private fun readStorageUsedPercent(): Int {
        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            if (total <= 0L) 0 else (((total - avail) * 100.0) / total).toInt().coerceIn(0, 100)
        }.getOrDefault(0)
    }

    private fun readNetworkStatus(): Pair<Boolean, String> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false to "Offline"
        val network = cm.activeNetwork ?: return false to "Offline"
        val caps = cm.getNetworkCapabilities(network) ?: return false to "Offline"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true to "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true to "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true to "Ethernet"
            else -> true to "Online"
        }
    }

    private fun readNextAlarmText(): String? {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        val next = alarmManager.nextAlarmClock ?: return null
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next.triggerTime))
    }

    private fun readUpcomingCalendarEvents(limit: Int): List<CalendarEventItem> {
        val now = System.currentTimeMillis()
        val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val args = arrayOf(now.toString(), end.toString())
        val results = mutableListOf<CalendarEventItem>()

        runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT)
                while (cursor.moveToNext() && results.size < limit) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx).orEmpty() else "Event"
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else now
                    results.add(
                        CalendarEventItem(
                            id = id,
                            title = title.ifBlank { "Untitled Event" },
                            startTimeMillis = start,
                            formattedTime = timeFormatter.format(Date(start))
                        )
                    )
                }
            }
        }
        return results
    }
}
