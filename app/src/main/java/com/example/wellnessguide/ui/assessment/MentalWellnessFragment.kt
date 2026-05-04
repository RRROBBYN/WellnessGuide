package com.example.wellnessguide.ui.assessment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.data.repository.WellnessLogRepository
import com.example.wellnessguide.ui.common.WellnessUi

class MentalWellnessFragment : Fragment() {

    private var mood = ""
    private var stress = ""
    private var energy = ""
    private var focus = ""
    private var trigger = ""

    private lateinit var suggestion: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val pair = WellnessUi.screen(
            context,
            "Mental Wellness",
            "Progress: Step 4 of 5"
        ) {
            (requireActivity() as MainActivity).openDrawer()
        }

        val scroll = pair.first
        val root = pair.second

        addSingleQuestion(
            root,
            "How are you feeling emotionally today?",
            listOf("Okay", "Tired", "Anxious", "Sad", "Overwhelmed", "Irritable")
        ) {
            mood = it
        }

        addSingleQuestion(
            root,
            "What is your stress level today?",
            listOf("Low", "Moderate", "High")
        ) {
            stress = it
        }

        addSingleQuestion(
            root,
            "How is your energy level?",
            listOf("High", "Normal", "Low", "Very Low")
        ) {
            energy = it
        }

        addSingleQuestion(
            root,
            "How is your focus today?",
            listOf("Good", "Okay", "Poor")
        ) {
            focus = it
        }

        addSingleQuestion(
            root,
            "Did anything affect your mood today?",
            listOf(
                "School or Work Pressure",
                "Lack of Sleep",
                "Personal Problem",
                "Too Much Screen Time",
                "Physical Discomfort",
                "Not Sure"
            )
        ) {
            trigger = it
        }

        root.addView(
            WellnessUi.resultCard(
                context,
                """
                Quick Breathing Exercise

                Inhale for 4 seconds.
                Hold for 4 seconds.
                Exhale for 4 seconds.
                Repeat 3 times.
                """.trimIndent()
            )
        )

        suggestion = WellnessUi.resultCard(
            context,
            "Mental wellness recommendation will appear here after saving."
        )
        root.addView(suggestion)

        val breathingButton = WellnessUi.secondaryButton(
            context,
            "Start Breathing Exercise"
        )

        breathingButton.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Quick Breathing Exercise")
                .setMessage(
                    """
                    Inhale for 4 seconds.
                    Hold for 4 seconds.
                    Exhale for 4 seconds.
                    Repeat 3 times.
                    """.trimIndent()
                )
                .setPositiveButton("Done", null)
                .show()
        }

        root.addView(breathingButton)

        val continueButton = WellnessUi.actionButton(
            context,
            "Save & Continue to Sleep Tracker"
        )

        continueButton.setOnClickListener {
            if (!validateMentalWellness()) return@setOnClickListener

            saveToSession()

            suggestion.text = """
                Mental Wellness Summary

                Mood: ${AssessmentSession.mood}
                Stress: ${AssessmentSession.mentalStress}
                Energy: ${AssessmentSession.energy}
                Focus: ${AssessmentSession.focus}
                Trigger: ${AssessmentSession.emotionalTrigger}

                You reported ${AssessmentSession.mentalStress} stress and ${AssessmentSession.energy} energy. Try resting for a few minutes, drinking water, doing slow breathing, and reducing screen time.
            """.trimIndent()

            WellnessLogRepository.saveMentalLog(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Mental wellness check saved.",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.sleepTrackerFragment)
                },
                onFailure = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        root.addView(continueButton)

        return scroll
    }

    private fun addSingleQuestion(
        root: LinearLayout,
        title: String,
        options: List<String>,
        onPick: (String) -> Unit
    ) {
        val context = requireContext()

        root.addView(
            WellnessUi.sectionTitle(context, title)
        )

        val optionContainer = WellnessUi.optionsContainer(context)
        root.addView(optionContainer)

        WellnessUi.addSingleOptions(
            optionContainer,
            options,
            onPick
        )
    }

    private fun validateMentalWellness(): Boolean {
        val missing = mutableListOf<String>()

        if (mood.isBlank()) missing.add("Mood")
        if (stress.isBlank()) missing.add("Stress level")
        if (energy.isBlank()) missing.add("Energy level")
        if (focus.isBlank()) missing.add("Focus level")
        if (trigger.isBlank()) missing.add("Emotional trigger")

        return if (missing.isEmpty()) {
            true
        } else {
            Toast.makeText(
                requireContext(),
                "Please answer: ${missing.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun saveToSession() {
        AssessmentSession.mood = mood
        AssessmentSession.mentalStress = stress
        AssessmentSession.energy = energy
        AssessmentSession.focus = focus
        AssessmentSession.emotionalTrigger = trigger
    }
}