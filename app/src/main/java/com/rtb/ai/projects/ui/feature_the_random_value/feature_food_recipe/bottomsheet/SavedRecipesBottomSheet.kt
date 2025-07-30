package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.bottomsheet // Adjust package

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.databinding.BottomsheetSavedRecipesBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.RandomRecipeViewModel
import com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.adapter.SavedRecipesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedRecipesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetSavedRecipesBinding? = null
    private val binding get() = _binding!!

    // Use activityViewModels to share the ViewModel with RandomRecipeFragment
    private val viewModel: RandomRecipeViewModel by activityViewModels()

    private lateinit var savedRecipesAdapter: SavedRecipesAdapter

    companion object {
        const val TAG = "SavedRecipesBottomSheet"
        const val REQUEST_KEY_RECIPE_CLICKED = "requestRecipeClicked"
        const val BUNDLE_KEY_RECIPE_ID = "bundleRecipeId"

        fun newInstance(): SavedRecipesBottomSheet {
            return SavedRecipesBottomSheet()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSavedRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // Fetch all recipes when the BottomSheet is created/shown
        // viewModel.getAllRecipes() // Assuming you have this method in your ViewModel
        // Or it's a Flow that you collect
    }

    private fun setupRecyclerView() {
        savedRecipesAdapter = SavedRecipesAdapter(
            onItemClick = { recipeId ->
                // Pass the recipeId back to the RandomRecipeFragment
                setFragmentResult(
                    REQUEST_KEY_RECIPE_CLICKED,
                    bundleOf(BUNDLE_KEY_RECIPE_ID to recipeId)
                )
                dismiss() // Close the bottom sheet
            },
            onDeleteClick = { recipe ->
                // Call ViewModel's delete method
                viewModel.deleteRecipe(recipe)
            }
        )
        binding.recyclerViewSavedRecipes.apply {
            adapter = savedRecipesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the list of all recipes from the ViewModel
                viewModel.allRecipes.collect { recipes -> // Assuming Flow named 'allSavedRecipes'
                    Log.d(TAG, "Saved recipes received: ${recipes.size}")
                    if (recipes.isEmpty()) {
                        binding.recyclerViewSavedRecipes.visibility = View.GONE
                        binding.textViewEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewSavedRecipes.visibility = View.VISIBLE
                        binding.textViewEmptyState.visibility = View.GONE
                    }
                    savedRecipesAdapter.submitList(recipes)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
