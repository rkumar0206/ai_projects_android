package com.rtb.ai.projects.ui.feature_the_random_value.feature_programming.feature_random_dsa_problem.bottomsheet

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.LayoutBottomSheetDsaFilterBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_programming.feature_random_dsa_problem.RandomDSAProblemFilter

class DSAFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetDsaFilterBinding? = null
    private val binding get() = _binding!!

    // To pre-fill the form if editing an existing filter
    private var currentFilter: RandomDSAProblemFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_CURRENT_FILTER, RandomDSAProblemFilter::class.java)
            } else {
                @Suppress("DEPRECATION") // Suppress warning for the older API call
                it.getSerializable(ARG_CURRENT_FILTER) as? RandomDSAProblemFilter
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutBottomSheetDsaFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        prefillForm()
        setupApplyButton()
    }

    private fun setupDropdowns() {
        // Data Structure Types
        val dsAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.data_structure_types)
        )
        binding.actvDataStructure.setAdapter(dsAdapter)

        // Algorithm Types
        val algoAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.algorithm_types)
        )
        binding.actvAlgorithmType.setAdapter(algoAdapter)

        // Languages
        val langAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.programming_languages)
        )
        binding.actvLanguage.setAdapter(langAdapter)

        // Complexity
        val complexityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            resources.getStringArray(R.array.complexity_levels)
        )
        binding.actvComplexity.setAdapter(complexityAdapter)
    }

    private fun prefillForm() {
        currentFilter?.let { filter ->
            setSelectedDropdownItem(binding.actvDataStructure, filter.dataStructureType)
            setSelectedDropdownItem(binding.actvAlgorithmType, filter.algorithmType)
            setSelectedDropdownItem(binding.actvLanguage, filter.language)
            setSelectedDropdownItem(binding.actvComplexity, filter.complexity)
            binding.etOtherConsiderations.setText(filter.otherConsiderations)
        }
    }

    private fun setSelectedDropdownItem(
        autoCompleteTextView: AutoCompleteTextView,
        value: String?
    ) {
        value?.let {
            val adapter = autoCompleteTextView.adapter
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == it) {
                    autoCompleteTextView.setText(adapter.getItem(i).toString(), false)
                    break
                }
            }
        }
    }


    private fun setupApplyButton() {
        binding.buttonApplyFilter.setOnClickListener {
            val dsType = getSelectedValue(
                binding.actvDataStructure.text.toString(),
                resources.getStringArray(R.array.data_structure_types)[0]
            )
            val algoType = getSelectedValue(
                binding.actvAlgorithmType.text.toString(),
                resources.getStringArray(R.array.algorithm_types)[0]
            )
            val lang = getSelectedValue(
                binding.actvLanguage.text.toString(),
                resources.getStringArray(R.array.programming_languages)[0]
            )
            val complexity = getSelectedValue(
                binding.actvComplexity.text.toString(),
                resources.getStringArray(R.array.complexity_levels)[0]
            )
            val otherConsiderations =
                binding.etOtherConsiderations.text.toString().trim().takeIf { it.isNotEmpty() }

            val filter = RandomDSAProblemFilter(
                dataStructureType = dsType,
                algorithmType = algoType,
                language = lang,
                complexity = complexity,
                otherConsiderations = otherConsiderations
            )

            setFragmentResult(REQUEST_KEY_DSA_FILTER, bundleOf(BUNDLE_KEY_DSA_FILTER to filter))
            dismiss()
        }
    }

    private fun getSelectedValue(selectedValue: String, defaultValue: String): String? {
        return if (selectedValue.isNotEmpty() && selectedValue != defaultValue) {
            selectedValue
        } else {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DSAFilterBottomSheet"
        const val REQUEST_KEY_DSA_FILTER = "dsaFilterRequestKey"
        const val BUNDLE_KEY_DSA_FILTER = "dsaFilterBundleKey"
        private const val ARG_CURRENT_FILTER = "currentFilter"

        fun newInstance(currentFilter: RandomDSAProblemFilter? = null): DSAFilterBottomSheet {
            val fragment = DSAFilterBottomSheet()
            currentFilter?.let {
                val args = Bundle()
                args.putSerializable(ARG_CURRENT_FILTER, it)
                fragment.arguments = args
            }
            return fragment
        }
    }
}
