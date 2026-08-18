package com.example.foldercleaner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File

/**
 * Runs as a foreground service so Android does not kill it. For every folder
 * the user added, sets up a FileObserver that fires when a new entry is
 * created directly inside that folder, and deletes it immediately.
 *
 * Note: FileObserver.CREATE fires for top-level children of the watched
 * directory only (not deeply nested paths). That matches "delete a folder
 * the moment it's created inside X".
 */
class FolderWatcherService : Service() {

    private val observers = mutableListOf<FileObserver>()

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopAllObservers()
        val folders = WatchedFoldersStore(this).getFolders()
        folders.forEach { path -> watchFolder(path) }
        // Restart automatically if the system kills the process.
        return START_STICKY
    }

    private fun watchFolder(path: String) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) {
            Log.w(TAG, "Skipping missing directory: $path")
            return
        }

        @Suppress("DEPRECATION")
        val observer = object : FileObserver(path, CREATE) {
            override fun onEvent(event: Int, relativePath: String?) {
                if (relativePath == null) return
                val target = File(dir, relativePath)
                deleteRecursively(target)
            }
        }
        observer.startWatching()
        observers.add(observer)
        Log.i(TAG, "Watching: $path")
    }

    private fun deleteRecursively(target: File) {
        try {
            if (target.isDirectory) {
                target.listFiles()?.forEach { deleteRecursively(it) }
            }
            val deleted = target.delete()
            Log.i(TAG, "Deleted (${target.isDirectory}) $target -> $deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete $target", e)
        }
    }

    private fun stopAllObservers() {
        observers.forEach { it.stopWatching() }
        observers.clear()
    }

    override fun onDestroy() {
        stopAllObservers()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Folder Watcher", NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Folder Cleaner active")
            .setContentText("Watching folders for new items")
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "FolderWatcherService"
        private const val CHANNEL_ID = "folder_watcher_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
