package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.adapter // Adjust package

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.Recipe
import com.rtb.ai.projects.databinding.ItemSavedBinding
import com.rtb.ai.projects.util.AppUtil

class SavedRecipesAdapter(
    private val onItemClick: (Long) -> Unit, // Recipe ID
    private val onDeleteClick: (Recipe) -> Unit
) : ListAdapter<Recipe, SavedRecipesAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding =
            ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        private val binding: ItemSavedBinding,
        private val onItemClick: (Long) -> Unit,
        private val onDeleteClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentRecipe: Recipe? = null

        init {
            binding.root.setOnClickListener {
                currentRecipe?.id?.let { recipeId -> // Assuming your Recipe has an 'id'
                    onItemClick(recipeId)
                }
            }
            binding.imageViewDelete.setOnClickListener {
                currentRecipe?.let { recipe ->
                    onDeleteClick(recipe)
                }
            }
        }

        fun bind(recipe: Recipe) {
            currentRecipe = recipe
            binding.textViewItemName.text = recipe.recipeName

            // Load image using Glide (or your preferred image loading library)
            // Assuming your Recipe model has an imagePath or imageByteArray

            val imageByteArray = AppUtil.retrieveImageAsByteArray(recipe.imageFilePath)

            if (imageByteArray != null) {

                Glide.with(binding.imageViewItem.context)
                    .load(imageByteArray)
                    .placeholder(R.drawable.food_recipe_img) // Your placeholder
                    .error(R.drawable.ic_broken_image) // Error placeholder
                    .into(binding.imageViewItem)
            } else {
                // Use a default placeholder if no image
                binding.imageViewItem.setImageResource(R.drawable.food_recipe_img)
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id // Assuming your Recipe has an 'id'
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}
