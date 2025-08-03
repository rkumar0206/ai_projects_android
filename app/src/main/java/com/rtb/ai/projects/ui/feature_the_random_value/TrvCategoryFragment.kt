package com.rtb.ai.projects.ui.feature_the_random_value

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIProjectCategory
import com.rtb.ai.projects.databinding.FragmentTrvCategoryBinding
import com.rtb.ai.projects.ui.adapters.AIProjectsCategoriesAdapter

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class TrvCategoryFragment : Fragment() {

    private var _binding: FragmentTrvCategoryBinding? = null
    private lateinit var projectCategoryAdapter: AIProjectsCategoriesAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrvCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
    }

    private fun initRecyclerView() {

        projectCategoryAdapter = AIProjectsCategoriesAdapter(
            getCategories(),
            onItemClick = { category: AIProjectCategory ->
                openRespectiveCategory(category)
            }
        )
        binding.trvCategoryRV.apply {
            adapter = projectCategoryAdapter
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun openRespectiveCategory(category: AIProjectCategory) {

        when (category.id) {

            0 -> {
                // Navigate to RandomRecipeFragment
                // Use the action ID defined in your navigation graph
                findNavController().navigate(R.id.action_TrvCategoryFragment_to_randomRecipeFragment)
            }

            1 -> {
                findNavController().navigate(R.id.action_TrvCategoryFragment_to_imageGenerationCategoryFragment)
            }

            2 -> {
                findNavController().navigate(R.id.action_TrvCategoryFragment_to_randomStoryFragment)
            }

        }

    }

    private fun getCategories(): List<AIProjectCategory> {

        return listOf(
            AIProjectCategory(0, R.drawable.food_recipe_img, getString(R.string.food_recipe)),
            AIProjectCategory(1, R.drawable.images_img, getString(R.string.images)),
            AIProjectCategory(2, R.drawable.stories_img, getString(R.string.stories)),
            AIProjectCategory(3, R.drawable.github_img, getString(R.string.github)),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}