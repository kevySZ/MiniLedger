package com.kevy.ledger.ui.main

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.FragmentHomeBinding
import com.kevy.ledger.domain.model.Book
import com.kevy.ledger.domain.model.LedgerTransaction
import com.kevy.ledger.ui.common.Refreshable
import com.kevy.ledger.ui.common.TransactionAdapter
import com.kevy.ledger.ui.transaction.TransactionEditorActivity
import com.kevy.ledger.util.DateUtils
import com.kevy.ledger.util.MoneyUtils
import java.time.YearMonth

class HomeFragment : Fragment(R.layout.fragment_home), Refreshable {
    private var binding: FragmentHomeBinding? = null
    private val repository get() = AppGraph.repository

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
        binding = FragmentHomeBinding.bind(view)

        backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (!adapter.collapseExpanded()) {
                    isEnabled = false
                }
            }
        }.also {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
        }

        binding?.recyclerRecent?.layoutManager = LinearLayoutManager(requireContext())
        binding?.recyclerRecent?.adapter = adapter
        binding?.buttonSwitchBook?.setOnClickListener { showBookPicker() }
        binding?.buttonAddTransaction?.setOnClickListener {
            startActivity(Intent(requireContext(), TransactionEditorActivity::class.java))
        }

        refreshContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback = null
        binding = null
    }

    override fun refreshContent() {
        val currentBook = repository.getCurrentBook() ?: return
        val currentMonth = YearMonth.from(DateUtils.today())
        val summary = repository.getMonthSummary(currentBook.id, currentMonth)
        val totalBalance = repository
            .getAccounts(currentBook.id, includeInactive = true)
            .sumOf { it.currentBalanceCents }
        val recentTransactions = repository.getRecentTransactions(currentBook.id)

        binding?.apply {
            textCurrentBook.text = currentBook.name
            textTotalBalance.text = MoneyUtils.centsToDisplay(totalBalance)
            textIncome.text = MoneyUtils.centsToDisplay(summary.incomeCents)
            textExpense.text = MoneyUtils.centsToDisplay(summary.expenseCents)
            textMonthIncome.text = MoneyUtils.centsToDisplay(summary.incomeCents)
            textMonthExpense.text = MoneyUtils.centsToDisplay(summary.expenseCents)
            textMonthBalance.text = MoneyUtils.centsToDisplay(summary.balanceCents)
            textEmptyRecent.visibility = if (recentTransactions.isEmpty()) View.VISIBLE else View.GONE
        }

        adapter.submitList(recentTransactions)
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

    private fun showBookPicker() {
        val books = repository.getBooks().filterNot(Book::isArchived)
        if (books.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_switch_book)
            .setItems(books.map(Book::name).toTypedArray()) { _, which ->
                repository.setSelectedBookId(books[which].id)
                refreshContent()
            }
            .show()
    }
}
