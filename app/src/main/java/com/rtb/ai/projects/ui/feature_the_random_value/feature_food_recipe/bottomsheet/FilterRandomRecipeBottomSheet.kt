package com.rtb.ai.projects.ui.feature_the_random_value.feature_food_recipe.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.databinding.BottomsheetFilterRandomRecipeBinding

class FilterRandomRecipeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetFilterRandomRecipeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFilterRandomRecipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill fields if needed (e.g., from arguments)
        binding.editTextRegion.setText(arguments?.getString(ARG_REGION))
        binding.editTextIngredients.setText(arguments?.getString(ARG_INGREDIENTS))
        binding.editTextOtherConsiderations.setText(arguments?.getString(ARG_OTHER_CONSIDERATIONS))

        binding.buttonApplyFilters.setOnClickListener {
            val region = binding.editTextRegion.text.toString().trim()
            val ingredients = binding.editTextIngredients.text.toString().trim()
            val otherConsiderations = binding.editTextOtherConsiderations.text.toString().trim()

            // Pass the data back to the calling fragment
            setFragmentResult(
                REQUEST_KEY_FILTER, bundleOf(
                    BUNDLE_KEY_REGION to region,
                    BUNDLE_KEY_INGREDIENTS to ingredients,
                    BUNDLE_KEY_OTHER_CONSIDERATIONS to otherConsiderations
                )
            )
            dismiss() // Close the bottom sheet
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterRandomRecipeBottomSheet"
        const val REQUEST_KEY_FILTER = "filterRequestKey"
        const val BUNDLE_KEY_REGION = "bundleRegion"
        const val BUNDLE_KEY_INGREDIENTS = "bundleIngredients"
        const val BUNDLE_KEY_OTHER_CONSIDERATIONS = "bundleOtherConsiderations"

        // Optional: To pre-fill the bottom sheet if it's opened again
        private const val ARG_REGION = "argRegion"
        private const val ARG_INGREDIENTS = "argIngredients"
        private const val ARG_OTHER_CONSIDERATIONS = "argOtherConsiderations"

        fun newInstance(
            currentRegion: String? = null,
            currentIngredients: String? = null,
            currentOtherConsiderations: String? = null
        ): FilterRandomRecipeBottomSheet {
            val fragment = FilterRandomRecipeBottomSheet()
            fragment.arguments = bundleOf(
                ARG_REGION to currentRegion,
                ARG_INGREDIENTS to currentIngredients,
                ARG_OTHER_CONSIDERATIONS to currentOtherConsiderations
            )
            return fragment
        }
    }
}