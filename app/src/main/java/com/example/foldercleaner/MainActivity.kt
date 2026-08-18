package com.example.foldercleaner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foldercleaner.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: WatchedFoldersStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = WatchedFoldersStore(this)

        refreshList()

        binding.btnGrantPermission.setOnClickListener { requestAllFilesPermission() }

        binding.btnAddDownloads.setOnClickListener {
            addFolder(File(Environment.getExternalStorageDirectory(), "Download").absolutePath)
        }
        binding.btnAddScreenshots.setOnClickListener {
            addFolder(File(Environment.getExternalStorageDirectory(), "Pictures/Screenshots").absolutePath)
        }
        binding.btnAddCustom.setOnClickListener {
            val path = binding.editCustomPath.text.toString().trim()
            if (path.isNotEmpty()) addFolder(path)
        }

        binding.btnStartService.setOnClickListener {
            if (!hasAllFilesPermission()) {
                Toast.makeText(this, "Grant 'All files access' first", Toast.LENGTH_LONG).show()
                requestAllFilesPermission()
                return@setOnClickListener
            }
            if (prefs.getFolders().isEmpty()) {
                Toast.makeText(this, "Add at least one folder to watch", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            ContextCompat.startForegroundService(this, Intent(this, FolderWatcherService::class.java))
            Toast.makeText(this, "Watcher started", Toast.LENGTH_SHORT).show()
        }

        binding.btnStopService.setOnClickListener {
            stopService(Intent(this, FolderWatcherService::class.java))
            Toast.makeText(this, "Watcher stopped", Toast.LENGTH_SHORT).show()
        }

        updatePermissionLabel()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionLabel()
    }

    private fun addFolder(path: String) {
        val folders = prefs.getFolders().toMutableSet()
        folders.add(path)
        prefs.saveFolders(folders)
        refreshList()
        // If service is already running, restart it so it picks up the new folder list.
        stopService(Intent(this, FolderWatcherService::class.java))
        if (hasAllFilesPermission()) {
            ContextCompat.startForegroundService(this, Intent(this, FolderWatcherService::class.java))
        }
    }

    private fun refreshList() {
        val folders = prefs.getFolders().toList()
        binding.listFolders.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, folders)
    }

    private fun hasAllFilesPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        }
    }

    private fun updatePermissionLabel() {
        binding.txtPermissionStatus.text = if (hasAllFilesPermission())
            "Permission: granted" else "Permission: NOT granted"
    }
}
