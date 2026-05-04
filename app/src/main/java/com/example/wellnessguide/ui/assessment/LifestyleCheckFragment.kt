package com.example.wellnessguide.ui.assessment

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

class LifestyleCheckFragment : Fragment() {

    private val exposureFactors = mutableSetOf<String>()

    private var sleep = ""
    private var water = ""
    private var food = ""
    private var activity = ""
    private var screen = ""
    private var stress = ""
    private var travel = ""

    private lateinit var insight: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val pair = WellnessUi.screen(
            context,
            "Lifestyle Check",
            "Progress: Step 3 of 5"
        ) {
            (requireActivity() as MainActivity).openDrawer()
        }

        val scroll = pair.first
        val root = pair.second

        addSingleQuestion(
            root,
            "How many hours did you sleep last night?",
            listOf("Less than 4 hours", "4-5 hours", "6-7 hours", "8+ hours")
        ) {
            sleep = it
        }

        addSingleQuestion(
            root,
            "How much water did you drink today?",
            listOf("Low", "Enough", "More than usual")
        ) {
            water = it
        }

        addSingleQuestion(
            root,
            "Have you eaten properly today?",
            listOf("Yes", "No", "Only light meals", "Skipped meals")
        ) {
            food = it
        }

        addSingleQuestion(
            root,
            "What was your activity level today?",
            listOf("Resting", "Light Activity", "Moderate Activity", "Heavy Activity")
        ) {
            activity = it
        }

        addSingleQuestion(
            root,
            "How long was your screen time today?",
            listOf("Less than 2 hours", "2-4 hours", "5-7 hours", "8+ hours")
        ) {
            screen = it
        }

        addSingleQuestion(
            root,
            "How stressed are you today?",
            listOf("Low", "Moderate", "High")
        ) {
            stress = it
        }

        root.addView(
            WellnessUi.sectionTitle(
                context,
                "Were you exposed to any of these today?"
            )
        )

        val exposureContainer = WellnessUi.optionsContainer(context)
        root.addView(exposureContainer)

        WellnessUi.addMultiOptions(
            exposureContainer,
            listOf(
                "Dust",
                "Smoke",
                "Pollution",
                "Strong Smell",
                "Rain",
                "Heat",
                "Cold Weather",
                "Allergens",
                "Sick Person",
                "None"
            ),
            exposureFactors
        )

        addSingleQuestion(
            root,
            "Did you recently travel or have contact with someone sick?",
            listOf("Yes", "No", "Not Sure")
        ) {
            travel = it
        }

        insight = WellnessUi.resultCard(
            context,
            "Lifestyle Insight will appear here after saving."
        )
        root.addView(insight)

        val continueButton = WellnessUi.actionButton(
            context,
            "Save & Continue to Mental Wellness"
        )

        continueButton.setOnClickListener {
            if (!validateLifestyle()) return@setOnClickListener

            saveToSession()

            insight.text = """
                Lifestyle Insight

                Sleep: ${AssessmentSession.sleepHours}
                Water: ${AssessmentSession.water}
                Food: ${AssessmentSession.food}
                Activity: ${AssessmentSession.activity}
                Screen Time: ${AssessmentSession.screenTime}
                Stress: ${AssessmentSession.lifestyleStress}
                Exposure: ${AssessmentSession.exposure}
                Recent Travel or Sick Contact: ${AssessmentSession.travelContact}

                These factors may contribute to headache, fatigue, low energy, cough, or body discomfort.
            """.trimIndent()

            WellnessLogRepository.saveLifestyleLog(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Lifestyle check saved.",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.mentalWellnessFragment)
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

    private fun validateLifestyle(): Boolean {
        val missing = mutableListOf<String>()

        if (sleep.isBlank()) missing.add("Sleep hours")
        if (water.isBlank()) missing.add("Water intake")
        if (food.isBlank()) missing.add("Food intake")
        if (activity.isBlank()) missing.add("Physical activity")
        if (screen.isBlank()) missing.add("Screen time")
        if (stress.isBlank()) missing.add("Stress level")
        if (exposureFactors.isEmpty()) missing.add("Exposure factors")
        if (travel.isBlank()) missing.add("Recent travel or sick contact")

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
        AssessmentSession.sleepHours = sleep
        AssessmentSession.water = water
        AssessmentSession.food = food
        AssessmentSession.activity = activity
        AssessmentSession.screenTime = screen
        AssessmentSession.lifestyleStress = stress
        AssessmentSession.exposure = exposureFactors.joinToString(", ")
        AssessmentSession.travelContact = travel
    }
}