package com.kevy.ledger.ui.book

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
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
        binding.textSubtitle.text = "\u70b9\u51fb\u8d26\u672c\u53ef\u8bbe\u4e3a\u5f53\u524d\u3001\u7f16\u8f91\u6216\u5f52\u6863"
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
                subtitle = it.note.ifBlank { "\u4e3b\u9898\u8272 ${it.colorHex}" },
                meta = buildString {
                    if (it.id == currentId) append("\u5f53\u524d\u8d26\u672c ")
                    if (it.isDefault) append("\u9ed8\u8ba4\u8d26\u672c ")
                    if (it.isArchived) append("\u5df2\u5f52\u6863")
                }.trim()
            )
        }
        binding.textEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(rows)
    }

    private fun showBookActions(bookId: Long) {
        val book = repository.getBook(bookId) ?: return
        AlertDialog.Builder(this)
            .setTitle(book.name)
            .setItems(arrayOf("\u8bbe\u4e3a\u5f53\u524d\u8d26\u672c", "\u7f16\u8f91", "\u5f52\u6863")) { _, which ->
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
        val nameEdit = EditText(this).apply {
            hint = "\u8d26\u672c\u540d\u79f0"
            setText(book?.name.orEmpty())
        }
        val colorEdit = EditText(this).apply {
            hint = "\u989c\u8272\u503c"
            setText(book?.colorHex ?: "#8FC9B4")
        }
        val noteEdit = EditText(this).apply {
            hint = "\u5907\u6ce8"
            setText(book?.note.orEmpty())
        }
        val defaultCheck = CheckBox(this).apply {
            text = "\u8bbe\u4e3a\u9ed8\u8ba4\u8d26\u672c"
            isChecked = book?.isDefault ?: false
        }
        layout.addView(nameEdit)
        layout.addView(colorEdit)
        layout.addView(noteEdit)
        layout.addView(defaultCheck)
        AlertDialog.Builder(this)
            .setTitle(if (book == null) "\u65b0\u589e\u8d26\u672c" else "\u7f16\u8f91\u8d26\u672c")
            .setView(layout)
            .setPositiveButton(R.string.action_save) { _, _ ->
                runCatching {
                    repository.saveBook(
                        Book(
                            id = book?.id ?: 0L,
                            name = nameEdit.text.toString().trim(),
                            colorHex = colorEdit.text.toString().trim().ifBlank { "#8FC9B4" },
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
