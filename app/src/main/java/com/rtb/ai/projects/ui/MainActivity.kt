package com.rtb.ai.projects.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIProjectCategory
import com.rtb.ai.projects.databinding.ActivityMainBinding
import com.rtb.ai.projects.ui.adapters.AIProjectsCategoriesAdapter
import com.rtb.ai.projects.ui.feature_prompt_refiner.PromptRefinerActivity
import com.rtb.ai.projects.ui.feature_the_random_value.TheRandomValueActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectCategoryAdapter: AIProjectsCategoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()

        setSupportActionBar(binding.toolbar)
    }

    private fun initRecyclerView() {

        projectCategoryAdapter = AIProjectsCategoriesAdapter(
            getCategories(),
            onItemClick = { category: AIProjectCategory ->
                openRespectiveCategory(category)
            }
        )

        binding.androidProjectRv.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = projectCategoryAdapter
        }
    }

    private fun openRespectiveCategory(category: AIProjectCategory) {

        when (category.id) {

            0 -> {
                val intent = Intent(this@MainActivity, PromptRefinerActivity::class.java)
                startActivity(intent)
            }

            1 -> {
                val intent = Intent(this@MainActivity, TheRandomValueActivity::class.java)
                startActivity(intent)
            }
        }

    }

    private fun getCategories(): List<AIProjectCategory> {
        return listOf(
            AIProjectCategory(0, R.drawable.prompt_refiner, getString(R.string.prompt_refiner)),
            AIProjectCategory(
                1,
                R.drawable.the_random_value_img,
                getString(R.string.the_random_value)
            )
        )
    }
}