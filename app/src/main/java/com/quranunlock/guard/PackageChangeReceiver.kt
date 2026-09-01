package com.applicreation0.quransafeguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        ProtectedApps.clearClassificationCache()
        BrowserDetector.refresh()
    }
}
