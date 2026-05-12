package com.kevy.ledger.ui.category

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.ActivityManagerListBinding
import com.kevy.ledger.domain.model.Category
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.ui.common.ManagerRow
import com.kevy.ledger.ui.common.ManagerRowAdapter

class CategoryManagerActivity : AppCompatActivity() {
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
        binding.toolbar.title = getString(R.string.title_category_manager)
        binding.textEmpty.text = getString(R.string.empty_categories)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.fabAdd.setOnClickListener { showEditDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        val bookName = repository.getCurrentBook()?.name ?: ""
        binding.textSubtitle.text = "当前账本：$bookName"
        loadCategories()
    }

    private fun loadCategories() {
        val rows = repository.getCategories(bookId, includeInactive = true).map {
            ManagerRow(
                id = it.id,
                title = it.name,
                subtitle = if (it.type == CategoryType.EXPENSE) "支出分类" else "收入分类",
                meta = "${it.colorHex} ${if (it.isActive) "· 启用" else "· 停用"}"
            )
        }
        binding.textEmpty.visibility = if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        adapter.submitList(rows)
    }

    private fun showEditDialog(categoryId: Long?) {
        val existing = categoryId?.let { repository.getCategory(it) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36)
        }
        val nameEdit = EditText(this).apply { hint = "分类名称"; setText(existing?.name.orEmpty()) }
        val typeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@CategoryManagerActivity, android.R.layout.simple_spinner_dropdown_item, listOf("支出", "收入"))
            setSelection(if (existing?.type == CategoryType.INCOME) 1 else 0)
        }
        val colorEdit = EditText(this).apply { hint = "颜色值"; setText(existing?.colorHex ?: "#C44536") }
        val activeCheck = CheckBox(this).apply { text = "启用分类"; isChecked = existing?.isActive ?: true }
        layout.addView(nameEdit)
        layout.addView(typeSpinner)
        layout.addView(colorEdit)
        layout.addView(activeCheck)
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "新增分类" else "编辑分类")
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                runCatching {
                    repository.saveCategory(
                        Category(
                            id = existing?.id ?: 0L,
                            bookId = bookId,
                            name = nameEdit.text.toString().trim(),
                            type = if (typeSpinner.selectedItemPosition == 1) CategoryType.INCOME else CategoryType.EXPENSE,
                            colorHex = colorEdit.text.toString().trim().ifBlank { "#C44536" },
                            isActive = activeCheck.isChecked
                        )
                    )
                }.onSuccess { loadCategories() }
                    .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
