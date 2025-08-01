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
import com.rtb.ai.projects.databinding.BottomsheetSavedItemsBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.RandomImageGenerationViewModel
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.adapter.SavedImagesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SavedImagesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetSavedItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomImageGenerationViewModel by activityViewModels()
    private lateinit var savedImagesAdapter: SavedImagesAdapter

    companion object {
        const val TAG = "SavedImagesBottomSheet"
        const val REQUEST_KEY_IMAGE_CLICKED = "requestImageClicked"
        const val BUNDLE_KEY_IMAGE_ID = "bundleImageId"

        fun newInstance(): SavedImagesBottomSheet {
            return SavedImagesBottomSheet()
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

        setupRecyclerView()
        observeViewModel()

        // Fetch all Images when the BottomSheet is created/shown
        // viewModel.getAllImages() // Assuming you have this method in your ViewModel
        // Or it's a Flow that you collect
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
                // Observe the list of all Images from the ViewModel
                viewModel.allImages.collect { images -> // Assuming Flow named 'allSavedImages'
                    Log.d(TAG, "Saved images received: ${images.size}")
                    if (images.isEmpty()) {
                        binding.recyclerViewSavedItems.visibility = View.GONE
                        binding.textViewEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.recyclerViewSavedItems.visibility = View.VISIBLE
                        binding.textViewEmptyState.visibility = View.GONE
                    }
                    savedImagesAdapter.submitList(images)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
