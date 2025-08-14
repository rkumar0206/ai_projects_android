package com.rtb.ai.projects.ui.feature_the_random_value.feature_programming.feature_random_dsa_problem

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rtb.ai.projects.util.GeminiAiHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.Serializable
import javax.inject.Inject

data class RandomDSAProblemFilter(
    val dataStructureType: String? = null,
    val algorithmType: String? = null,
    val language: String? = null,
    val complexity: String? = null,
    val otherConsiderations: String? = null
) : Serializable

data class RandomDSAUiState(
    var isLoading: Boolean = true,
    var errorMessage: String? = null,
    var problemStatement: String? = null
) : Serializable

@HiltViewModel
class RandomDSAProblemViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val geminiAiHelper: GeminiAiHelper
) : AndroidViewModel(application) {

    val uiState: StateFlow<RandomDSAUiState> =
        savedStateHandle.getStateFlow(PROBLEM_STATEMENT_KEY, RandomDSAUiState(isLoading = true))
    val dsaProblemFilter =
        savedStateHandle.getStateFlow(DATA_STRUCTURE_FILTER_KEY, RandomDSAProblemFilter())
    val alreadyGeneratedProblems: StateFlow<MutableList<String>> = savedStateHandle.getStateFlow(
        ALREADY_GENERATED_PROBLEMS_KEY, mutableListOf()
    )

    init {
        viewModelScope.launch {
            delay(1000)

//            val lastSavedStory = aiStoryRepository.getLastSavedRepository().first()
//
//            if (lastSavedStory != null) {
//                updateUiBySavedStory(lastSavedStory)
//            } else {
//                generateRandomStory(StoryInput(length = "500"))
//            }
            generateProblem()
        }
    }

    fun applyFilter(filter: RandomDSAProblemFilter) {
        onDataStructureFilterChange(filter)
        generateProblem()
    }

    fun generateProblem() {

        viewModelScope.launch {

            onUiStateChanges(RandomDSAUiState(isLoading = true))

            geminiAiHelper.getPromptTextResult(generatePrompt())
                .onSuccess { result ->

                    if (!result.isNullOrBlank()) {
                        val dsaProblem = result

                        Log.i("dsaProblemViewmodel", "generateProblem: \n$result")

                        onUiStateChanges(
                            RandomDSAUiState(
                                isLoading = false,
                                errorMessage = null,
                                problemStatement = dsaProblem
                            )
                        )

                        val problemTitle = extractProblemTitle(dsaProblem)
                        if (!problemTitle.isNullOrBlank()) {
                            val list = alreadyGeneratedProblems.value
                            list.add(problemTitle)
                            savedStateHandle[ALREADY_GENERATED_PROBLEMS_KEY] = list
                        }

                    } else {
                        showError("Error fetching DSA problem")
                    }
                }
                .onFailure { error ->
                    showError(error.message)
                }
        }

    }

    private fun showError(errorMessage: String?) {

        onUiStateChanges(RandomDSAUiState(isLoading = false, errorMessage = errorMessage))
    }

    private fun generatePrompt(): String {

        var prompt =
            "You are an expert in Data Structures and Algorithms (DSA) and a proficient programmer. Your primary task is to act as a DSA problem generator.\n" +
                    "\n" +
                    "**Objective:**\n" +
                    "\n" +
                    "Provide a single, random, popular, and optimal Data Structures and Algorithms (DSA) question.\n" +
                    "\n" +
                    "**Input Parameters:**\n" +
                    "\n" +
                    "*`data_structure_type` (Optional): Specify a specific data structure (e.g., \"Array\", \"Linked List\", \"Tree\", \"Graph\", \"Heap\", \"Hash Map\", \"Stack\", \"Queue\").\n" +
                    "\n" +
                    "*`algorithm_type` (Optional): Specify an algorithm paradigm (e.g., \"Dynamic Programming\", \"Greedy\", \"Sorting\", \"Searching\", \"DFS\", \"BFS\", \"Backtracking\", \"Divide and Conquer\").\n" +
                    "\n" +
                    "*`language` (Optional): Specify the programming language for the solution (e.g., \"C++\", \"Python\", \"Java\", \"JavaScript\", \"Go\"). If this parameter is not provided, default to \"Java\".\n" +
                    "\n" +
                    "*`complexity` (Optional): Specify the programming complexity (e.g., \"Hard\", \"Medium\", \"Random\", \"Easy\"). If this parameter is not provided, default to \"Random\".\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "**Question Selection Criteria:**\n" +
                    "\n" +
                    "*The question must be a well-known and frequently asked problem from various popular DSA sources (e.g., LeetCode, InterviewBit, HackerRank).\n" +
                    "\n" +
                    "*It should be suitable for technical interviews.\n" +
                    "\n" +
                    "*The provided solution must be the *optimal* one in terms of time and space complexity.\n" +
                    "\n" +
                    "*If applicable, explicitly mention popular tech companies (e.g., Google, Amazon, Microsoft, Facebook, Apple) that are known to have asked this question in interviews.\n" +
                    "\n" +
                    "\n" +
                    "**Output Format:**\n" +
                    "\n" +
                    "Strictly adhere to the following markdown format for your response. Do not include any introductory or concluding remarks, conversational text, explanations, or any other information beyond what is specified below.\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "### Problem: [Question Title]\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "**Description:**\n" +
                    "\n" +
                    "[Full problem statement with all necessary details and constraints]\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "**Examples:**\n" +
                    "\n" +
                    "\n" +
                    "[Input Example 1]\n" +
                    "\n" +
                    "[\n\nOutput Example 1]\n" +
                    "\n" +
                    "\n" +
                    "[Input Example 2]\n" +
                    "\n" +
                    "[\n\nOutput Example 2]\n" +
                    "\n" +
                    "\n" +
                    "... (Include at least two illustrative examples, or more if the problem complexity requires)\n" +
                    "\n" +
                    "\n" +
                    "\n" +
                    "**Optimal Solution:**\n" +
                    "\n" +
                    "```[language_name]\n" +
                    "\n" +
                    "[Complete optimal code implementation of the solution, including necessary imports and class/function definitions]\n" +
                    "\n" +
                    "```\n" +
                    "\n" +
                    "\n" +
                    "**Companies Asked In:**\n" +
                    "\n" +
                    "[List of popular companies where this question has been asked, e.g., Google, Amazon, Microsoft]\n"

        dsaProblemFilter.value.let { filter ->

            prompt += "\n\n--- User Provided Inputs ---"

            filter.dataStructureType?.let {
                prompt += "\nData Structure Type: $it"
            }

            filter.algorithmType?.let {
                prompt += "\nAlgorithm Type: $it"
            }

            filter.language?.let {
                prompt += "\nLanguage: $it"
            }

            filter.complexity?.let {
                prompt += "\nComplexity: $it"
            }
            filter.otherConsiderations?.let {
                prompt += "\nOther Considerations: $it"
            }

            prompt += "\n\n--- End of User Provided Inputs ---"
        }

        if (alreadyGeneratedProblems.value.isNotEmpty()) {

            prompt += "\n\nExclude these problems: ${alreadyGeneratedProblems.value}"
        }

        Log.d(TAG, "generatedPrompt: \n$prompt")

        return prompt
    }


    fun onUiStateChanges(uiState: RandomDSAUiState) {
        savedStateHandle[PROBLEM_STATEMENT_KEY] = uiState
    }

    fun onDataStructureFilterChange(newDataStructureFilter: RandomDSAProblemFilter) {
        savedStateHandle[DATA_STRUCTURE_FILTER_KEY] = newDataStructureFilter
    }

    fun extractProblemTitle(response: String): String? {
        val regex = Regex("### Problem: (.*)")
        val matchResult = regex.find(response)
        return matchResult?.groups?.get(1)?.value?.trim()
    }

    companion object {
        private const val PROBLEM_STATEMENT_KEY = "problemStatement"
        private const val DATA_STRUCTURE_FILTER_KEY = "dataStructureFilter"
        private const val ALREADY_GENERATED_PROBLEMS_KEY = "alreadyGeneratedProblems"
        private const val TAG = "dsaProblemViewmodel"
    }

}