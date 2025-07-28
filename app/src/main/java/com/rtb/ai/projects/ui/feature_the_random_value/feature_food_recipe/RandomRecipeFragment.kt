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
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.FragmentRandomRecipeBinding
import com.rtb.ai.projects.util.AppUtil.displayMarkdownWithMarkwon
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RandomRecipeFragment : Fragment() {

    private var _binding: FragmentRandomRecipeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomRecipeViewModel by viewModels()

    // Store current filter values if you want to pre-fill the bottom sheet next time
    private var currentRegionFilter: String? = null
    private var currentIngredientsFilter: String? = null
    private var currentOtherConsiderationsFilter: String? = null

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
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.recipeUiState.observe(viewLifecycleOwner) { state ->

            if (state.isLoading) {

                binding.imageViewRecipe.visibility = View.INVISIBLE
                binding.progressBarLoading.visibility = View.VISIBLE
                binding.divider2.visibility = View.VISIBLE

            } else {

                binding.imageViewRecipe.visibility = View.VISIBLE
                binding.progressBarLoading.visibility = View.INVISIBLE
                binding.divider2.visibility = View.INVISIBLE

                // binding.progressBarRecipe.visibility = View.GONE
                if (state == null || state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                    // Optionally display error message in a TextView
                    binding.textViewRecipeName.text = "Error"
                    // ...
                } else {
                    binding.textViewRecipeName.text = state.recipeName
                    binding.textViewValueYield.text = state.yield
                    binding.textViewValuePrepTime.text = state.prepTime
                    binding.textViewValueCookTime.text = state.cookTime
                    binding.textViewIngredientsAndInstructions.displayMarkdownWithMarkwon(requireContext(), state.ingredientsAndInstruction)

                    if (state.imageUrl != null) {
                        Glide.with(this)
                            .load(state.imageUrl)
                            .placeholder(R.drawable.food_recipe_img) // Your placeholder
                            .error(R.drawable.ic_broken_image) // Error placeholder
                            .into(binding.imageViewRecipe)

                        binding.imageViewRecipe.visibility = View.VISIBLE
                    } else {
                        // Use a default placeholder if no image URL
                        binding.imageViewRecipe.setImageResource(R.drawable.food_recipe_img)
                    }
                }
            }
        }

        viewModel.currentFilters.observe(viewLifecycleOwner) { filters ->
            // This observation is mostly if you need to react to filter changes
            // directly in the UI, other than just re-fetching.
            // The main use is for pre-filling the bottom sheet.
            Log.d("RandomRecipeFragment", "Current filters updated: $filters")
            currentRegionFilter = filters.region
            currentIngredientsFilter = filters.ingredients
            currentOtherConsiderationsFilter = filters.otherConsiderations
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

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.fr_menu_filter -> {

                        showFilterBottomSheet()
                        true
                    }

                    R.id.fr_menu_refresh -> {

                        viewModel.resetRecipeUiState()

                        // todo: refresh recipes
                        viewModel.refreshRecipe()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}