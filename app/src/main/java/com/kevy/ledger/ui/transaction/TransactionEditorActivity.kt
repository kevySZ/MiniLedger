package com.kevy.ledger.ui.transaction

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.ActivityTransactionEditorBinding
import com.kevy.ledger.domain.model.Account
import com.kevy.ledger.domain.model.Category
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.domain.model.EntryDirection
import com.kevy.ledger.domain.model.TransactionInput
import com.kevy.ledger.domain.model.TransactionType
import com.kevy.ledger.ui.common.ThemedActivity
import com.kevy.ledger.ui.common.CategoryGridAdapter
import com.kevy.ledger.ui.common.CategoryVisuals
import com.kevy.ledger.util.AmountExpressionEvaluator
import com.kevy.ledger.util.DateUtils
import com.kevy.ledger.util.MoneyUtils
import java.time.LocalDate

class TransactionEditorActivity : ThemedActivity() {
    private lateinit var binding: ActivityTransactionEditorBinding
    private val repository get() = AppGraph.repository

    private var transactionId: Long? = null
    private var bookId: Long = 0L
    private var selectedDate: LocalDate = DateUtils.today()
    private var categories: List<Category> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var selectedCategoryId: Long? = null
    private var categoryPage: Int = 0
    private var currentNote: String = ""

    private val categoryPageSize = 24
    private val categoryAdapter by lazy {
        CategoryGridAdapter { category ->
            selectedCategoryId = category.id
            updateSelectedCategorySummary()
            renderCategoryPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionId = intent.getLongExtra(EXTRA_TRANSACTION_ID, 0L).takeIf { it > 0L }
        bookId = repository.getSelectedBookId()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupTabs()
        setupCategoryGrid()
        setupSpinners()
        setupDatePicker()
        setupNoteEditor()
        setupAmountField()
        setupKeypad()
        setupAmountPreview()

        if (transactionId != null) {
            loadExistingTransaction()
        } else {
            supportActionBar?.title = getString(R.string.title_add_transaction)
            loadBaseData()
            applyType(currentType())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (transactionId != null) {
            menuInflater.inflate(R.menu.menu_transaction_editor, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.actionDelete -> {
                deleteTransaction()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadExistingTransaction() {
        val transaction = repository.getTransaction(transactionId ?: return) ?: run {
            finish()
            return
        }
        supportActionBar?.title = getString(R.string.title_edit_transaction)
        bookId = transaction.bookId
        selectedDate = transaction.eventDate
        selectedCategoryId = transaction.categoryId
        currentNote = transaction.note
        loadBaseData()
        selectTab(transaction.type)
        setSpinnerSelection(binding.spinnerAccount, transaction.accountId)
        setSpinnerSelection(binding.spinnerTransferAccount, transaction.transferAccountId)
        binding.spinnerDirection.setSelection(if (transaction.direction == EntryDirection.IN) 0 else 1)
        binding.editAmount.setText(MoneyUtils.centsToPlain(transaction.amountCents))
        applyType(transaction.type)
        updateDateDisplay()
        updateNoteDisplay()
        invalidateOptionsMenu()
    }

    private fun loadBaseData() {
        categories = repository.getCategories(bookId, includeInactive = false)
        accounts = repository.getAccounts(bookId).filter(Account::isActive)
        bindAccountSpinners()
        updateDateDisplay()
        updateNoteDisplay()

        if (transactionId == null) {
            selectedCategoryId = categoriesForType(currentType()).firstOrNull()?.id
        }
    }

    private fun setupTabs() {
        if (binding.tabType.tabCount == 0) {
            binding.tabType.addTab(binding.tabType.newTab().setText(R.string.transaction_type_expense))
            binding.tabType.addTab(binding.tabType.newTab().setText(R.string.transaction_type_income))
            binding.tabType.addTab(binding.tabType.newTab().setText(R.string.transaction_type_transfer))
            binding.tabType.addTab(binding.tabType.newTab().setText(R.string.transaction_type_adjustment))
        }
        binding.tabType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                applyType(tabPositionToType(tab.position))
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupCategoryGrid() {
        binding.recyclerCategories.apply {
            layoutManager = GridLayoutManager(this@TransactionEditorActivity, 6)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
        binding.buttonPrevPage.setOnClickListener {
            if (categoryPage > 0) {
                categoryPage--
                renderCategoryPage()
            }
        }
        binding.buttonNextPage.setOnClickListener {
            val lastPageIndex = (categoryCountForCurrentType() - 1) / categoryPageSize
            if (categoryPage < lastPageIndex) {
                categoryPage++
                renderCategoryPage()
            }
        }
    }

    private fun setupSpinners() {
        binding.spinnerDirection.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                getString(R.string.editor_increase_balance),
                getString(R.string.editor_decrease_balance)
            )
        )
    }

    private fun bindAccountSpinners() {
        val accountAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            accounts.map { it.name }
        )
        binding.spinnerAccount.adapter = accountAdapter
        binding.spinnerTransferAccount.adapter = accountAdapter
    }

    private fun setupDatePicker() {
        binding.buttonDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateDisplay()
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }
    }

    private fun setupNoteEditor() {
        binding.buttonNote.setOnClickListener {
            val input = EditText(this).apply {
                setText(currentNote)
                hint = getString(R.string.hint_optional_note)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setSelection(text?.length ?: 0)
                minLines = 3
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_note))
                .setView(input)
                .setPositiveButton(R.string.action_save) { _, _ ->
                    currentNote = input.text?.toString()?.trim().orEmpty()
                    updateNoteDisplay()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.action_clear) { _, _ ->
                    currentNote = ""
                    updateNoteDisplay()
                }
                .show()
        }
    }

    private fun setupAmountField() {
        binding.editAmount.apply {
            showSoftInputOnFocus = false
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            isLongClickable = false
            setTextIsSelectable(false)
            setOnClickListener { }
            setOnTouchListener { _, _ -> true }
        }
        syncAmountViewMetrics(binding.editAmount)
    }

    private fun setupAmountPreview() {
        binding.editAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateAmountPreview()
        })
        updateAmountPreview()
    }

    private fun setupKeypad() {
        bindKey(binding.keyOne, "1")
        bindKey(binding.keyTwo, "2")
        bindKey(binding.keyThree, "3")
        bindKey(binding.keyFour, "4")
        bindKey(binding.keyFive, "5")
        bindKey(binding.keySix, "6")
        bindKey(binding.keySeven, "7")
        bindKey(binding.keyEight, "8")
        bindKey(binding.keyNine, "9")
        bindKey(binding.keyZero, "0")
        bindKey(binding.keyPlus, "+")
        bindKey(binding.keyMinus, "-")
        bindKey(binding.keyMultiply, "*")
        bindKey(binding.keyDivide, "/")
        bindKey(binding.keyDot, ".")
        binding.keyClear.setOnClickListener { binding.editAmount.setText("") }
        binding.keyDelete.setOnClickListener { deleteLastAmountChar() }
        binding.keyOk.setOnClickListener { saveTransaction() }
    }

    private fun bindKey(view: View, token: String) {
        view.setOnClickListener {
            val current = binding.editAmount.text?.toString().orEmpty()
            binding.editAmount.setText(current + token)
            binding.editAmount.setSelection(binding.editAmount.text?.length ?: 0)
        }
    }

    private fun deleteLastAmountChar() {
        val current = binding.editAmount.text?.toString().orEmpty()
        if (current.isNotEmpty()) {
            binding.editAmount.setText(current.dropLast(1))
            binding.editAmount.setSelection(binding.editAmount.text?.length ?: 0)
        }
    }

    private fun syncAmountViewMetrics(editText: AppCompatEditText) {
        editText.includeFontPadding = false
    }

    private fun currentType(): TransactionType {
        return tabPositionToType(binding.tabType.selectedTabPosition.coerceAtLeast(0))
    }

    private fun tabPositionToType(position: Int): TransactionType {
        return when (position) {
            1 -> TransactionType.INCOME
            2 -> TransactionType.TRANSFER
            3 -> TransactionType.BALANCE_ADJUSTMENT
            else -> TransactionType.EXPENSE
        }
    }

    private fun selectTab(type: TransactionType) {
        val index = when (type) {
            TransactionType.EXPENSE -> 0
            TransactionType.INCOME -> 1
            TransactionType.TRANSFER -> 2
            TransactionType.BALANCE_ADJUSTMENT -> 3
        }
        binding.tabType.getTabAt(index)?.select()
    }

    private fun applyType(type: TransactionType) {
        val categoryItems = categoriesForType(type)
        if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
            if (categoryItems.none { it.id == selectedCategoryId }) {
                selectedCategoryId = categoryItems.firstOrNull()?.id
            }
            val selectedIndex = categoryItems.indexOfFirst { it.id == selectedCategoryId }
            categoryPage = if (selectedIndex >= 0) selectedIndex / categoryPageSize else 0
        } else {
            selectedCategoryId = null
            categoryPage = 0
        }

        binding.layoutCategorySection.visibility =
            if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) View.VISIBLE else View.GONE
        binding.layoutTransferAccountRow.visibility = if (type == TransactionType.TRANSFER) View.VISIBLE else View.GONE
        binding.layoutDirectionRow.visibility = if (type == TransactionType.BALANCE_ADJUSTMENT) View.VISIBLE else View.GONE
        binding.labelAccount.text =
            if (type == TransactionType.TRANSFER) getString(R.string.editor_out_account) else getString(R.string.label_account)

        renderCategoryPage()
        updateSelectedCategorySummary()
        updateAmountPreview()
    }

    private fun categoriesForType(type: TransactionType): List<Category> {
        return when (type) {
            TransactionType.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> categories.filter { it.type == CategoryType.INCOME }
            TransactionType.TRANSFER,
            TransactionType.BALANCE_ADJUSTMENT -> emptyList()
        }
    }

    private fun categoryCountForCurrentType(): Int = categoriesForType(currentType()).size

    private fun renderCategoryPage() {
        val items = categoriesForType(currentType())
        val pageCount = if (items.isEmpty()) 0 else ((items.size - 1) / categoryPageSize) + 1

        if (items.isEmpty()) {
            categoryAdapter.submitList(emptyList(), null)
            binding.layoutCategoryPager.visibility = View.GONE
            binding.textCategoryPage.text = getString(R.string.editor_no_category)
            return
        }

        categoryPage = categoryPage.coerceIn(0, pageCount - 1)
        val pageItems = items.drop(categoryPage * categoryPageSize).take(categoryPageSize)
        categoryAdapter.submitList(pageItems, selectedCategoryId)
        binding.layoutCategoryPager.visibility = if (pageCount > 1) View.VISIBLE else View.GONE
        binding.textCategoryPage.text = getString(R.string.label_category_page, categoryPage + 1, pageCount)
        binding.buttonPrevPage.alpha = if (categoryPage > 0) 1f else 0.45f
        binding.buttonNextPage.alpha = if (categoryPage < pageCount - 1) 1f else 0.45f
    }

    private fun updateSelectedCategorySummary() {
        val type = currentType()
        val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId }
        val visual = if (selectedCategory != null) {
            CategoryVisuals.forCategory(selectedCategory.name, selectedCategory.type, selectedCategory.colorHex)
        } else {
            CategoryVisuals.forTransaction(null, type)
        }

        binding.cardSelectedCategory.setCardBackgroundColor(Color.parseColor(visual.colorHex))
        binding.textSelectedCategoryIcon.setImageResource(visual.iconRes)
        binding.textSelectedCategoryTitle.text = when {
            selectedCategory != null -> getString(R.string.editor_selected_category, selectedCategory.name)
            type == TransactionType.TRANSFER -> getString(R.string.transaction_type_transfer)
            type == TransactionType.BALANCE_ADJUSTMENT -> getString(R.string.transaction_type_adjustment)
            else -> getString(R.string.editor_no_category)
        }
    }

    private fun updateAmountPreview() {
        val raw = binding.editAmount.text?.toString().orEmpty()
        binding.textAmountPreview.text = when {
            raw.isBlank() -> getString(R.string.editor_preview_empty)
            else -> runCatching {
                getString(
                    R.string.editor_preview_prefix,
                    MoneyUtils.centsToDisplay(AmountExpressionEvaluator.evaluateToCents(raw))
                )
            }.getOrElse {
                getString(R.string.editor_preview_invalid)
            }
        }
    }

    private fun updateDateDisplay() {
        binding.buttonDate.text =
            "${selectedDate.year}\n${selectedDate.monthValue}\u6708${selectedDate.dayOfMonth}\u53f7"
    }

    private fun updateNoteDisplay() {
        binding.buttonNote.text = if (currentNote.isBlank()) "\u6dfb\u52a0\u5907\u6ce8" else currentNote
    }

    private fun saveTransaction() {
        runCatching {
            val type = currentType()
            val amountCents = AmountExpressionEvaluator.evaluateToCents(
                binding.editAmount.text?.toString().orEmpty()
            )
            val input = TransactionInput(
                id = transactionId,
                bookId = bookId,
                type = type,
                direction = when (type) {
                    TransactionType.BALANCE_ADJUSTMENT ->
                        if (binding.spinnerDirection.selectedItemPosition == 0) EntryDirection.IN else EntryDirection.OUT

                    else -> EntryDirection.NONE
                },
                categoryId = if (type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                    selectedCategoryId
                } else {
                    null
                },
                accountId = accounts.getOrNull(binding.spinnerAccount.selectedItemPosition)?.id,
                transferAccountId = if (type == TransactionType.TRANSFER) {
                    accounts.getOrNull(binding.spinnerTransferAccount.selectedItemPosition)?.id
                } else {
                    null
                },
                amountCents = amountCents,
                note = currentNote,
                eventDate = selectedDate
            )
            repository.saveTransaction(input)
        }.onSuccess {
            Toast.makeText(this, "\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show()
            finish()
        }.onFailure { error ->
            Toast.makeText(this, error.message ?: "\u4fdd\u5b58\u5931\u8d25", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteTransaction() {
        val id = transactionId ?: return
        AlertDialog.Builder(this)
            .setMessage(R.string.message_delete_confirm)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repository.deleteTransaction(id)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setSpinnerSelection(spinner: Spinner, accountId: Long?) {
        val index = accounts.indexOfFirst { it.id == accountId }
        if (index >= 0) {
            spinner.setSelection(index)
        }
    }

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }
}
