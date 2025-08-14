package com.rtb.ai.projects.ui.feature_the_random_value.feature_programming.feature_random_dsa_problem

import android.os.Build
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
import com.rtb.ai.projects.R
import com.rtb.ai.projects.databinding.FragmentRandomDSAProblemBinding
import com.rtb.ai.projects.ui.feature_the_random_value.feature_programming.feature_random_dsa_problem.bottomsheet.DSAFilterBottomSheet
import com.rtb.ai.projects.util.AppUtil.copyToClipboard
import com.rtb.ai.projects.util.AppUtil.displayMarkdownWithMarkwon
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RandomDSAProblemFragment : Fragment() {

    private var _binding: FragmentRandomDSAProblemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RandomDSAProblemViewModel by viewModels()
    private var isLoading: Boolean = false
    private var currentFilter: RandomDSAProblemFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Listen for the result from the bottom sheet
        setFragmentResultListener(DSAFilterBottomSheet.REQUEST_KEY_DSA_FILTER) { requestKey, bundle ->
            if (requestKey == DSAFilterBottomSheet.REQUEST_KEY_DSA_FILTER) {
                val filter: RandomDSAProblemFilter? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bundle.getSerializable(
                        DSAFilterBottomSheet.BUNDLE_KEY_DSA_FILTER,
                        RandomDSAProblemFilter::class.java
                    )
                } else {
                    @Suppress("DEPRECATION") // Suppress warning for the older API call
                    bundle.getSerializable(DSAFilterBottomSheet.BUNDLE_KEY_DSA_FILTER) as? RandomDSAProblemFilter
                }

                filter?.let {
                    // Pass the filter to your ViewModel
                    viewModel.applyFilter(it)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomDSAProblemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMenu()
        observeViewModel()

        binding.buttonCopyDsaProblem.setOnClickListener {
            binding.textViewDsaProblem.text.toString().copyToClipboard(requireContext())
        }

    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collect { uiState ->
                        updateUI(uiState)
                    }
                }
//
//                launch {
//                    viewModel.isStorySavedToDb.collect {
//                        isStoryAlreadySaved = it
//                        requireActivity().invalidateMenu()
//                    }
//                }
            }
        }

    }

    private fun updateUI(uiState: RandomDSAUiState) {

        isLoading = uiState.isLoading

        if (isLoading) {

            binding.progressBar.isVisible = true
            binding.textViewDsaProblem.isVisible = false
            binding.buttonCopyDsaProblem.isVisible = false
        } else {

            binding.progressBar.isVisible = false
            binding.textViewDsaProblem.isVisible = true
            binding.buttonCopyDsaProblem.isVisible = true

            binding.textViewDsaProblem.displayMarkdownWithMarkwon(
                requireContext(),
                uiState.problemStatement ?: "Unable to load problem statement"
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                binding.textViewDsaProblem.text = uiState.errorMessage
            }
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

//                val saveItem = menu.findItem(R.id.fr_menu_save)
//
//                saveItem.isVisible = !isLoading
//
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
                            showFilterBottomSheet()
                        }else {
                            inProgressToast()
                        }

                        true
                    }

                    R.id.fr_menu_refresh -> {

                        if (!isLoading) {
                            viewModel.generateProblem()
                        } else {
                            inProgressToast()
                        }
                        true
                    }

                    R.id.fr_menu_save -> {


                        true
                    }

                    R.id.fr_menu_show_list -> {

                        true
                    }

                    R.id.fr_menu_download -> {

                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED) // Add provider with lifecycle awareness

    }

    private fun showFilterBottomSheet() {

        DSAFilterBottomSheet.newInstance(currentFilter).show(parentFragmentManager, DSAFilterBottomSheet.TAG)
    }

    private fun inProgressToast() {

        Toast.makeText(
            requireContext(),
            "Problem generation in progress",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}