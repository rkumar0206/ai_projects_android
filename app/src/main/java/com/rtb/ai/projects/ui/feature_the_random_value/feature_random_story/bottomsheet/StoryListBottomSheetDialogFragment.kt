package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.data.model.AIStory
import com.rtb.ai.projects.databinding.BottomSheetStoryListBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.RandomStoryViewModel
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.adapter.StoryPreviewAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StoryListBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: RandomStoryViewModel by viewModels()
    private lateinit var storyPreviewAdapter: StoryPreviewAdapter

    private var _binding: BottomSheetStoryListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optional: Set a specific style for the bottom sheet
        // setStyle(DialogFragment.STYLE_NORMAL, R.style.YourBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetStoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewmodel()

        // Example: Load stories (replace with your actual data loading mechanism)
        // This would typically come from a ViewModel observed by this BottomSheet or its parent
        // loadStories()
    }

    private fun observeViewmodel() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allStories().collect {
                    submitStories(it)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        storyPreviewAdapter = StoryPreviewAdapter { story ->
            setFragmentResult(
                REQUEST_KEY_STORY_CLICKED,
                bundleOf(BUNDLE_KEY_STORY_ID to story.id)
            )
            dismiss()
        }

        binding.rvStoryList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = storyPreviewAdapter
        }
    }

    // Call this method to submit data to the adapter
    fun submitStories(stories: List<AIStory>) {
        if (::storyPreviewAdapter.isInitialized) { // Ensure adapter is ready
            storyPreviewAdapter.submitList(stories)
        }
    }

    // Example data loading function - replace with your logic
    // private fun loadStories() {
    //    // Fetch stories from ViewModel or repository
    //    val dummyStories = listOf(
    //        DisplayStory("1", listOf("path1", "path2", "path3"), "Preview for story 1..."),
    //        DisplayStory("2", listOf("pathA", "pathB", "pathC", "pathD"), "Preview for story 2 which is a bit longer...")
    //    )
    //    submitStories(dummyStories)
    // }

    companion object {
        const val TAG = "StoryListBottomSheet"
        const val REQUEST_KEY_STORY_CLICKED = "requestStoryClicked"
        const val BUNDLE_KEY_STORY_ID = "bundleStoryId"
        fun newInstance(): StoryListBottomSheetDialogFragment {
            return StoryListBottomSheetDialogFragment()
        }
    }
}
