package com.kevy.ledger.ui.main

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.kevy.ledger.R
import com.kevy.ledger.databinding.ActivityAboutBinding
import com.kevy.ledger.ui.common.ThemedActivity
import com.kevy.ledger.update.AppUpdateClient
import com.kevy.ledger.update.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AboutActivity : ThemedActivity() {
    private lateinit var binding: ActivityAboutBinding
    private val updateClient by lazy { AppUpdateClient(this) }
    private var pendingUpdateInfo: AppUpdateInfo? = null

    private val unknownAppsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val pending = pendingUpdateInfo ?: return@registerForActivityResult
            if (updateClient.canInstallUnknownApps()) {
                startDownloadAndInstall(pending)
            } else {
                Toast.makeText(this, R.string.about_update_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.title_about)

        binding.textVersionValue.text = buildVersionText()
        binding.textUpdateNotesValue.text = getString(R.string.about_update_notes_content)
        binding.textAboutContent.text = getString(R.string.about_content)
        binding.buttonCheckUpdate.setOnClickListener {
            checkForUpdates()
        }
    }

    private fun buildVersionText(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return getString(R.string.about_version_value, versionName, versionCode)
    }

    private fun checkForUpdates() {
        lifecycleScope.launch {
            setBusy(true, getString(R.string.about_update_checking), false)
            val updateInfo = withContext(Dispatchers.IO) { updateClient.fetchLatestUpdate() }
            if (updateInfo == null) {
                setBusy(false, getString(R.string.about_update_manifest_error), false)
                Toast.makeText(this@AboutActivity, R.string.about_update_manifest_error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (!updateClient.hasNewVersion(updateInfo)) {
                setBusy(false, getString(R.string.about_update_already_latest), false)
                Toast.makeText(this@AboutActivity, R.string.about_update_already_latest, Toast.LENGTH_SHORT).show()
                return@launch
            }

            setBusy(false, null, false)
            pendingUpdateInfo = updateInfo
            showUpdateDialog(updateInfo)
        }
    }

    private fun showUpdateDialog(info: AppUpdateInfo) {
        val body = buildString {
            append(getString(R.string.about_update_dialog_version, info.versionName, info.versionCode))
            if (info.changelog.isNotEmpty()) {
                append("\n\n")
                append(getString(R.string.about_update_dialog_notes))
                info.changelog.forEach {
                    append("\n• ")
                    append(it)
                }
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(info.title.ifBlank { getString(R.string.about_update_found_title, info.versionName) })
            .setMessage(body)
            .setPositiveButton(R.string.about_update_now) { _, _ ->
                ensureInstallPermissionThenUpdate(info)
            }
        if (!info.force) {
            dialog.setNegativeButton(android.R.string.cancel, null)
        }
        dialog.show()
    }

    private fun ensureInstallPermissionThenUpdate(info: AppUpdateInfo) {
        pendingUpdateInfo = info
        if (updateClient.canInstallUnknownApps()) {
            startDownloadAndInstall(info)
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.about_update_permission_title)
            .setMessage(R.string.about_update_permission_message)
            .setPositiveButton(R.string.about_update_open_settings) { _, _ ->
                unknownAppsPermissionLauncher.launch(updateClient.buildUnknownAppSourcesIntent())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startDownloadAndInstall(info: AppUpdateInfo) {
        lifecycleScope.launch {
            setBusy(true, getString(R.string.about_update_downloading, info.versionName), true)
            runCatching {
                withContext(Dispatchers.IO) {
                    updateClient.downloadApk(info) { downloadedBytes, totalBytes ->
                        runOnUiThread {
                            updateDownloadProgress(downloadedBytes, totalBytes)
                        }
                    }
                }
            }.onSuccess { result ->
                setBusy(false, getString(R.string.about_update_installing), false)
                runCatching {
                    startActivity(updateClient.buildInstallIntent(result.file))
                }.onFailure { error ->
                    val message = if (error is ActivityNotFoundException) {
                        getString(R.string.about_update_install_failed)
                    } else {
                        error.message ?: getString(R.string.about_update_install_failed)
                    }
                    binding.textUpdateStatus.text = message
                    binding.textUpdateStatus.isVisible = true
                    Toast.makeText(this@AboutActivity, message, Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                val message = error.message ?: getString(R.string.about_update_download_failed)
                setBusy(false, message, false)
                Toast.makeText(this@AboutActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDownloadProgress(downloadedBytes: Long, totalBytes: Long) {
        binding.textUpdateStatus.isVisible = true
        binding.progressUpdate.isVisible = true
        if (totalBytes > 0) {
            val percent = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
            binding.progressUpdate.isIndeterminate = false
            binding.progressUpdate.progress = percent
            binding.textUpdateStatus.text = getString(
                R.string.about_update_progress,
                percent,
                humanReadableBytes(downloadedBytes),
                humanReadableBytes(totalBytes)
            )
        } else {
            binding.progressUpdate.isIndeterminate = true
            binding.textUpdateStatus.text = getString(
                R.string.about_update_progress_unknown,
                humanReadableBytes(downloadedBytes)
            )
        }
    }

    private fun setBusy(active: Boolean, status: String?, showProgress: Boolean) {
        binding.buttonCheckUpdate.isEnabled = !active
        binding.buttonCheckUpdate.text =
            if (active) getString(R.string.about_update_busy) else getString(R.string.about_check_update)
        binding.textUpdateStatus.text = status.orEmpty()
        binding.textUpdateStatus.visibility = if (status.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.progressUpdate.visibility = if (active && showProgress) View.VISIBLE else View.GONE
        binding.progressUpdate.isIndeterminate = showProgress
        if (!active && !showProgress) {
            binding.progressUpdate.progress = 0
        }
    }

    private fun humanReadableBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }
}
