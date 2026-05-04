package com.example.wellnessguide.data.repository

import com.example.wellnessguide.ui.assessment.AssessmentSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object WellnessLogRepository {

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private const val COLLECTION_NAME = "wellness_logs"

    fun saveSymptomLog(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val status = when {
            AssessmentSession.severity.equals("Severe", true) -> "Red - Seek medical advice"
            AssessmentSession.duration.equals("More than 1 week", true) -> "Yellow - Needs monitoring"
            AssessmentSession.relatedSymptoms.contains("Shortness of Breath", true) -> "Red - Seek medical advice"
            AssessmentSession.symptoms.contains("Chest Discomfort", true) -> "Red - Seek medical advice"
            AssessmentSession.severity.equals("Moderate", true) -> "Yellow - Needs monitoring"
            else -> "Green - Low concern"
        }

        val recommendations = buildString {
            append("Rest, drink enough water, and monitor your symptoms. ")

            if (AssessmentSession.trigger.contains("Long Screen Time", true)) {
                append("Take screen breaks and rest your eyes. ")
            }

            if (AssessmentSession.trigger.contains("Stress", true)) {
                append("Try slow breathing and reduce stressors when possible. ")
            }

            if (AssessmentSession.trigger.contains("Dust", true) || AssessmentSession.trigger.contains("Smoke", true)) {
                append("Avoid dust, smoke, strong smells, and other irritants. ")
            }

            if (status.startsWith("Red")) {
                append("Because this may be urgent, please consult a healthcare professional as soon as possible. ")
            }

            append("This is not a medical diagnosis.")
        }

        saveLog(
            logType = "symptom",
            title = "Symptom Assessment",
            status = status,
            summary = """
                Symptoms: ${AssessmentSession.symptoms}
                Severity: ${AssessmentSession.severity}
                Duration: ${AssessmentSession.duration}
                Frequency: ${AssessmentSession.frequency}
                Location: ${AssessmentSession.location}
                Related symptoms: ${AssessmentSession.relatedSymptoms}
                Trigger: ${AssessmentSession.trigger}
                Notes: ${AssessmentSession.symptomNotes}
            """.trimIndent(),
            recommendations = recommendations,
            details = mapOf(
                "symptoms" to AssessmentSession.symptoms,
                "severity" to AssessmentSession.severity,
                "duration" to AssessmentSession.duration,
                "frequency" to AssessmentSession.frequency,
                "location" to AssessmentSession.location,
                "relatedSymptoms" to AssessmentSession.relatedSymptoms,
                "trigger" to AssessmentSession.trigger,
                "notes" to AssessmentSession.symptomNotes
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun saveLifestyleLog(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val status = when {
            AssessmentSession.sleepHours.equals("Less than 4 hours", true) -> "Yellow - Needs monitoring"
            AssessmentSession.screenTime.equals("8+ hours", true) -> "Yellow - Needs monitoring"
            AssessmentSession.lifestyleStress.equals("High", true) -> "Yellow - Needs monitoring"
            AssessmentSession.water.equals("Low", true) -> "Yellow - Needs monitoring"
            else -> "Green - Low concern"
        }

        val recommendations = buildString {
            if (AssessmentSession.sleepHours == "Less than 4 hours" || AssessmentSession.sleepHours == "4-5 hours") {
                append("Try to get more rest and keep a consistent sleep schedule. ")
            }

            if (AssessmentSession.water == "Low") {
                append("Drink more water throughout the day. ")
            }

            if (AssessmentSession.screenTime == "8+ hours" || AssessmentSession.screenTime == "5-7 hours") {
                append("Take regular screen breaks and rest your eyes. ")
            }

            if (AssessmentSession.lifestyleStress == "High") {
                append("Try slow breathing, short breaks, and light movement to reduce stress. ")
            }

            if (AssessmentSession.exposure.contains("Dust", true) || AssessmentSession.exposure.contains("Smoke", true)) {
                append("Avoid dust, smoke, pollution, and strong smells when possible. ")
            }

            append("Monitor how these habits affect your symptoms.")
        }

        saveLog(
            logType = "lifestyle",
            title = "Lifestyle Check",
            status = status,
            summary = """
                Sleep: ${AssessmentSession.sleepHours}
                Water: ${AssessmentSession.water}
                Food: ${AssessmentSession.food}
                Activity: ${AssessmentSession.activity}
                Screen time: ${AssessmentSession.screenTime}
                Stress: ${AssessmentSession.lifestyleStress}
                Exposure: ${AssessmentSession.exposure}
                Travel or sick contact: ${AssessmentSession.travelContact}
            """.trimIndent(),
            recommendations = recommendations,
            details = mapOf(
                "sleepHours" to AssessmentSession.sleepHours,
                "water" to AssessmentSession.water,
                "food" to AssessmentSession.food,
                "activity" to AssessmentSession.activity,
                "screenTime" to AssessmentSession.screenTime,
                "stress" to AssessmentSession.lifestyleStress,
                "exposure" to AssessmentSession.exposure,
                "travelContact" to AssessmentSession.travelContact
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun saveMentalLog(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val status = when {
            AssessmentSession.mentalStress.equals("High", true) -> "Yellow - Needs monitoring"
            AssessmentSession.energy.equals("Very Low", true) -> "Yellow - Needs monitoring"
            AssessmentSession.focus.equals("Poor", true) -> "Yellow - Needs monitoring"
            AssessmentSession.mood.equals("Overwhelmed", true) -> "Yellow - Needs monitoring"
            else -> "Green - Low concern"
        }

        val recommendations = buildString {
            if (AssessmentSession.mentalStress == "High") {
                append("Try a short breathing exercise and take a quiet break. ")
            }

            if (AssessmentSession.energy == "Low" || AssessmentSession.energy == "Very Low") {
                append("Rest, hydrate, and avoid overexertion today. ")
            }

            if (AssessmentSession.focus == "Poor") {
                append("Reduce distractions and take short focus breaks. ")
            }

            if (AssessmentSession.emotionalTrigger.contains("Too Much Screen Time", true)) {
                append("Reduce screen time and rest your eyes. ")
            }

            append("If emotional distress feels severe, persistent, or overwhelming, consider talking to a trusted person or healthcare professional.")
        }

        saveLog(
            logType = "mental",
            title = "Mental Wellness",
            status = status,
            summary = """
                Mood: ${AssessmentSession.mood}
                Stress: ${AssessmentSession.mentalStress}
                Energy: ${AssessmentSession.energy}
                Focus: ${AssessmentSession.focus}
                Emotional trigger: ${AssessmentSession.emotionalTrigger}
            """.trimIndent(),
            recommendations = recommendations,
            details = mapOf(
                "mood" to AssessmentSession.mood,
                "stress" to AssessmentSession.mentalStress,
                "energy" to AssessmentSession.energy,
                "focus" to AssessmentSession.focus,
                "emotionalTrigger" to AssessmentSession.emotionalTrigger
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun saveSleepLog(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val status = when {
            AssessmentSession.sleepDuration.contains("Enter time", true) -> "Yellow - Needs complete sleep time"
            AssessmentSession.sleepQuality.equals("Poor", true) -> "Yellow - Needs monitoring"
            AssessmentSession.screenBeforeSleep.equals("Yes", true) -> "Yellow - Needs monitoring"
            AssessmentSession.caffeineBeforeSleep.equals("Yes", true) -> "Yellow - Needs monitoring"
            AssessmentSession.wokeUpDuringSleep.equals("Yes, many times", true) -> "Yellow - Needs monitoring"
            else -> "Green - Low concern"
        }

        val recommendations = buildString {
            append("Keep your bedroom cool and dark. ")

            if (AssessmentSession.screenBeforeSleep == "Yes") {
                append("Avoid screens at least 30 minutes before sleeping. ")
            }

            if (AssessmentSession.caffeineBeforeSleep == "Yes") {
                append("Avoid caffeine late in the day. ")
            }

            if (AssessmentSession.sleepQuality == "Poor" || AssessmentSession.wokeUpDuringSleep == "Yes, many times") {
                append("Try a consistent bedtime routine and monitor your sleep quality. ")
            }

            append("Poor sleep may contribute to headache, fatigue, low energy, stress, or poor focus.")
        }

        saveLog(
            logType = "sleep",
            title = "Sleep Tracker",
            status = status,
            summary = """
                Bedtime: ${AssessmentSession.bedtime}
                Wake time: ${AssessmentSession.wakeTime}
                Sleep duration: ${AssessmentSession.sleepDuration}
                Sleep quality: ${AssessmentSession.sleepQuality}
                Screen before sleep: ${AssessmentSession.screenBeforeSleep}
                Caffeine before sleep: ${AssessmentSession.caffeineBeforeSleep}
                Woke up during sleep: ${AssessmentSession.wokeUpDuringSleep}
                Sleep consistency: ${AssessmentSession.sleepConsistency}
                Notes: ${AssessmentSession.sleepNotes}
            """.trimIndent(),
            recommendations = recommendations,
            details = mapOf(
                "bedtime" to AssessmentSession.bedtime,
                "wakeTime" to AssessmentSession.wakeTime,
                "sleepDuration" to AssessmentSession.sleepDuration,
                "sleepQuality" to AssessmentSession.sleepQuality,
                "screenBeforeSleep" to AssessmentSession.screenBeforeSleep,
                "caffeineBeforeSleep" to AssessmentSession.caffeineBeforeSleep,
                "wokeUpDuringSleep" to AssessmentSession.wokeUpDuringSleep,
                "sleepConsistency" to AssessmentSession.sleepConsistency,
                "notes" to AssessmentSession.sleepNotes
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun saveFullAssessmentLog(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        saveLog(
            logType = "full_assessment",
            title = "Full Wellness Assessment",
            status = AssessmentSession.wellnessStatus(),
            summary = AssessmentSession.finalSummary(),
            recommendations = """
                Rest, hydrate, monitor your symptoms, reduce screen time, keep a consistent sleep schedule, and check again after 6 to 12 hours.
                
                Seek professional help if symptoms are severe, persistent, or worsening.
            """.trimIndent(),
            details = mapOf(
                "urgentSymptoms" to AssessmentSession.urgentSymptoms,
                "symptoms" to AssessmentSession.symptoms,
                "severity" to AssessmentSession.severity,
                "duration" to AssessmentSession.duration,
                "lifestyleStress" to AssessmentSession.lifestyleStress,
                "mood" to AssessmentSession.mood,
                "sleepDuration" to AssessmentSession.sleepDuration,
                "sleepQuality" to AssessmentSession.sleepQuality
            ),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    private fun saveLog(
        logType: String,
        title: String,
        status: String,
        summary: String,
        recommendations: String,
        details: Map<String, Any?>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onFailure("Please login to save your wellness log.")
            return
        }

        val cleanDetails = details.mapValues { entry ->
            entry.value ?: ""
        }

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to logType,
            "title" to title,
            "status" to status,
            "summary" to summary,
            "recommendations" to recommendations,
            "createdAt" to System.currentTimeMillis(),
            "details" to cleanDetails
        )

        db.collection(COLLECTION_NAME)
            .add(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onFailure(error.message ?: "Failed to save wellness log.")
            }
    }
}