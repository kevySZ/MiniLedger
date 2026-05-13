package com.kevy.ledger.ui.common

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kevy.ledger.R
import com.kevy.ledger.databinding.ItemCategoryOptionBinding
import com.kevy.ledger.domain.model.Category

class CategoryGridAdapter(
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryGridAdapter.CategoryViewHolder>() {
    private val items = mutableListOf<Category>()
    private var selectedCategoryId: Long? = null

    fun submitList(data: List<Category>, selectedId: Long?) {
        items.clear()
        items.addAll(data)
        selectedCategoryId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position], items[position].id == selectedCategoryId)
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Category, selected: Boolean) {
            val visual = CategoryVisuals.forCategory(item.name, item.type, item.colorHex)
            binding.cardIcon.setCardBackgroundColor(Color.parseColor(visual.colorHex))
            binding.textIcon.setImageResource(visual.iconRes)
            binding.textName.text = item.name
            binding.root.alpha = if (selected) 1f else 0.9f
            binding.cardIcon.strokeWidth = if (selected) 3 else 0
            binding.cardIcon.strokeColor = if (selected) {
                ContextCompat.getColor(binding.root.context, R.color.brand_secondary)
            } else {
                Color.TRANSPARENT
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}
