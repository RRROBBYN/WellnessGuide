package com.example.wellnessguide.ui.assessment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.ui.common.WellnessUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.wellnessguide.data.repository.WellnessLogRepository

class AssessmentFragment : Fragment() {

    private val selectedSymptoms = mutableSetOf<String>()
    private val selectedRelatedSymptoms = mutableSetOf<String>()

    private var severity = ""
    private var duration = ""
    private var frequency = ""
    private var painLocation = ""
    private var trigger = ""

    private lateinit var summaryText: TextView
    private lateinit var notesInput: EditText

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()

        val pair = WellnessUi.screen(
            context,
            "Symptom Assessment",
            "Progress: Step 2 of 5"
        ) {
            (requireActivity() as MainActivity).openDrawer()
        }

        val scroll = pair.first
        val root = pair.second

        root.addView(WellnessUi.sectionTitle(context, "What symptoms are you feeling today?"))
        val symptomsContainer = WellnessUi.optionsContainer(context)
        root.addView(symptomsContainer)

        WellnessUi.addMultiOptions(
            symptomsContainer,
            listOf(
                "Headache",
                "Fever",
                "Cough",
                "Sore Throat",
                "Fatigue",
                "Body Pain",
                "Dizziness",
                "Nausea",
                "Stomach Pain",
                "Runny Nose",
                "Chest Discomfort",
                "Difficulty Breathing",
                "Other"
            ),
            selectedSymptoms
        ) {
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "How severe is it?"))
        val severityContainer = WellnessUi.optionsContainer(context)
        root.addView(severityContainer)

        WellnessUi.addSingleOptions(severityContainer, listOf("Mild", "Moderate", "Severe")) {
            severity = it
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "How long have you been feeling this?"))
        val durationContainer = WellnessUi.optionsContainer(context)
        root.addView(durationContainer)

        WellnessUi.addSingleOptions(
            durationContainer,
            listOf("A few hours", "1 day", "2-3 days", "1 week", "More than 1 week")
        ) {
            duration = it
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "How often does it happen?"))
        val frequencyContainer = WellnessUi.optionsContainer(context)
        root.addView(frequencyContainer)

        WellnessUi.addSingleOptions(frequencyContainer, listOf("Once", "Sometimes", "Often", "Constant")) {
            frequency = it
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "Where do you feel the pain?"))
        val locationContainer = WellnessUi.optionsContainer(context)
        root.addView(locationContainer)

        WellnessUi.addSingleOptions(
            locationContainer,
            listOf("Forehead", "Back of Head", "Chest", "Stomach", "Lower Back", "Arms", "Legs", "Whole Body")
        ) {
            painLocation = it
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "Do you also feel any of these?"))
        val relatedContainer = WellnessUi.optionsContainer(context)
        root.addView(relatedContainer)

        WellnessUi.addMultiOptions(
            relatedContainer,
            listOf(
                "Fever",
                "Cough",
                "Body Pain",
                "Dizziness",
                "Nausea",
                "Vomiting",
                "Fatigue",
                "Sore Throat",
                "Shortness of Breath"
            ),
            selectedRelatedSymptoms
        ) {
            updateSummary()
        }

        root.addView(WellnessUi.sectionTitle(context, "What do you think triggered it?"))
        val triggerContainer = WellnessUi.optionsContainer(context)
        root.addView(triggerContainer)

        WellnessUi.addSingleOptions(
            triggerContainer,
            listOf(
                "Lack of Sleep",
                "Stress",
                "Long Screen Time",
                "Dust or Smoke",
                "Weather Change",
                "Food",
                "Exercise",
                "Travel",
                "Contact with Sick Person",
                "Not Sure"
            )
        ) {
            trigger = it
            updateSummary()
        }

        notesInput = WellnessUi.input(
            context,
            "Add notes: Example: My headache started after using my phone for many hours.",
            120
        )
        notesInput.gravity = Gravity.TOP
        root.addView(notesInput)

        summaryText = WellnessUi.resultCard(context, "Symptom summary will appear here.")
        root.addView(summaryText)

        val continueButton = WellnessUi.actionButton(
            context,
            "Save & Continue to Lifestyle Check"
        )

        continueButton.setOnClickListener {
            if (!validateSymptomAssessment()) return@setOnClickListener

            saveToSession()

            WellnessLogRepository.saveSymptomLog(
                onSuccess = {
                    Toast.makeText(
                        context,
                        "Symptom assessment saved.",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigate(R.id.lifestyleCheckFragment)
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

    private fun updateSummary() {
        summaryText.text = """
            Symptom: ${selectedSymptoms.ifEmpty { listOf("Not selected") }.joinToString(", ")}
            Severity: ${severity.ifBlank { "Not selected" }}
            Duration: ${duration.ifBlank { "Not selected" }}
            Frequency: ${frequency.ifBlank { "Not selected" }}
            Location: ${painLocation.ifBlank { "Not selected" }}
            Related Symptoms: ${selectedRelatedSymptoms.ifEmpty { listOf("None selected") }.joinToString(", ")}
            Trigger: ${trigger.ifBlank { "Not selected" }}
        """.trimIndent()
    }
    private fun validateSymptomAssessment(): Boolean {
        val missing = mutableListOf<String>()

        if (selectedSymptoms.isEmpty()) missing.add("Symptoms")
        if (severity.isBlank()) missing.add("Severity")
        if (duration.isBlank()) missing.add("Duration")
        if (frequency.isBlank()) missing.add("Frequency")
        if (painLocation.isBlank()) missing.add("Pain location")
        if (selectedRelatedSymptoms.isEmpty()) missing.add("Related symptoms")
        if (trigger.isBlank()) missing.add("Possible trigger")

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
        AssessmentSession.symptoms = selectedSymptoms.ifEmpty { listOf("Not selected") }.joinToString(", ")
        AssessmentSession.severity = severity.ifBlank { "Not selected" }
        AssessmentSession.duration = duration.ifBlank { "Not selected" }
        AssessmentSession.frequency = frequency.ifBlank { "Not selected" }
        AssessmentSession.location = painLocation.ifBlank { "Not selected" }
        AssessmentSession.relatedSymptoms = selectedRelatedSymptoms.ifEmpty { listOf("None selected") }.joinToString(", ")
        AssessmentSession.trigger = trigger.ifBlank { "Not selected" }
        AssessmentSession.symptomNotes = notesInput.text.toString()
    }


}