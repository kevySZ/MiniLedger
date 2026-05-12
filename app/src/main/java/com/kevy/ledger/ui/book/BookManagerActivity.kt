package com.kevy.ledger.ui.book

import android.app.AlertDialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.ActivityManagerListBinding
import com.kevy.ledger.domain.model.Book
import com.kevy.ledger.ui.common.ManagerRow
import com.kevy.ledger.ui.common.ManagerRowAdapter

class BookManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerListBinding
    private val repository get() = AppGraph.repository
    private val adapter by lazy { ManagerRowAdapter { showBookActions(it.id) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.title_book_manager)
        binding.textSubtitle.text = "点击账本可设为当前、编辑或归档"
        binding.textEmpty.text = getString(R.string.empty_books)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.fabAdd.setOnClickListener { showEditDialog(null) }
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun loadBooks() {
        val currentId = repository.getSelectedBookId()
        val rows = repository.getBooks(includeArchived = true).map {
            ManagerRow(
                id = it.id,
                title = it.name,
                subtitle = it.note.ifBlank { "颜色 ${it.colorHex}" },
                meta = buildString {
                    if (it.id == currentId) append("当前账本 ")
                    if (it.isDefault) append("默认账本 ")
                    if (it.isArchived) append("已归档")
                }.trim()
            )
        }
        binding.textEmpty.visibility = if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        adapter.submitList(rows)
    }

    private fun showBookActions(bookId: Long) {
        val book = repository.getBook(bookId) ?: return
        AlertDialog.Builder(this)
            .setTitle(book.name)
            .setItems(arrayOf("设为当前账本", "编辑", "归档")) { _, which ->
                when (which) {
                    0 -> {
                        repository.setSelectedBookId(book.id)
                        loadBooks()
                    }
                    1 -> showEditDialog(book)
                    2 -> runCatching { repository.archiveBook(book.id) }
                        .onSuccess { loadBooks() }
                        .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
                }
            }
            .show()
    }

    private fun showEditDialog(book: Book?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36)
        }
        val nameEdit = EditText(this).apply { hint = "账本名称"; setText(book?.name.orEmpty()) }
        val colorEdit = EditText(this).apply { hint = "颜色值"; setText(book?.colorHex ?: "#1B6B5C") }
        val noteEdit = EditText(this).apply { hint = "备注"; setText(book?.note.orEmpty()) }
        val defaultCheck = CheckBox(this).apply { text = "设为默认账本"; isChecked = book?.isDefault ?: false }
        layout.addView(nameEdit)
        layout.addView(colorEdit)
        layout.addView(noteEdit)
        layout.addView(defaultCheck)
        AlertDialog.Builder(this)
            .setTitle(if (book == null) "新增账本" else "编辑账本")
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                runCatching {
                    repository.saveBook(
                        Book(
                            id = book?.id ?: 0L,
                            name = nameEdit.text.toString().trim(),
                            colorHex = colorEdit.text.toString().trim().ifBlank { "#1B6B5C" },
                            note = noteEdit.text.toString().trim(),
                            isDefault = defaultCheck.isChecked,
                            isArchived = book?.isArchived ?: false
                        )
                    )
                }.onSuccess { loadBooks() }
                    .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
