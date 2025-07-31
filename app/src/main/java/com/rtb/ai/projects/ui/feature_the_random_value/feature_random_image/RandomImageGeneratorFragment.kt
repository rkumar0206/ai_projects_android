package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image

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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.FragmentRandomImageGeneratorBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet.ImageKeywordsBottomSheet
import com.rtb.ai.projects.util.AppUtil.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RandomImageGeneratorFragment : Fragment() {

    private var _binding: FragmentRandomImageGeneratorBinding? = null
    private val binding get() = _binding!!
    private var currentKeywords: String = ""
    private val viewModel: RandomImageGenerationViewModel by viewModels()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for results from the ImageKeywordsBottomSheet
        setFragmentResultListener(ImageKeywordsBottomSheet.REQUEST_KEY_KEYWORDS) { requestKey, bundle ->
            if (requestKey == ImageKeywordsBottomSheet.REQUEST_KEY_KEYWORDS) {
                val receivedKeywords =
                    bundle.getString(ImageKeywordsBottomSheet.BUNDLE_KEY_KEYWORDS_STRING, "")
                currentKeywords = receivedKeywords
                viewModel.generateImagePromptAndImage(currentKeywords)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomImageGeneratorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textViewImagePrompt.text = ""

        binding.imageViewGenerated.setOnClickListener {
            togglePromptVisibility()
        }

        binding.imageViewCopyPrompt.setOnClickListener {
            val promptText = binding.textViewImagePrompt.text.toString()
            if (promptText.isNotEmpty()) {
                copyToClipboard(promptText)
            }
        }

        initMenu()
        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        updateUI(uiState)
                    }
                }

                launch {
                    viewModel.keywordsFilter.collect { currentKeywords = it }
                }
            }
        }
    }

    private fun updateUI(uiState: RandomImageGenerationUIState) {

        binding.progressBarLoading.isVisible = uiState.isLoading
        isLoading = uiState.isLoading

        if (uiState.image != null) {

            Glide.with(requireContext())
                .load(uiState.image)
                .placeholder(R.drawable.generate_random_image) // Your placeholder
                .error(R.drawable.ic_broken_image) // Error placeholder
                .into(binding.imageViewGenerated)

            binding.textViewImagePrompt.text = uiState.imagePrompt
            binding.groupTextElements.visibility = View.VISIBLE

        } else {
            // Use a default placeholder if no image URL
            binding.imageViewGenerated.setImageResource(R.drawable.generate_random_image)
        }
    }

    private fun togglePromptVisibility() {
        if (binding.groupTextElements.isVisible) {
            binding.groupTextElements.visibility = View.GONE
        } else {
            binding.groupTextElements.visibility = View.VISIBLE
        }
    }

    private fun copyToClipboard(textToCopy: String) {

        textToCopy.copyToClipboard(requireContext())
        Toast.makeText(requireContext(), "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
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

//                val saveItem = menu.findItem(R.id.fr_menu_save)
//
//                saveItem.isVisible = !isRecipeRefreshing
//
//                if (isRecipeAlreadySaved) {
//                    saveItem.setIcon(R.drawable.ic_bookmark_fill)
//                } else {
//                    saveItem.setIcon(R.drawable.ic_bookmark_no_fill)
//                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.fr_menu_filter -> {

                        if (!isLoading) {
                            showKeywordsBottomSheet()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Image generation in progress",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.fr_menu_refresh -> {

                        if (!isLoading) {
                            viewModel.generateImagePromptAndImage(currentKeywords)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Image generation in progress",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.fr_menu_save -> {
                        true
                    }

                    R.id.fr_menu_show_list -> {
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add provider with lifecycle awareness

    }

    private fun showKeywordsBottomSheet() {
        val keywordsSheet =
            ImageKeywordsBottomSheet.newInstance(currentKeywords) // Pass current keywords
        keywordsSheet.show(parentFragmentManager, ImageKeywordsBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to avoid memory leaks
    }
}
