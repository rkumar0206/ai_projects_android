package com.rtb.ai.projects.ui.feature_prompt_refiner

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rtb.ai.projects.databinding.ActivityPromptRefinerBinding
import com.rtb.ai.projects.util.AppUtil.copyToClipboard
import com.rtb.ai.projects.util.AppUtil.displayMarkdownWithMarkwon
import com.rtb.ai.projects.util.AppUtil.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PromptRefinerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPromptRefinerBinding
    private val viewModel: PromptRefinerViewModel by viewModels() // Hilt ViewModel Injection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //enableEdgeToEdge()

        binding = ActivityPromptRefinerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Optional: Enable the Up button (back arrow) if you have a parent activity
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Make the TextView scrollable
        binding.textViewResult.movementMethod = ScrollingMovementMethod() // <-- Add this line


        setupViews()
        observeUiState()
    }


    private fun setupViews() {
        binding.promptET.doAfterTextChanged { text ->
            viewModel.onPromptChanged(text.toString())
        }

        binding.buttonSubmitPrompt.setOnClickListener {
            hideKeyboard(this)
            viewModel.onSubmitClicked()
        }

        binding.imageViewCopyResult.setOnClickListener {
            val textToCopy = binding.textViewResult.text.toString()
            textToCopy.copyToClipboard(this)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { // Collect when STARTED or RESUMED
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: PromptRefinerUiState) {
        // Update EditText only if the text is different to avoid cursor jumps
        if (binding.promptET.text.toString() != state.promptInput) {
            binding.promptET.setText(state.promptInput)
            binding.promptET.setSelection(state.promptInput.length) // Move cursor to end
        }

        binding.textViewResult.displayMarkdownWithMarkwon(this, state.resultText)
        binding.buttonSubmitPrompt.isEnabled = state.isSubmitButtonEnabled
        binding.buttonSubmitPrompt.visibility =
            if (state.isLoading) View.INVISIBLE else View.VISIBLE

        binding.progressBarLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        binding.imageViewCopyResult.visibility =
            if (state.isCopyButtonVisible) View.VISIBLE else View.GONE
        binding.textViewResult.visibility =
            if (state.resultText.isNotEmpty()) View.VISIBLE else View.GONE

        state.errorMessage?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            viewModel.onErrorMessageShown() // Notify ViewModel that error was shown
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}