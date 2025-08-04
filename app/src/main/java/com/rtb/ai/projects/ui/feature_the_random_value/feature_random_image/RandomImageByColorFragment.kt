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
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet.ColorPickerBottomSheet
import com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet.SavedImagesBottomSheet
import com.rtb.ai.projects.util.AppUtil
import com.rtb.ai.projects.util.AppUtil.copyToClipboard
import com.rtb.ai.projects.util.constant.ImageCategoryTag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RandomImageByColorFragment : Fragment() {

    private var _binding: FragmentRandomImageGeneratorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RandomImageGenerationViewModel by viewModels()
    private var isLoading = false
    private var isImageAlreadySaved = false
    private var currentSelectedColors: List<Int> = emptyList() // Or initialize with defaults

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the listener for the result from ColorPickerBottomSheet
        setFragmentResultListener(ColorPickerBottomSheet.REQUEST_KEY_COLORS) { requestKey, bundle ->
            if (requestKey == ColorPickerBottomSheet.REQUEST_KEY_COLORS) {
                val colors =
                    bundle.getIntegerArrayList(ColorPickerBottomSheet.BUNDLE_KEY_SELECTED_COLORS)
                if (colors != null) {
                    currentSelectedColors = colors
                    viewModel.generateImageByColors(currentSelectedColors)
                }
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

        viewModel.setImageGenerationTag(ImageCategoryTag.IMAGE_GENERATION_BY_COLOR_TAG)

        binding.textViewImagePrompt.text = ""
        binding.imageViewGenerated.setOnClickListener { togglePromptVisibility() }

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
                    viewModel.selectedColorsStateFlow.collect { currentSelectedColors = it }
                }

                launch {
                    viewModel.isAIImageSavedToDb.collect {
                        isImageAlreadySaved = it
                        requireActivity().invalidateMenu()
                    }
                }
            }
        }
    }

    private fun updateUI(uiState: RandomImageGenerationUIState) {

        binding.progressBarLoading.isVisible = uiState.isLoading
        isLoading = uiState.isLoading
        requireActivity().invalidateMenu()

        if (uiState.isLoading) {
            binding.imageViewGenerated.setImageResource(R.drawable.generate_random_image)
            binding.textViewImagePrompt.text = ""
            binding.groupTextElements.visibility = View.GONE
        }

        if (!uiState.imageFilePath.isNullOrBlank()) {

            Glide.with(requireContext())
                .load(AppUtil.retrieveImageAsByteArray(uiState.imageFilePath))
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

                val saveItem = menu.findItem(R.id.fr_menu_save)

                saveItem.isVisible = !isLoading

                if (isImageAlreadySaved) {
                    saveItem.setIcon(R.drawable.ic_bookmark_fill)
                } else {
                    saveItem.setIcon(R.drawable.ic_bookmark_no_fill)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.fr_menu_filter -> {

                        if (!isLoading) {
                            showColorPickerBottomSheet()
                        } else {
                            showImageGenerationInProgressToast()
                        }
                        true
                    }

                    R.id.fr_menu_refresh -> {

                        if (!isLoading) {
                            viewModel.generateImageByColors(currentSelectedColors)
                        } else {
                            showImageGenerationInProgressToast()
                        }
                        true
                    }

                    R.id.fr_menu_save -> {

                        if (!isLoading) {
                            viewModel.saveOrDeleteAIImageFromDbBasedOnBookmarkMenuPressed()
                        } else {
                            showImageGenerationInProgressToast()
                        }

                        true
                    }

                    R.id.fr_menu_show_list -> {

                        if (!isLoading) {
                            showSavedImagesBottomSheet()
                        } else {
                            showImageGenerationInProgressToast()
                        }

                        true
                    }

                    R.id.fr_menu_download -> {

                        if (!isLoading) {
                            viewModel.downloadCurrentImage()
                        } else {
                            showImageGenerationInProgressToast()
                        }

                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add provider with lifecycle awareness

    }

    private fun showImageGenerationInProgressToast() {

        Toast.makeText(
            requireContext(),
            "Image generation in progress",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Add a method to show the bottom sheet
    private fun showColorPickerBottomSheet() {
        val bottomSheet = ColorPickerBottomSheet.newInstance(
            currentSelectedColors.ifEmpty { null }
        )
        bottomSheet.show(parentFragmentManager, ColorPickerBottomSheet.TAG)
    }

    private fun showSavedImagesBottomSheet() {
        val savedImageSheet = SavedImagesBottomSheet.newInstance(ImageCategoryTag.IMAGE_GENERATION_BY_COLOR_TAG)
        savedImageSheet.show(parentFragmentManager, SavedImagesBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to avoid memory leaks
    }
}