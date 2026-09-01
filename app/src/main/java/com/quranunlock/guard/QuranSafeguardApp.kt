package com.applicreation0.quransafeguard

import android.app.Application

class QuranSafeguardApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val migration = AppMigrations.run(this)
        GuardDiagnostics.log(
            this,
            code = if (migration.succeeded) {
                "APP_MIGRATION_OK"
            } else {
                "APP_MIGRATION_FAILED"
            },
            detail = "schema=" + migration.fromSchema + "->" + migration.toSchema
        )

        // Reminders stay entirely local. This also restores alarms after an app
        // process recreation when user preferences already exist.
        MindfulReminderScheduler.scheduleAll(this)
    }
}
