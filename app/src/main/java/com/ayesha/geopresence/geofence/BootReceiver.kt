package com.ayesha.geopresence.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        // Config exists only while someone is signed in (it is cleared on sign-out).
        val config = CampusConfigStore(context).load() ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                GeofenceManager(context.applicationContext).register(config)
            } finally {
                pending.finish()
            }
        }
    }
}