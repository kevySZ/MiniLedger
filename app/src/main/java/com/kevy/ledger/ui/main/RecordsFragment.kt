package com.kevy.ledger.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.FragmentRecordsBinding
import com.kevy.ledger.domain.model.CategoryType
import com.kevy.ledger.domain.model.LedgerTransaction
import com.kevy.ledger.domain.model.TransactionFilter
import com.kevy.ledger.domain.model.TransactionType
import com.kevy.ledger.ui.common.Refreshable
import com.kevy.ledger.ui.common.TransactionAdapter
import com.kevy.ledger.ui.transaction.TransactionEditorActivity
import com.kevy.ledger.util.AmountExpressionEvaluator
import com.kevy.ledger.util.DateUtils
import java.time.YearMonth

class RecordsFragment : Fragment(R.layout.fragment_records), Refreshable {
    private var binding: FragmentRecordsBinding? = null
    private val repository get() = AppGraph.repository

    private var currentMonth: YearMonth = YearMonth.now()
    private var selectedType: TransactionType? = null
    private var selectedCategoryId: Long? = null
    private var selectedAccountId: Long? = null
    private var minAmount: Long? = null
    private var maxAmount: Long? = null
    private var backPressedCallback: OnBackPressedCallback? = null

    private val adapter by lazy {
        TransactionAdapter(
            onEditClick = { transaction -> editTransaction(transaction) },
            onDeleteClick = { transaction -> confirmDeleteTransaction(transaction) },
            onActionVisibilityChanged = { visible ->
                backPressedCallback?.isEnabled = visible
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecordsBinding.bind(view)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!adapter.collapseExpanded()) {
                    isEnabled = false
                }
            }
        }.also {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
        }

        binding?.recyclerRecords?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerRecords?.adapter = adapter
        binding?.buttonPrevMonth?.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            refreshContent()
        }
        binding?.buttonNextMonth?.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            refreshContent()
        }
        binding?.buttonFilter?.setOnClickListener { showFilterDialog() }
        binding?.editSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refreshContent()
        })

        refreshContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback = null
        binding = null
    }

    override fun refreshContent() {
        val filter = TransactionFilter(
            bookId = repository.getSelectedBookId(),
            startDate = currentMonth.atDay(1),
            endDate = currentMonth.atEndOfMonth(),
            type = selectedType,
            categoryId = selectedCategoryId,
            accountId = selectedAccountId,
            keyword = binding?.editSearch?.text?.toString()?.trim().orEmpty().ifBlank { null },
            minAmountCents = minAmount,
            maxAmountCents = maxAmount
        )
        val records = repository.getTransactions(filter)

        binding?.apply {
            textMonth.text = DateUtils.formatMonth(currentMonth)
            textFilterSummary.text = buildFilterSummary()
            textEmptyRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
        adapter.submitList(records)
    }

    private fun buildFilterSummary(): String {
        val parts = mutableListOf(getString(R.string.filter_current_month))
        selectedType?.let { parts += transactionTypeLabel(it) }
        if (selectedCategoryId != null) parts += getString(R.string.filter_selected_category)
        if (selectedAccountId != null) parts += getString(R.string.filter_selected_account)
        if (minAmount != null || maxAmount != null) parts += getString(R.string.filter_amount_range)
        return parts.joinToString(" 路 ")
    }

    private fun editTransaction(transaction: LedgerTransaction) {
        startActivity(
            Intent(requireContext(), TransactionEditorActivity::class.java)
                .putExtra(TransactionEditorActivity.EXTRA_TRANSACTION_ID, transaction.id)
        )
    }

    private fun confirmDeleteTransaction(transaction: LedgerTransaction) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.action_delete)
            .setMessage(R.string.message_delete_confirm)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                repository.deleteTransaction(transaction.id)
                refreshContent()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFilterDialog() {
        val context = requireContext()
        val bookId = repository.getSelectedBookId()
        val categories = repository.getCategories(bookId, includeInactive = false)
        val accounts = repository.getAccounts(bookId)
        val horizontalPadding = (24 * resources.displayMetrics.density).toInt()
        val verticalPadding = (16 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }

        val typeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    getString(R.string.dialog_all),
                    getString(R.string.transaction_type_expense),
                    getString(R.string.transaction_type_income),
                    getString(R.string.transaction_type_transfer),
                    getString(R.string.transaction_type_adjustment)
                )
            )
            setSelection(
                when (selectedType) {
                    TransactionType.EXPENSE -> 1
                    TransactionType.INCOME -> 2
                    TransactionType.TRANSFER -> 3
                    TransactionType.BALANCE_ADJUSTMENT -> 4
                    null -> 0
                }
            )
        }

        val categorySpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                buildList {
                    add(getString(R.string.dialog_all_categories))
                    addAll(categories.map { "${categoryTypeLabel(it.type)} 路 ${it.name}" })
                }
            )
            selectedCategoryId?.let { id ->
                val index = categories.indexOfFirst { it.id == id }
                if (index >= 0) setSelection(index + 1)
            }
        }

        val accountSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                buildList {
                    add(getString(R.string.dialog_all_accounts))
                    addAll(accounts.map { it.name })
                }
            )
            selectedAccountId?.let { id ->
                val index = accounts.indexOfFirst { it.id == id }
                if (index >= 0) setSelection(index + 1)
            }
        }

        val minEdit = EditText(context).apply {
            hint = getString(R.string.dialog_min_amount)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(minAmount?.let { it / 100.0 }?.toString().orEmpty())
        }

        val maxEdit = EditText(context).apply {
            hint = getString(R.string.dialog_max_amount)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(maxAmount?.let { it / 100.0 }?.toString().orEmpty())
        }

        layout.addView(typeSpinner)
        layout.addView(categorySpinner)
        layout.addView(accountSpinner)
        layout.addView(minEdit)
        layout.addView(maxEdit)

        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_filter_records)
            .setView(layout)
            .setPositiveButton(R.string.dialog_apply) { _, _ ->
                selectedType = when (typeSpinner.selectedItemPosition) {
                    1 -> TransactionType.EXPENSE
                    2 -> TransactionType.INCOME
                    3 -> TransactionType.TRANSFER
                    4 -> TransactionType.BALANCE_ADJUSTMENT
                    else -> null
                }
                selectedCategoryId = categories.getOrNull(categorySpinner.selectedItemPosition - 1)?.id
                selectedAccountId = accounts.getOrNull(accountSpinner.selectedItemPosition - 1)?.id
                minAmount = minEdit.text?.toString()?.takeIf { it.isNotBlank() }?.let(AmountExpressionEvaluator::evaluateToCents)
                maxAmount = maxEdit.text?.toString()?.takeIf { it.isNotBlank() }?.let(AmountExpressionEvaluator::evaluateToCents)
                refreshContent()
            }
            .setNeutralButton(R.string.dialog_clear) { _, _ ->
                selectedType = null
                selectedCategoryId = null
                selectedAccountId = null
                minAmount = null
                maxAmount = null
                refreshContent()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun transactionTypeLabel(type: TransactionType): String {
        return when (type) {
            TransactionType.EXPENSE -> getString(R.string.transaction_type_expense)
            TransactionType.INCOME -> getString(R.string.transaction_type_income)
            TransactionType.TRANSFER -> getString(R.string.transaction_type_transfer)
            TransactionType.BALANCE_ADJUSTMENT -> getString(R.string.transaction_type_adjustment)
        }
    }

    private fun categoryTypeLabel(type: CategoryType): String {
        return when (type) {
            CategoryType.EXPENSE -> getString(R.string.transaction_type_expense)
            CategoryType.INCOME -> getString(R.string.transaction_type_income)
        }
    }
}
