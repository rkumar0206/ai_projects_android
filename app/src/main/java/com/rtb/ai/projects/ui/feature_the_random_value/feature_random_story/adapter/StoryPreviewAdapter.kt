package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIStory
import com.rtb.ai.projects.databinding.ItemStoryPreviewBinding
import com.rtb.ai.projects.util.constant.Constants.TYPE_IMAGE
import com.rtb.ai.projects.util.constant.Constants.TYPE_TEXT


class StoryPreviewAdapter(
    private val onStoryClicked: (AIStory) -> Unit
) : ListAdapter<AIStory, StoryPreviewAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding =
            ItemStoryPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = getItem(position)
        holder.bind(story, onStoryClicked)
    }

    class StoryViewHolder(val binding: ItemStoryPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageContainer: LinearLayout = binding.llImageContainer
        private val storyPreviewText: TextView = binding.tvStoryPreview
        private val context: Context = itemView.context
        fun bind(story: AIStory, onStoryClicked: (AIStory) -> Unit) {

            val imagePaths = story.storyContent?.filter { item -> item.type == TYPE_IMAGE }
                ?.map { it.imageFilePath }

            storyPreviewText.text = story.storyContent?.first { it.type == TYPE_TEXT }?.text ?: ""
            binding.tvStoryTitle.text = story.storyTitle

            imageContainer.removeAllViews() // Clear previous images

            if (!imagePaths.isNullOrEmpty()) {

                // Ensure minimum of 3 images, max of, say, 5 for display
                val displayImageCount: Int
                val imagesToShow = imagePaths.take(5) // Max 5 images
                displayImageCount = imagesToShow.size

                for (i in 0 until displayImageCount) {
                    val imageView = ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            100.dpToPx(context), // Fixed width
                            100.dpToPx(context)  // Fixed height
                        ).also {
                            if (i < displayImageCount - 1) {
                                it.marginEnd = 4.dpToPx(context)
                            }
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    Glide.with(context)
                        .load(imagesToShow[i]) // Assuming imagePaths are valid URLs or file paths
                        .placeholder(R.drawable.generate_random_image) // Replace with your placeholder
                        .error(R.drawable.ic_broken_image)       // Replace with your error drawable
                        .into(imageView)
                    imageContainer.addView(imageView)
                }

                // If less than 3 images, you might want to add placeholders or adjust layout
                // For this example, it just shows what's available up to 5.
                // The requirement was "minimum 3 images for each story" - this implies data integrity.
                // If imagePaths can have less than 3, add logic here to show placeholders to fill up to 3.
                if (displayImageCount < 3) {
                    for (i in 0 until (3 - displayImageCount)) {
                        val placeholderView = ImageView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                100.dpToPx(context),
                                100.dpToPx(context)
                            ).also {
                                it.marginEnd = 4.dpToPx(context)
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageResource(R.drawable.generate_random_image) // Your placeholder drawable
                        }
                        imageContainer.addView(placeholderView)
                    }
                }
            }

            itemView.setOnClickListener {
                onStoryClicked(story)
            }
        }

        private fun Int.dpToPx(context: Context): Int =
            (this * context.resources.displayMetrics.density).toInt()
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<AIStory>() {
        override fun areItemsTheSame(oldItem: AIStory, newItem: AIStory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AIStory, newItem: AIStory): Boolean {
            return oldItem == newItem
        }
    }
}
