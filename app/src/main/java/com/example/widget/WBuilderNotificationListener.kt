package com.example.widget

import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrackedNotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val isClearable: Boolean,
    val postTime: Long,
    val contentIntent: PendingIntent?
)

class WBuilderNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        if (instance === this) {
            instance = null
        }
        _notifications.value = emptyList()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        syncActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        syncActiveNotifications()
    }

    private fun syncActiveNotifications() {
        val list = runCatching {
            activeNotifications.orEmpty().map { sbn ->
                val extras = sbn.notification?.extras
                val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
                TrackedNotificationItem(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title.ifBlank { sbn.packageName },
                    text = text,
                    isClearable = sbn.isClearable,
                    postTime = sbn.postTime,
                    contentIntent = sbn.notification?.contentIntent
                )
            }.sortedByDescending { it.postTime }
        }.getOrDefault(emptyList())
        _notifications.value = list
    }

    companion object {
        @Volatile
        private var instance: WBuilderNotificationListener? = null

        private val _notifications = MutableStateFlow<List<TrackedNotificationItem>>(emptyList())
        val notifications: StateFlow<List<TrackedNotificationItem>> = _notifications.asStateFlow()

        fun isConnected(): Boolean = instance != null

        fun openNotificationAt(index: Int): Boolean {
            val list = _notifications.value
            val target = list.getOrNull(index) ?: list.firstOrNull() ?: return false
            val pi = target.contentIntent ?: return false
            return runCatching {
                pi.send()
                true
            }.getOrDefault(false)
        }

        fun dismissNotificationAt(index: Int): Boolean {
            val svc = instance ?: return false
            val clearable = _notifications.value.filter { it.isClearable }
            val target = clearable.getOrNull(index) ?: clearable.firstOrNull() ?: return false
            return runCatching {
                svc.cancelNotification(target.key)
                true
            }.getOrDefault(false)
        }

        fun dismissAll(): Boolean {
            val svc = instance ?: return false
            return runCatching {
                svc.cancelAllNotifications()
                true
            }.getOrDefault(false)
        }
    }
}
