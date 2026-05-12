package com.kevy.ledger.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kevy.ledger.databinding.ItemManagerRowBinding

data class ManagerRow(
    val id: Long,
    val title: String,
    val subtitle: String,
    val meta: String
)

class ManagerRowAdapter(private val onClick: (ManagerRow) -> Unit) :
    RecyclerView.Adapter<ManagerRowAdapter.ManagerRowViewHolder>() {
    private val items = mutableListOf<ManagerRow>()

    fun submitList(rows: List<ManagerRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManagerRowViewHolder {
        val binding = ItemManagerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ManagerRowViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ManagerRowViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ManagerRowViewHolder(private val binding: ItemManagerRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ManagerRow) {
            binding.textTitle.text = item.title
            binding.textSubtitle.text = item.subtitle
            binding.textMeta.text = item.meta
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
