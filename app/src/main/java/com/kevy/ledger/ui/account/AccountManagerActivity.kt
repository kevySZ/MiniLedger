package com.kevy.ledger.ui.account

import android.app.AlertDialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.ActivityManagerListBinding
import com.kevy.ledger.domain.model.Account
import com.kevy.ledger.ui.common.ManagerRow
import com.kevy.ledger.ui.common.ManagerRowAdapter
import com.kevy.ledger.ui.common.ThemedActivity
import com.kevy.ledger.util.AmountExpressionEvaluator
import com.kevy.ledger.util.MoneyUtils

class AccountManagerActivity : ThemedActivity() {
    private lateinit var binding: ActivityManagerListBinding
    private val repository get() = AppGraph.repository
    private val adapter by lazy { ManagerRowAdapter { showEditDialog(it.id) } }
    private val bookId get() = repository.getSelectedBookId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.title_account_manager)
        binding.textEmpty.text = getString(R.string.empty_accounts)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.fabAdd.setOnClickListener { showEditDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        val bookName = repository.getCurrentBook()?.name ?: ""
        binding.textSubtitle.text = "当前账本：$bookName"
        loadAccounts()
    }

    private fun loadAccounts() {
        val rows = repository.getAccounts(bookId, includeInactive = true).map {
            ManagerRow(
                id = it.id,
                title = it.name,
                subtitle = "${it.type} · ${it.note.ifBlank { "无备注" }}",
                meta = "${MoneyUtils.centsToDisplay(it.currentBalanceCents)} ${if (it.isActive) "· 启用" else "· 停用"}"
            )
        }
        binding.textEmpty.visibility = if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        adapter.submitList(rows)
    }

    private fun showEditDialog(accountId: Long?) {
        val existing = accountId?.let { repository.getAccount(it) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36)
        }
        val nameEdit = EditText(this).apply { hint = "账户名称"; setText(existing?.name.orEmpty()) }
        val typeEdit = EditText(this).apply { hint = "账户类型"; setText(existing?.type ?: "现金") }
        val balanceEdit = EditText(this).apply { hint = "期初余额"; setText(existing?.let { MoneyUtils.centsToPlain(it.initialBalanceCents) }.orEmpty()) }
        val noteEdit = EditText(this).apply { hint = "备注"; setText(existing?.note.orEmpty()) }
        val activeCheck = CheckBox(this).apply { text = "启用账户"; isChecked = existing?.isActive ?: true }
        layout.addView(nameEdit)
        layout.addView(typeEdit)
        layout.addView(balanceEdit)
        layout.addView(noteEdit)
        layout.addView(activeCheck)
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "新增账户" else "编辑账户")
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                runCatching {
                    repository.saveAccount(
                        Account(
                            id = existing?.id ?: 0L,
                            bookId = bookId,
                            name = nameEdit.text.toString().trim(),
                            type = typeEdit.text.toString().trim().ifBlank { "现金" },
                            initialBalanceCents = balanceEdit.text.toString().trim().takeIf { it.isNotBlank() }?.let { AmountExpressionEvaluator.evaluateToCents(it) } ?: 0L,
                            note = noteEdit.text.toString().trim(),
                            isActive = activeCheck.isChecked
                        )
                    )
                }.onSuccess { loadAccounts() }
                    .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
