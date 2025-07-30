package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.FragmentRandomRecipeBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.bottomsheet.FilterRandomRecipeBottomSheet
import com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.bottomsheet.SavedRecipesBottomSheet
import com.rtb.ai.projects.util.AppUtil.displayMarkdownWithMarkwon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RandomRecipeFragment : Fragment() {

    companion object {
        const val TAG = "RandomRecipeFragment"
    }

    private var _binding: FragmentRandomRecipeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomRecipeViewModel by viewModels()

    // Store current filter values if you want to pre-fill the bottom sheet next time
    private var currentRegionFilter: String? = null
    private var currentIngredientsFilter: String? = null
    private var currentOtherConsiderationsFilter: String? = null
    private var isRecipeRefreshing = true
    private var isRecipeAlreadySaved = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for results from the BottomSheet
        setFragmentResultListener(FilterRandomRecipeBottomSheet.REQUEST_KEY_FILTER) { requestKey, bundle ->
            if (requestKey == FilterRandomRecipeBottomSheet.REQUEST_KEY_FILTER) {
                currentRegionFilter =
                    bundle.getString(FilterRandomRecipeBottomSheet.BUNDLE_KEY_REGION)
                currentIngredientsFilter =
                    bundle.getString(FilterRandomRecipeBottomSheet.BUNDLE_KEY_INGREDIENTS)
                currentOtherConsiderationsFilter =
                    bundle.getString(FilterRandomRecipeBottomSheet.BUNDLE_KEY_OTHER_CONSIDERATIONS)
                viewModel.applyFilters(
                    currentRegionFilter,
                    currentIngredientsFilter,
                    currentOtherConsiderationsFilter
                )
            }
        }

        // Listen for results from the SavedRecipesBottomSheet
        setFragmentResultListener(SavedRecipesBottomSheet.REQUEST_KEY_RECIPE_CLICKED) { requestKey, bundle ->
            if (requestKey == SavedRecipesBottomSheet.REQUEST_KEY_RECIPE_CLICKED) {
                val recipeId = bundle.getLong(SavedRecipesBottomSheet.BUNDLE_KEY_RECIPE_ID, -1L)
                if (recipeId != -1L) {
                    Log.d(TAG, "Recipe ID received from bottom sheet: $recipeId")
                    // Call ViewModel to load this recipe
                    viewModel.getAndShowSelectedRecipe(recipeId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMenu()
        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recipeUiState.collect { recipeUiState ->
                        updateRecipeUI(recipeUiState)
                    }
                }

                launch {
                    viewModel.imageResult.collect { imageResult ->
                        updateImageUi(imageResult)
                    }
                }

                launch {
                    viewModel.currentFilters.collect { filters ->
                        updateFilterValue(filters)
                    }
                }

                launch {
                    viewModel.isRecipeSavedToDb.collect {
                        isRecipeAlreadySaved = it
                        requireActivity().invalidateMenu()
                    }
                }
            }
        }
    }

    private fun updateFilterValue(filters: RecipeFilters) {

        // This observation is mostly if you need to react to filter changes
        // directly in the UI, other than just re-fetching.
        // The main use is for pre-filling the bottom sheet.
        Log.d("RandomRecipeFragment", "Current filters updated: $filters")
        currentRegionFilter = filters.region
        currentIngredientsFilter = filters.ingredients
        currentOtherConsiderationsFilter = filters.otherConsiderations
    }

    private fun updateImageUi(imageResult: ImageResult) {

        isRecipeRefreshing = imageResult.isLoading
        requireActivity().invalidateMenu()

        if (imageResult.isLoading) {

            binding.imageViewRecipe.visibility = View.INVISIBLE
            binding.divider2.visibility = View.VISIBLE
            binding.progressBarLoading.visibility = View.VISIBLE
        } else {

            binding.progressBarLoading.visibility = View.INVISIBLE
            binding.imageViewRecipe.visibility = View.VISIBLE
            binding.divider2.visibility = View.INVISIBLE

            if (imageResult.image != null) {

                Glide.with(requireContext())
                    .load(imageResult.image)
                    .placeholder(R.drawable.food_recipe_img) // Your placeholder
                    .error(R.drawable.ic_broken_image) // Error placeholder
                    .into(binding.imageViewRecipe)
            } else {
                // Use a default placeholder if no image URL
                binding.imageViewRecipe.setImageResource(R.drawable.food_recipe_img)
            }
        }
    }

    private fun updateRecipeUI(recipeUiState: RecipeUiState) {

        if (recipeUiState.errorMessage != null) {
            Toast.makeText(requireContext(), recipeUiState.errorMessage, Toast.LENGTH_LONG)
                .show()
            binding.textViewRecipeName.text = getString(R.string.error)
        } else {
            binding.textViewRecipeName.text = recipeUiState.recipeName
            binding.textViewValueYield.text = recipeUiState.yield
            binding.textViewValuePrepTime.text = recipeUiState.prepTime
            binding.textViewValueCookTime.text = recipeUiState.cookTime
            binding.textViewIngredientsAndInstructions.displayMarkdownWithMarkwon(
                requireContext(),
                recipeUiState.ingredientsAndInstruction
            )
        }
    }

    private fun initMenu() {

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {

                menuInflater.inflate(R.menu.random_recipe_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                val saveItem = menu.findItem(R.id.fr_menu_save)

                saveItem.isVisible = !isRecipeRefreshing

                if (isRecipeAlreadySaved) {
                    saveItem.setIcon(R.drawable.ic_bookmark_fill)
                } else {
                    saveItem.setIcon(R.drawable.ic_bookmark_no_fill)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.fr_menu_filter -> {

                        if (!isRecipeRefreshing) {
                            showFilterBottomSheet()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Recipe fetching in progress...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.fr_menu_refresh -> {

                        if (!isRecipeRefreshing) {
                            viewModel.refreshRecipeClicked()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Recipe fetching in progress...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.fr_menu_save -> {
                        viewModel.saveOrDeleteRecipeFromDbBasedOnBookmarkMenuPressed()
                        true
                    }

                    R.id.fr_menu_show_list -> {
                        if (!isRecipeRefreshing) {
                            showSavedRecipesBottomSheet()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Recipe fetching in progress...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add provider with lifecycle awareness

    }

    private fun showFilterBottomSheet() {
        val filterBottomSheet = FilterRandomRecipeBottomSheet.newInstance(
            currentRegionFilter,
            currentIngredientsFilter,
            currentOtherConsiderationsFilter
        )
        filterBottomSheet.show(parentFragmentManager, FilterRandomRecipeBottomSheet.TAG)
    }

    private fun showSavedRecipesBottomSheet() {
        val savedRecipesSheet = SavedRecipesBottomSheet.newInstance()
        savedRecipesSheet.show(parentFragmentManager, SavedRecipesBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}