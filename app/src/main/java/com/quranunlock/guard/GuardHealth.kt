package com.applicreation0.quransafeguard

import android.content.Context

object GuardHealth {
    private const val FILE = "guard_health"
    private const val SERVICE_CONNECTED = "service_connected"
    private const val LAST_HEARTBEAT_WALL = "last_heartbeat_wall"
    private const val LAST_EVENT_WALL = "last_event_wall"
    private const val LAST_PROTECTED_PACKAGE = "last_protected_package"

    fun markConnected(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SERVICE_CONNECTED, true)
            .putLong(LAST_HEARTBEAT_WALL, System.currentTimeMillis())
            .apply()
    }

    fun heartbeat(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_HEARTBEAT_WALL, System.currentTimeMillis())
            .apply()
    }

    fun markDisconnected(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SERVICE_CONNECTED, false)
            .putLong(LAST_HEARTBEAT_WALL, System.currentTimeMillis())
            .apply()
    }

    fun markProtectedEvent(context: Context, packageName: String) {
        if (ProtectedApps.shouldNeverPersist(context, packageName)) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_EVENT_WALL, System.currentTimeMillis())
            .putString(LAST_PROTECTED_PACKAGE, packageName)
            .apply()
    }

    fun serviceLooksAlive(context: Context, maxAgeMs: Long = 90_000L): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(SERVICE_CONNECTED, false)) return false
        val last = prefs.getLong(LAST_HEARTBEAT_WALL, 0L)
        val now = System.currentTimeMillis()
        return last > 0L && now >= last && now - last <= maxAgeMs
    }

    fun lastHeartbeatAgeMs(context: Context): Long? {
        val last = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(LAST_HEARTBEAT_WALL, 0L)
        if (last <= 0L) return null
        val now = System.currentTimeMillis()
        return if (now >= last) now - last else null
    }

    fun lastProtectedEventAgeMs(context: Context): Long? {
        val last = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(LAST_EVENT_WALL, 0L)
        if (last <= 0L) return null
        val now = System.currentTimeMillis()
        return if (now >= last) now - last else null
    }

    fun lastProtectedPackage(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(LAST_PROTECTED_PACKAGE, null)
}
