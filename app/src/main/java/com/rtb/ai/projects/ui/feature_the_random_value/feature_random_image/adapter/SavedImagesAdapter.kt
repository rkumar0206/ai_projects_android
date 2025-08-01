package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.adapter // Adjust package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.databinding.ItemSavedBinding
import com.rtb.ai.projects.util.AppUtil

class SavedImagesAdapter(
    private val onItemClick: (Long) -> Unit,
    private val onDeleteClick: (AIImage) -> Unit
) : ListAdapter<AIImage, SavedImagesAdapter.AIImageViewHolder>(AIImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AIImageViewHolder {
        val binding =
            ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AIImageViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: AIImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AIImageViewHolder(
        private val binding: ItemSavedBinding,
        private val onItemClick: (Long) -> Unit,
        private val onDeleteClick: (AIImage) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAIImage: AIImage? = null

        init {
            binding.root.setOnClickListener {
                currentAIImage?.id?.let { aiImageId ->
                    onItemClick(aiImageId)
                }
            }
            binding.imageViewDelete.setOnClickListener {
                currentAIImage?.let { aiImage ->
                    onDeleteClick(aiImage)
                }
            }
        }

        fun bind(aIImage: AIImage) {
            currentAIImage = aIImage
            binding.textViewItemName.text = aIImage.imagePrompt

            val imageByteArray = AppUtil.retrieveImageAsByteArray(aIImage.imageFilePath)

            if (imageByteArray != null) {

                Glide.with(binding.imageViewItem.context)
                    .load(imageByteArray)
                    .placeholder(R.drawable.generate_random_image) // Your placeholder
                    .error(R.drawable.ic_broken_image) // Error placeholder
                    .into(binding.imageViewItem)
            } else {
                // Use a default placeholder if no image
                binding.imageViewItem.setImageResource(R.drawable.generate_random_image)
            }
        }
    }

    class AIImageDiffCallback : DiffUtil.ItemCallback<AIImage>() {
        override fun areItemsTheSame(oldItem: AIImage, newItem: AIImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AIImage, newItem: AIImage): Boolean {
            return oldItem == newItem
        }
    }
}
