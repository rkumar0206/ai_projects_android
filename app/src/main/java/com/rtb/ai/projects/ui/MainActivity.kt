package com.rtb.ai.projects.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import com.rtb.ai.projects.R
import com.rtb.ai.projects.data.model.AIProjectCategory
import com.rtb.ai.projects.databinding.ActivityMainBinding
import com.rtb.ai.projects.ui.adapters.AIProjectsCategoriesAdapter
import com.rtb.ai.projects.ui.feature_prompt_refiner.PromptRefinerActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var projectCategoryAdapter: AIProjectsCategoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRecyclerView()

        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)


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

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}