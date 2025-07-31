package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.databinding.BottomSheetImageKeywordsBinding // Your generated binding class

class ImageKeywordsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetImageKeywordsBinding? = null
    private val binding get() = _binding!!

    private var initialKeywords: String? = null

    companion object {
        const val TAG = "ImageKeywordsBottomSheet"
        const val REQUEST_KEY_KEYWORDS = "requestKeywords"
        const val BUNDLE_KEY_KEYWORDS_STRING = "bundleKeywordsString"
        private const val ARG_INITIAL_KEYWORDS = "argInitialKeywords"

        fun newInstance(currentKeywords: String? = null): ImageKeywordsBottomSheet {
            val fragment = ImageKeywordsBottomSheet()
            val args = Bundle()
            args.putString(ARG_INITIAL_KEYWORDS, currentKeywords)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialKeywords = it.getString(ARG_INITIAL_KEYWORDS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetImageKeywordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialKeywords?.let {
            binding.editTextKeywords.setText(it)
        }

        binding.buttonApplyKeywords.setOnClickListener {
            val keywords = binding.editTextKeywords.text.toString().trim()
            setFragmentResult(REQUEST_KEY_KEYWORDS, bundleOf(BUNDLE_KEY_KEYWORDS_STRING to keywords))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
