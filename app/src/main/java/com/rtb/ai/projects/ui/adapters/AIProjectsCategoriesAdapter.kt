package com.rtb.ai.projects.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rtb.ai.projects.data.model.AIProjectCategory
import com.rtb.ai.projects.databinding.ItemAiProjectsCategoryBinding

class AIProjectsCategoriesAdapter(
    private val categories: List<AIProjectCategory>,
    private val onItemClick: (AIProjectCategory) -> Unit
) : RecyclerView.Adapter<AIProjectsCategoriesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAiProjectsCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(category: AIProjectCategory, onItemClick: (AIProjectCategory) -> Unit) {
            binding.imageViewItem.setImageResource(category.imageResId);
            binding.textViewTitle.text = category.title;
            binding.root.setOnClickListener { onItemClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAiProjectsCategoryBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val currentItem = categories[position]
        holder.bind(currentItem, onItemClick)
    }

}