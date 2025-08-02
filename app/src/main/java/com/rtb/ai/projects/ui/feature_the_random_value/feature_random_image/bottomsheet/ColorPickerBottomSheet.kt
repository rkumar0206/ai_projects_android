package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_image.bottomsheet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rtb.ai.projects.databinding.BottomSheetColorPickerBinding // ViewBinding class
import com.rtb.ai.projects.util.AppUtil

class ColorPickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetColorPickerBinding? = null
    private val binding get() = _binding!!
    // Store current selected colors (as integers)
    private val selectedColors = MutableList(5) { AppUtil.getRandomColor() }
    private lateinit var colorPreviewCards: List<CardView>
    private lateinit var editColorButtons: List<View>

    companion object {
        const val TAG = "ColorPickerBottomSheet"
        const val REQUEST_KEY_COLORS = "requestKeyColors"
        const val BUNDLE_KEY_SELECTED_COLORS = "bundleKeySelectedColors"

        fun newInstance(initialColors: List<Int>? = null): ColorPickerBottomSheet {
            val fragment = ColorPickerBottomSheet()
            initialColors?.let {
                val args = Bundle()
                args.putIntegerArrayList(BUNDLE_KEY_SELECTED_COLORS, ArrayList(it))
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve initial colors if passed
        arguments?.getIntegerArrayList(BUNDLE_KEY_SELECTED_COLORS)?.let {
            if (it.size == selectedColors.size) {
                selectedColors.clear()
                selectedColors.addAll(it)
            }
        }

        initializeViews()
        setupColorPickers()
        updateColorPreviews()

        binding.buttonApplyColors.setOnClickListener {
            // Pass the list of selected color integers back to the fragment
            setFragmentResult(
                REQUEST_KEY_COLORS,
                bundleOf(BUNDLE_KEY_SELECTED_COLORS to ArrayList(selectedColors))
            )
            dismiss()
        }

        binding.buttonRefreshColors.setOnClickListener {
            selectedColors.clear()
            selectedColors.addAll(List(5) { AppUtil.getRandomColor() })
            updateColorPreviews()
            updateCombinedPreview()
        }

    }

    private fun initializeViews() {
        colorPreviewCards = listOf(
            binding.cardViewColor1,
            binding.cardViewColor2,
            binding.cardViewColor3,
            binding.cardViewColor4,
            binding.cardViewColor5
        )
        editColorButtons = listOf(
            binding.buttonEditColor1,
            binding.buttonEditColor2,
            binding.buttonEditColor3,
            binding.buttonEditColor4,
            binding.buttonEditColor5
        )
    }

    private fun setupColorPickers() {
        editColorButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                showColorPickerDialog(index, selectedColors[index])
            }
        }
    }

    private fun showColorPickerDialog(colorIndex: Int, initialColor: Int) {
        ColorPickerDialogBuilder
            .with(requireContext())
            .setTitle("Choose Color ${colorIndex + 1}")
            .initialColor(initialColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER) // Or CIRCLE
            .density(12)
            .setPositiveButton("OK") { dialog, selectedColor, allColors ->
                selectedColors[colorIndex] = selectedColor
                updateColorPreview(colorIndex)
                updateCombinedPreview()
            }
            .setNegativeButton("Cancel") { dialog, which -> }
            .build()
            .show()
    }

    private fun updateColorPreviews() {
        selectedColors.forEachIndexed { index, color ->
            colorPreviewCards[index].setCardBackgroundColor(color)
        }
         updateCombinedPreview()
    }

    private fun updateColorPreview(index: Int) {
        if (index < colorPreviewCards.size) {
            colorPreviewCards[index].setCardBackgroundColor(selectedColors[index])
        }
    }

    private fun updateCombinedPreview() {
        binding.linearLayoutCombinedColors.removeAllViews()
        if (selectedColors.isNotEmpty()) {
            binding.cardViewCombinedPreview.visibility = View.VISIBLE
            for (color in selectedColors) {
                val colorSegment = View(requireContext())
                val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                colorSegment.layoutParams = params
                colorSegment.setBackgroundColor(color)
                binding.linearLayoutCombinedColors.addView(colorSegment)
            }
        } else {
            binding.cardViewCombinedPreview.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
