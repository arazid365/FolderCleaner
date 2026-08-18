package com.example.foldercleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val folders = WatchedFoldersStore(context).getFolders()
            if (folders.isNotEmpty()) {
                ContextCompat.startForegroundService(
                    context, Intent(context, FolderWatcherService::class.java)
                )
            }
        }
    }
}
