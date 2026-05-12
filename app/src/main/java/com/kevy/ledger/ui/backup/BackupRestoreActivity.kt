package com.kevy.ledger.ui.backup

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.ActivityBackupRestoreBinding
import java.nio.charset.Charset

class BackupRestoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackupRestoreBinding
    private val repository get() = AppGraph.repository
    private var pendingExportContent: String = ""
    private var pendingMimeType: String = "application/json"

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
        uri ?: return@registerForActivityResult
        writeContent(uri, pendingExportContent)
    }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charset.defaultCharset())?.use { it.readText() }.orEmpty()
        if (raw.isBlank()) {
            Toast.makeText(this, "文件内容为空", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.message_restore_confirm)
            .setPositiveButton(R.string.action_restore) { _, _ ->
                runCatching { repository.restoreBackupJson(raw) }
                    .onSuccess {
                        binding.textResult.text = "恢复完成，当前本地数据已被备份文件覆盖。"
                    }
                    .onFailure {
                        Toast.makeText(this, it.message ?: "恢复失败", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.title_backup_restore)

        binding.buttonBackupJson.setOnClickListener {
            pendingExportContent = repository.exportBackupJson()
            pendingMimeType = "application/json"
            createDocumentLauncher.launch("ledger_backup.json")
        }
        binding.buttonExportCsv.setOnClickListener {
            pendingExportContent = repository.exportCsv(repository.getSelectedBookId())
            pendingMimeType = "text/csv"
            createDocumentLauncher.launch("ledger_export.csv")
        }
        binding.buttonRestoreJson.setOnClickListener {
            openDocumentLauncher.launch(arrayOf("application/json", "text/plain"))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.textBookInfo.text = "当前账本：${repository.getCurrentBook()?.name ?: "未选择"}"
    }

    private fun writeContent(uri: Uri, content: String) {
        contentResolver.openOutputStream(uri)?.use {
            it.write(content.toByteArray(Charset.defaultCharset()))
            it.flush()
        }
        binding.textResult.text = "导出完成：${uri.lastPathSegment ?: uri.toString()}"
    }
}
