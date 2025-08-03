package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story

import android.os.Bundle
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.FragmentRandomStoryBinding
import com.rtb.ai.projects.ui.adapters.ContentAdapter
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.bottomsheet.StoryInputDialogFragment
import com.rtb.ai.projects.util.constant.Constants.LOADING
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RandomStoryFragment : Fragment() {

    private var _binding: FragmentRandomStoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomStoryViewModel by viewModels()
    private lateinit var contentAdapter: ContentAdapter
    private var isLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentAdapter = ContentAdapter(onGenerateImage = { itemId, prompt ->
            viewModel.generateImageForItem(itemId, prompt)
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomStoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMenu()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.contentRecyclerView.apply {
            adapter = contentAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    updateUI(uiState)
                }
            }
        }
    }

    private fun updateUI(uiState: RandomStoryUIState) {

        isLoading = uiState.isLoading

        if (uiState.isLoading) {

            binding.progressBarLoading.visibility = View.VISIBLE
            binding.contentRecyclerView.visibility = View.GONE
            binding.storyTitleTV.text = LOADING
        } else {

            binding.progressBarLoading.visibility = View.GONE
            binding.contentRecyclerView.visibility = View.VISIBLE

            binding.storyTitleTV.text = uiState.storyTitle
        }

        if (!isLoading && !uiState.isImageLoading) {
            contentAdapter.submitList(uiState.storyContent)
        }
    }

    private fun initMenu() {

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {

            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(R.menu.random_generator_shared_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                val saveItem = menu.findItem(R.id.fr_menu_save)

                saveItem.isVisible = !isLoading

//                if (isImageAlreadySaved) {
//                    saveItem.setIcon(R.drawable.ic_bookmark_fill)
//                } else {
//                    saveItem.setIcon(R.drawable.ic_bookmark_no_fill)
//                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.fr_menu_filter -> {

                        if (!isLoading) {
                            showStoryInputDialog()
                        } else {
                            inProgressToast()
                        }
                        true
                    }

                    R.id.fr_menu_refresh -> {

                        if (!isLoading) {
                            viewModel.generateRandomStory(null)
                        } else {
                            inProgressToast()
                        }

                        true
                    }

                    R.id.fr_menu_save -> {

//                        if (!isLoading) {
//
//                            viewModel.saveOrDeleteAIImageFromDbBasedOnBookmarkMenuPressed()
//                        } else {
//                            showImageGenerationInProgressToast()
//                        }

                        true
                    }

                    R.id.fr_menu_show_list -> {

//                        if (!isLoading) {
//                            showSavedImagesBottomSheet()
//                        } else {
//                            showImageGenerationInProgressToast()
//                        }

                        true
                    }

                    R.id.fr_menu_download -> {

//                        if (!isLoading) {
//                            viewModel.downloadCurrentImage()
//                        }else{
//                            showImageGenerationInProgressToast()
//                        }

                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add provider with lifecycle awareness

    }

    // In your Fragment
    private fun showStoryInputDialog() {
        val dialog = StoryInputDialogFragment.newInstance()
        dialog.setOnSubmitListener { storyInput ->
            viewModel.generateRandomStory(storyInput)
        }
        dialog.show(parentFragmentManager, StoryInputDialogFragment.TAG)
    }

    private fun inProgressToast() {

        Toast.makeText(
            requireContext(),
            "Story generation in progress",
            Toast.LENGTH_SHORT
        ).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.contentRecyclerView.adapter = null // Clear adapter to avoid leaks
        _binding = null
    }

}
