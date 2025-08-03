package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.data.model.KeyElements
import com.rtb.ai.projects.data.model.StoryInput
import com.rtb.ai.projects.databinding.BottomSheetStoryInputBinding

data class LengthOption(val displayName: String, val definiteValue: String) {
    override fun toString(): String = displayName
}

class StoryInputDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStoryInputBinding? = null
    private val binding get() = _binding!!

    private var onSubmitListener: ((StoryInput) -> Unit)? = null
    private var selectedLengthDefiniteValue: String? = null

    fun setOnSubmitListener(listener: (StoryInput) -> Unit) {
        onSubmitListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStoryInputBinding.inflate(inflater, container, false)

        setGenreListAndSetupDropdown()
        setTargetAudienceAndSetupDropdown()
        setLengthAndSetupDropdown()

        return binding.root
    }

    private fun setLengthAndSetupDropdown() {

        // --- Setup Length Dropdown ---
        val lengthOptions = listOf(
            LengthOption(SELECT, ""),      // e.g., Short = 500 words
            LengthOption("Short (500)", "500"),      // e.g., Short = 500 words
            LengthOption("Medium (1500)", "1500"),    // e.g., Medium = 1500 words
            LengthOption("Long (3000)", "3000")       // e.g., Long = 3000 words
            // Add more options or adjust values as needed
        )
        val lengthArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            lengthOptions
        )
        binding.actLength.setAdapter(lengthArrayAdapter)
        binding.actLength.setText(lengthOptions[0].displayName, false)
    }

    private fun setTargetAudienceAndSetupDropdown() {


        // --- Setup Target Audience Dropdown ---
        val targetAudiences = listOf(
            SELECT,
            // Age Groups
            "Children (Ages 0-5, Picture Books)",
            "Children (Ages 5-7, Early Readers)",
            "Children (Ages 7-10, Chapter Books)",
            "Middle Grade (MG, Ages 8-12)",
            "Young Adult (YA, Ages 12-18)",
            "New Adult (NA, Ages 18-25)",
            "Adults (General)",
            "Mature Adults",
            "Seniors",
            // Interests/Preferences (Examples)
            "Fans of Epic Fantasy",
            "Readers Seeking Fast-Paced Thrillers",
            "Character-Driven Story Lovers",
            "Gamers",
            "History Buffs",
            "Science Enthusiasts",
            // You can add more specific or combined audiences
            "Teens interested in Sci-Fi with social commentary",
            "Adults looking for light-hearted Romance"
            // Add any other target audiences you want
        )

        val targetAudienceArrayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            targetAudiences
        )
        binding.actTargetAudience.setAdapter(targetAudienceArrayAdapter)
        binding.actTargetAudience.setText(targetAudiences[0], false)
    }

    private fun setGenreListAndSetupDropdown() {

        // --- Setup Genre Dropdown ---
        val genres = listOf(
            SELECT,
            "Fantasy",
            "High Fantasy",
            "Urban Fantasy",
            "Dark Fantasy",
            "Sword and Sorcery",
            "Science Fiction (Sci-Fi)",
            "Hard Sci-Fi",
            "Space Opera",
            "Cyberpunk",
            "Steampunk",
            "Dystopian",
            "Utopian",
            "Mystery",
            "Detective Fiction",
            "Cozy Mystery",
            "Noir",
            "Thriller",
            "Psychological Thriller",
            "Crime Thriller",
            "Spy Thriller",
            "Horror",
            "Supernatural Horror",
            "Psychological Horror",
            "Slasher",
            "Body Horror",
            "Cosmic Horror",
            "Romance",
            "Contemporary Romance",
            "Historical Romance",
            "Paranormal Romance",
            "Romantic Comedy",
            "Historical Fiction",
            "Contemporary/Realistic Fiction",
            "Adventure",
            "Comedy",
            "Satire",
            "Parody",
            "Slapstick",
            "Dark Comedy",
            "Drama",
            "Western",
            "Post-Apocalyptic",
            "Literary Fiction",
            "Young Adult (YA)",
            "Children's Fiction",
            "Fairy Tale/Fable",
            "Mythology/Folklore",
            "Alternative History"
            // Add any other genres you want
        )

        val genreArrayAdapter =
            ArrayAdapter(
                requireContext(),
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
                genres
            )
        binding.actGenre.setAdapter(genreArrayAdapter)

        binding.actGenre.setText(genres[0], false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpClickListeners()
    }

    private fun setUpClickListeners() {


        binding.btnCorePremiseInfo.setOnClickListener {
            showCorePremiseInfoDialog()
        }

        binding.btnProtagonistInfo.setOnClickListener {
            showInfoDialog(
                "Who is the Protagonist?",
                "The main character of your story. The story primarily revolves around their experiences, challenges, and goals. " +
                        "They are usually the character the audience roots for and follows most closely." +
                        "\nEXAMPLE:\nLyra, a 17-year-old scavenger with a knack for hacking"
            )
        }

        binding.btnAntagonistInfo.setOnClickListener {
            showInfoDialog(
                "Who or What is the Antagonist?",
                "The character, force, or internal struggle that opposes the protagonist and creates conflict. " +
                        "This doesn't always have to be a person; it could be society, nature, or the protagonist's own flaws." +
                        "\nEXAMPLE:\nThe Authority, a totalitarian regime that controls all AI knowledge"
            )
        }

        binding.btnSettingInfo.setOnClickListener {
            showInfoDialog(
                "What is the Setting?",
                "The time and place (or environment) where your story occurs. " +
                        "This includes the geographical location, historical period, social conditions, and overall atmosphere. " +
                        "A strong setting can feel like a character itself." +
                        "\nEXAMPLE:\nA crumbling mega-city on Earth in the year 2194, built atop forgotten ruins of older civilizations"
            )
        }

        binding.btnConflictInfo.setOnClickListener {
            showInfoDialog(
                "What is the Conflict?",
                "The central problem or struggle that the protagonist faces. Conflict drives the plot forward and creates tension. " +
                        "It can be internal (character vs. self) or external (character vs. character, character vs. nature, character vs. society, etc.)." +
                        "\nEXAMPLE:\nLyra must choose between exposing the truth about the AI and putting her life—and her community—at risk"
            )
        }

        binding.btnThemesInfo.setOnClickListener {
            showInfoDialog(
                "What are Themes?",
                "The underlying ideas, messages, or insights about life, society, or human nature that your story explores. " +
                        "Themes are often implicit and emerge through the plot, characters, and conflicts. Examples: love, loss, justice, prejudice." +
                        "\nEXAMPLE:\nRebellion, Technology vs Humanity, Truth and Power, Hope in Dystopia"
            )
        }

        binding.btnMoodToneInfo.setOnClickListener {
            showInfoDialog(
                "What is Mood/Tone?",
                "Mood: The atmosphere or feeling that the story evokes in the reader (e.g., suspenseful, melancholic, joyful).\n\n" +
                        "Tone: The author's attitude or approach towards the subject matter or audience (e.g., sarcastic, serious, humorous, nostalgic)." +
                        "\nEXAMPLE:\nGritty, suspenseful, with moments of wonder"
            )
        }

        binding.actLength.setOnItemClickListener { parent, _, position, _ ->
            val selectedOption = parent.adapter.getItem(position) as LengthOption
            selectedLengthDefiniteValue = selectedOption.definiteValue
        }

        binding.btnApply.setOnClickListener {
            val storyInput = StoryInput(
                genre = binding.actGenre.text.toString().takeIf { it.isNotBlank() && it != SELECT },
                targetAudience = binding.actTargetAudience.text.toString()
                    .takeIf { it.isNotBlank() && it != SELECT },
                corePremise = binding.etCorePremise.text.toString().takeIf { it.isNotBlank() },
                keyElements = KeyElements(
                    protagonist = binding.etProtagonist.text.toString().takeIf { it.isNotBlank() },
                    antagonist = binding.etAntagonist.text.toString().takeIf { it.isNotBlank() },
                    setting = binding.etSetting.text.toString().takeIf { it.isNotBlank() },
                    conflict = binding.etConflict.text.toString().takeIf { it.isNotBlank() },
                    themes = binding.etThemes.text.toString().split(",").map { it.trim() }
                        .filter { it.isNotBlank() },
                    moodTone = binding.etMoodTone.text.toString().takeIf { it.isNotBlank() }
                ),
                length = selectedLengthDefiniteValue.toString().takeIf { it.isNotBlank() },
                outputFormat = binding.etOutputFormat.text.toString().takeIf { it.isNotBlank() },
                language = binding.etLanguage.text.toString().takeIf { it.isNotBlank() }
            )
            onSubmitListener?.invoke(storyInput)
            dismiss()
        }
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Got it!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showCorePremiseInfoDialog() {

        showInfoDialog(
            "What is a Core Premise?",
            "The core premise is the fundamental concept or the 'what if' question that drives your story. " +
                    "It's the most basic building block of your narrative, usually summed up in one or two compelling sentences.\n\n" +
                    "It should generally include:\n" +
                    "- The Protagonist(s) (who the story is about)\n" +
                    "- The Situation/Inciting Incident (what kicks off the story)\n" +
                    "- The Core Conflict/Goal (what the protagonist must overcome or achieve)" +
                    "\nEXAMPLE:\n" +
                    "A rebellious teenager discovers an ancient AI hidden beneath her city, which may hold the key to saving a dying Earth."
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StoryInputDialogFragment"
        const val SELECT = "Select"
        fun newInstance(): StoryInputDialogFragment {
            return StoryInputDialogFragment()
        }
    }
}
