package com.example.wellnessguide.ui.assessment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.ui.common.WellnessUi

class StartWellnessCheckFragment : Fragment() {

    private val urgentSelected = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val pair = WellnessUi.screen(
            context,
            "Start Wellness Check",
            "Progress: Step 1 of 5"
        ) {
            (requireActivity() as MainActivity).openDrawer()
        }

        val scroll = pair.first
        val root = pair.second

        root.addView(
            WellnessUi.paragraph(
                context,
                "Answer a few simple questions about your symptoms, lifestyle, sleep, and emotional wellness. This guide will help you understand possible wellness factors and give helpful recommendations."
            )
        )

        root.addView(
            WellnessUi.disclaimer(
                context,
                "This is not a medical diagnosis. Please consult a healthcare professional if your symptoms are severe, persistent, or getting worse."
            )
        )

        root.addView(
            WellnessUi.sectionTitle(
                context,
                "Assessment steps"
            )
        )

        root.addView(
            WellnessUi.resultCard(
                context,
                "1. Symptoms\n2. Lifestyle habits\n3. Mental wellness\n4. Sleep\n5. Wellness result"
            )
        )

        root.addView(
            WellnessUi.sectionTitle(
                context,
                "Are you experiencing any urgent symptoms right now?"
            )
        )

        val urgentContainer = WellnessUi.optionsContainer(context)
        root.addView(urgentContainer)

        val warning = TextView(context).apply {
            text = "Your symptom may need urgent medical attention. Please seek help from a healthcare professional or emergency service immediately."
            textSize = 14f
            setTextColor(Color.rgb(153, 27, 27))
            setPadding(24, 24, 24, 24)
            background = WellnessUi.roundedBg(
                Color.rgb(254, 226, 226),
                Color.rgb(248, 113, 113),
                2,
                22f
            )
            visibility = View.GONE
        }

        WellnessUi.addMultiOptions(
            urgentContainer,
            listOf(
                "Chest pain",
                "Difficulty breathing",
                "Fainting",
                "Confusion",
                "Severe headache",
                "Severe dehydration",
                "None of these"
            ),
            urgentSelected
        ) {
            val hasUrgent = urgentSelected.any { it != "None of these" }

            warning.visibility = if (hasUrgent) {
                View.VISIBLE
            } else {
                View.GONE
            }

            AssessmentSession.urgentSymptoms = if (hasUrgent) {
                urgentSelected
                    .filter { it != "None of these" }
                    .joinToString(", ")
            } else {
                "None"
            }
        }

        root.addView(warning)

        val startButton = WellnessUi.actionButton(
            context,
            "Start Assessment"
        )

        startButton.setOnClickListener {
            findNavController().navigate(R.id.assessmentFragment)
        }

        root.addView(startButton)

        val warningButton = WellnessUi.secondaryButton(
            context,
            "View Warning Signs"
        )

        warningButton.setOnClickListener {
            findNavController().navigate(
                R.id.dynamicDrawerFragment,
                bundleOf(
                    "title" to "Warning Signs",
                    "description" to "Seek urgent medical attention for chest pain, difficulty breathing, fainting, confusion, severe headache, severe dehydration, persistent vomiting, or symptoms that are getting worse."
                )
            )
        }

        root.addView(warningButton)

        val historyButton = WellnessUi.secondaryButton(
            context,
            "View Assessment History"
        )

        historyButton.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        root.addView(historyButton)

        return scroll
    }
}