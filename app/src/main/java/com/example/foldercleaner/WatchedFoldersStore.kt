package com.example.foldercleaner

import android.content.Context

/**
 * Persists the set of absolute folder paths the user wants auto-cleaned.
 */
class WatchedFoldersStore(context: Context) {

    private val prefs = context.getSharedPreferences("folder_cleaner_prefs", Context.MODE_PRIVATE)

    fun getFolders(): Set<String> {
        return prefs.getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()
    }

    fun saveFolders(folders: Set<String>) {
        prefs.edit().putStringSet(KEY_FOLDERS, folders).apply()
    }

    companion object {
        private const val KEY_FOLDERS = "watched_folders"
    }
}
