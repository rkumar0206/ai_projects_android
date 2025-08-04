package com.rtb.ai.projects.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.ContentItem
import com.rtb.ai.projects.databinding.ItemImageContentBinding
import com.rtb.ai.projects.util.AppUtil
import com.rtb.ai.projects.util.AppUtil.copyToClipboard

class ContentAdapter(
    private val onGenerateImage: (itemId: String, prompt: String) -> Unit
) : ListAdapter<ContentItem, RecyclerView.ViewHolder>(ContentDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_IMAGE = 2
    }

    // --- ViewHolders ---
    inner class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.textViewContent)
        fun bind(item: ContentItem.TextContent) {
            textView.text = item.text
        }
    }

    inner class ImageViewHolder(val binding: ItemImageContentBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imageView: ImageView = binding.imageViewContent
        fun bind(item: ContentItem.ImageContent) {

            binding.progressBarLoading.isVisible = true
            binding.imageViewContent.setImageResource(R.drawable.image_by_color)

            if (item.imageFilePath != null) {
                Glide.with(itemView.context)
                    .load(AppUtil.retrieveImageAsByteArray(item.imageFilePath))
                    .placeholder(R.drawable.stories_img)
                    .error(R.drawable.ic_broken_image)
                    .into(imageView)

                binding.progressBarLoading.isVisible = false
                binding.textViewImagePrompt.text = item.imagePrompt

                binding.imageViewContent.setOnClickListener {
                   togglePromptVisibility()
                }
                
                binding.imageViewCopyPrompt.setOnClickListener {
                    item.imagePrompt?.copyToClipboard(binding.root.context)
                }

            } else if (item.imageResId != null) {
                Glide.with(itemView.context)
                    .load(item.imageResId)
                    .placeholder(R.drawable.stories_img)
                    .error(R.drawable.ic_broken_image)
                    .into(imageView)
                binding.progressBarLoading.isVisible = false
            }else if(item.imagePrompt != null) {
                onGenerateImage(item.id, item.imagePrompt)
            }
        }

        private fun togglePromptVisibility() {
            if (binding.groupTextElements.isVisible) {
                binding.groupTextElements.visibility = View.GONE
            } else {
                binding.groupTextElements.visibility = View.VISIBLE
            }
        }
    }

    // --- Adapter Overrides ---
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContentItem.TextContent -> VIEW_TYPE_TEXT
            is ContentItem.ImageContent -> VIEW_TYPE_IMAGE
            // Add more types if you expand ContentItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TEXT -> {
                val view = inflater.inflate(R.layout.item_text_content, parent, false)
                TextViewHolder(view)
            }

            VIEW_TYPE_IMAGE -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemImageContentBinding.inflate(inflater, parent, false)
                ImageViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TextViewHolder -> holder.bind(item as ContentItem.TextContent)
            is ImageViewHolder -> holder.bind(item as ContentItem.ImageContent)
        }
    }
}

// --- DiffUtil Callback for ListAdapter ---
class ContentDiffCallback : DiffUtil.ItemCallback<ContentItem>() {
    override fun areItemsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
        return oldItem.id == newItem.id // Use the common ID for item identity
    }

    override fun areContentsTheSame(oldItem: ContentItem, newItem: ContentItem): Boolean {
        return oldItem == newItem // Data classes implement equals correctly
    }
}