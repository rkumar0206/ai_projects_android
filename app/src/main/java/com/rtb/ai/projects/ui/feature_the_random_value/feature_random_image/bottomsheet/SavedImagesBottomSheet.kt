package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet // Adjust package

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
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIImage
import com.rtb.ai.projects.databinding.BottomsheetSavedItemsBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.RandomImageGenerationViewModel
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.adapter.SavedImagesAdapter
import com.rtb.ai.projects.util.constant.IMAGE_CATEGORY_TAG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedImagesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetSavedItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomImageGenerationViewModel by activityViewModels()
    private lateinit var savedImagesAdapter: SavedImagesAdapter
    private lateinit var currentImageCategoryTag: IMAGE_CATEGORY_TAG

    companion object {
        const val TAG = "SavedImagesBottomSheet"
        const val REQUEST_KEY_IMAGE_CLICKED = "requestImageClicked"
        const val BUNDLE_KEY_IMAGE_ID = "bundleImageId"
        const val CURRENT_IMAGE_CATEGORY_TAG = "currentImageCategoryTag"

        fun newInstance(tag: IMAGE_CATEGORY_TAG = IMAGE_CATEGORY_TAG.IMAGE_GENERATION_RANDOM_IMAGE_USING_KEYWORD_TAG): SavedImagesBottomSheet {
            val fragment = SavedImagesBottomSheet()
            val args = Bundle()
            args.putString(CURRENT_IMAGE_CATEGORY_TAG, tag.name)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val currentTag = it.getString(
                CURRENT_IMAGE_CATEGORY_TAG,
                IMAGE_CATEGORY_TAG.IMAGE_GENERATION_RANDOM_IMAGE_USING_KEYWORD_TAG.name
            )
            currentImageCategoryTag = IMAGE_CATEGORY_TAG.valueOf(currentTag)
            viewModel.setImageGenerationTag(IMAGE_CATEGORY_TAG.valueOf(currentTag))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSavedItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewTitle.text = getString(R.string.saved_images)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        savedImagesAdapter = SavedImagesAdapter(
            onItemClick = { imageId ->
                // Pass the imageId back to the RandomImageFragment
                setFragmentResult(
                    REQUEST_KEY_IMAGE_CLICKED,
                    bundleOf(BUNDLE_KEY_IMAGE_ID to imageId)
                )
                dismiss() // Close the bottom sheet
            },
            onDeleteClick = { image ->
                // Call ViewModel's delete method
                viewModel.deleteAIImage(image)
            }
        )
        binding.recyclerViewSavedItems.apply {
            adapter = savedImagesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                when (currentImageCategoryTag) {

                    IMAGE_CATEGORY_TAG.IMAGE_GENERATION_RANDOM_IMAGE_USING_KEYWORD_TAG -> {
                        // Observe the list of all Images from the ViewModel
                        viewModel.allImagesForGeneratedImageCategoryTag.collect { images ->
                            Log.d(TAG, "Saved images received: ${images.size}")
                            updateUI(images)
                            savedImagesAdapter.submitList(images)
                        }
                    }

                    IMAGE_CATEGORY_TAG.IMAGE_GENERATION_BY_COLOR_TAG -> {
                        viewModel.allImagesForGeneratedImageByColorTag.collect { images ->
                            Log.d(TAG, "Saved images received: ${images.size}")
                            updateUI(images)
                            savedImagesAdapter.submitList(images)
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(images: List<AIImage>) {

        if (images.isEmpty()) {
            binding.recyclerViewSavedItems.visibility = View.GONE
            binding.textViewEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewSavedItems.visibility = View.VISIBLE
            binding.textViewEmptyState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
