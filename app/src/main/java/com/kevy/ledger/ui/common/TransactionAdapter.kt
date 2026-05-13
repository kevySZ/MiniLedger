package com.kevy.ledger.ui.common

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kevy.ledger.R
import com.kevy.ledger.databinding.ItemTransactionBinding
import com.kevy.ledger.domain.model.LedgerTransaction
import com.kevy.ledger.domain.model.TransactionType
import com.kevy.ledger.util.MoneyUtils
import java.time.LocalDate

class TransactionAdapter(
    private val onEditClick: (LedgerTransaction) -> Unit,
    private val onDeleteClick: (LedgerTransaction) -> Unit,
    private val onActionVisibilityChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    private val items = mutableListOf<RenderItem>()
    private var expandedTransactionId: Long? = null

    fun submitList(data: List<LedgerTransaction>) {
        val dailyExpenseMap = data
            .groupBy(LedgerTransaction::eventDate)
            .mapValues { (_, group) ->
                group
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf(LedgerTransaction::amountCents)
            }

        items.clear()
        items.addAll(
            data.mapIndexed { index, transaction ->
                RenderItem(
                    transaction = transaction,
                    showHeader = index == 0 || data[index - 1].eventDate != transaction.eventDate,
                    dailyExpenseCents = dailyExpenseMap[transaction.eventDate] ?: 0L
                )
            }
        )
        if (items.none { it.transaction.id == expandedTransactionId }) {
            expandedTransactionId = null
            onActionVisibilityChanged(false)
        }
        notifyDataSetChanged()
    }

    fun collapseExpanded(): Boolean {
        val current = expandedTransactionId ?: return false
        val index = items.indexOfFirst { it.transaction.id == current }
        expandedTransactionId = null
        if (index >= 0) {
            notifyItemChanged(index)
        } else {
            notifyDataSetChanged()
        }
        onActionVisibilityChanged(false)
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(renderItem: RenderItem) {
            val item = renderItem.transaction
            val context = binding.root.context
            val visual = CategoryVisuals.forTransaction(item.categoryName, item.type)
            val tintColor = Color.parseColor(visual.colorHex)
            val title = when (item.type) {
                TransactionType.TRANSFER -> context.getString(R.string.timeline_transfer)
                TransactionType.BALANCE_ADJUSTMENT -> context.getString(R.string.timeline_adjustment)
                else -> item.categoryName ?: context.getString(R.string.category_none)
            }
            val note = item.note.trim()
            val showLeft = item.type == TransactionType.INCOME
            val actionsVisible = expandedTransactionId == item.id

            binding.cardBadge.setCardBackgroundColor(tintColor)
            binding.textBadge.setImageResource(visual.iconRes)
            binding.viewHeaderDot.backgroundTintList = ColorStateList.valueOf(tintColor)
            binding.textDate.text = formatDate(item.eventDate)
            binding.textDayExpense.text = context.getString(
                R.string.timeline_day_expense,
                MoneyUtils.centsToDisplay(renderItem.dailyExpenseCents)
            )

            binding.textDate.visibility = if (renderItem.showHeader) View.VISIBLE else View.INVISIBLE
            binding.textDayExpense.visibility = if (renderItem.showHeader) View.VISIBLE else View.INVISIBLE
            binding.viewHeaderDot.visibility = if (renderItem.showHeader) View.VISIBLE else View.INVISIBLE

            binding.layoutIncomeSide.visibility = if (showLeft) View.VISIBLE else View.INVISIBLE
            binding.layoutExpenseSide.visibility = if (showLeft) View.INVISIBLE else View.VISIBLE

            binding.layoutIncomeActions.visibility = if (showLeft && actionsVisible) View.VISIBLE else View.GONE
            binding.layoutExpenseActions.visibility = if (!showLeft && actionsVisible) View.VISIBLE else View.GONE

            if (showLeft) {
                binding.textIncomeTitle.text = title
                binding.textIncomeAmount.text = MoneyUtils.centsToDisplay(item.amountCents)
                binding.textIncomeSubtitle.text = note
                binding.textIncomeSubtitle.visibility = if (note.isBlank()) View.GONE else View.VISIBLE

                binding.textExpenseTitle.text = ""
                binding.textExpenseAmount.text = ""
                binding.textExpenseSubtitle.text = ""
                binding.textExpenseSubtitle.visibility = View.GONE
            } else {
                binding.textExpenseTitle.text = title
                binding.textExpenseAmount.text = MoneyUtils.centsToDisplay(item.amountCents)
                binding.textExpenseSubtitle.text = note
                binding.textExpenseSubtitle.visibility = if (note.isBlank()) View.GONE else View.VISIBLE

                binding.textIncomeTitle.text = ""
                binding.textIncomeAmount.text = ""
                binding.textIncomeSubtitle.text = ""
                binding.textIncomeSubtitle.visibility = View.GONE
            }

            binding.buttonIncomeEdit.setOnClickListener { onEditClick(item) }
            binding.buttonIncomeDelete.setOnClickListener { onDeleteClick(item) }
            binding.buttonExpenseEdit.setOnClickListener { onEditClick(item) }
            binding.buttonExpenseDelete.setOnClickListener { onDeleteClick(item) }
            binding.root.setOnClickListener { toggleActions(item.id) }
            binding.root.setOnLongClickListener {
                toggleActions(item.id)
                true
            }
        }

        private fun formatDate(date: LocalDate): String = "%02d.%02d".format(date.monthValue, date.dayOfMonth)
    }

    private fun toggleActions(transactionId: Long) {
        val previousId = expandedTransactionId
        expandedTransactionId = if (expandedTransactionId == transactionId) null else transactionId

        val changedIds = listOfNotNull(previousId, expandedTransactionId).distinct()
        if (changedIds.isEmpty()) {
            notifyDataSetChanged()
        } else {
            changedIds.forEach { id ->
                val index = items.indexOfFirst { it.transaction.id == id }
                if (index >= 0) notifyItemChanged(index)
            }
        }
        onActionVisibilityChanged(expandedTransactionId != null)
    }

    data class RenderItem(
        val transaction: LedgerTransaction,
        val showHeader: Boolean,
        val dailyExpenseCents: Long
    )
}
