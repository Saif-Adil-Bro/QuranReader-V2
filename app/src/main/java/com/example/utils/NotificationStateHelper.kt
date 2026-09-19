package com.example.utils

import android.content.Context
import android.content.SharedPreferences

object NotificationStateHelper {
    private const val PREFS_NAME = "notification_states"
    private const val KEY_READ_NOTIFICATIONS = "read_notifications"
    private const val KEY_HIDDEN_NOTIFICATIONS = "hidden_notifications"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getReadIds(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_READ_NOTIFICATIONS, emptySet())?.toSet() ?: emptySet()
    }

    fun getHiddenIds(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(KEY_HIDDEN_NOTIFICATIONS, emptySet())?.toSet() ?: emptySet()
    }

    fun isNotificationRead(context: Context, id: String): Boolean {
        return getReadIds(context).contains(id)
    }

    fun markAsRead(context: Context, id: String) {
        val prefs = getPrefs(context)
        val readIds = prefs.getStringSet(KEY_READ_NOTIFICATIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        readIds.add(id)
        prefs.edit().putStringSet(KEY_READ_NOTIFICATIONS, readIds).apply()
    }
    
    fun markAllAsRead(context: Context, ids: List<String>) {
        val prefs = getPrefs(context)
        val readIds = prefs.getStringSet(KEY_READ_NOTIFICATIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        readIds.addAll(ids)
        prefs.edit().putStringSet(KEY_READ_NOTIFICATIONS, readIds).apply()
    }

    fun isNotificationHidden(context: Context, id: String): Boolean {
        val prefs = getPrefs(context)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_NOTIFICATIONS, emptySet()) ?: emptySet()
        return hiddenIds.contains(id)
    }

    fun hideNotification(context: Context, id: String) {
        val prefs = getPrefs(context)
        val hiddenIds = prefs.getStringSet(KEY_HIDDEN_NOTIFICATIONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        hiddenIds.add(id)
        prefs.edit().putStringSet(KEY_HIDDEN_NOTIFICATIONS, hiddenIds).apply()
    }

    fun getAppFirstInstallTime(context: Context): Long {
        val syncPrefs = context.getSharedPreferences("posts_sync_prefs", Context.MODE_PRIVATE)
        var t = syncPrefs.getLong("app_first_install_time", 0L)
        if (t == 0L) {
            t = System.currentTimeMillis()
            syncPrefs.edit().putLong("app_first_install_time", t).putLong("last_sync_timestamp", t).apply()
        }
        return t
    }
}

