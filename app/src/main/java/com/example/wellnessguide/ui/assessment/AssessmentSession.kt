package com.example.wellnessguide.ui.assessment

object AssessmentSession {
    var urgentSymptoms: String = "None"

    var symptoms: String = "Not selected"
    var severity: String = "Not selected"
    var duration: String = "Not selected"
    var frequency: String = "Not selected"
    var location: String = "Not selected"
    var relatedSymptoms: String = "None selected"
    var trigger: String = "Not selected"
    var symptomNotes: String = ""

    var sleepHours: String = "Not selected"
    var water: String = "Not selected"
    var food: String = "Not selected"
    var activity: String = "Not selected"
    var screenTime: String = "Not selected"
    var lifestyleStress: String = "Not selected"
    var exposure: String = "None selected"
    var travelContact: String = "Not selected"

    var mood: String = "Not selected"
    var mentalStress: String = "Not selected"
    var energy: String = "Not selected"
    var focus: String = "Not selected"
    var emotionalTrigger: String = "Not selected"

    var bedtime: String = "Not selected"
    var wakeTime: String = "Not selected"
    var sleepDuration: String = "Not calculated"
    var sleepQuality: String = "Not selected"
    var screenBeforeSleep: String = "Not selected"
    var caffeineBeforeSleep: String = "Not selected"
    var wokeUpDuringSleep: String = "Not selected"
    var sleepConsistency: String = "Not selected"
    var sleepNotes: String = ""

    fun wellnessStatus(): String {
        return when {
            urgentSymptoms != "None" -> "Red - Seek medical advice"
            severity.equals("Severe", true) -> "Red - Seek medical advice"
            duration.equals("More than 1 week", true) -> "Yellow - Needs monitoring"
            mentalStress.equals("High", true) -> "Yellow - Needs monitoring"
            sleepHours.equals("Less than 4 hours", true) -> "Yellow - Needs monitoring"
            else -> "Green - Low concern"
        }
    }

    fun finalSummary(): String {
        return """
            Today's Wellness Summary

            Wellness Status:
            ${wellnessStatus()}

            Symptoms:
            Main symptoms: $symptoms
            Severity: $severity
            Duration: $duration
            Frequency: $frequency
            Pain location: $location
            Related symptoms: $relatedSymptoms
            Possible trigger: $trigger
            Notes: $symptomNotes

            Lifestyle Factors:
            Sleep hours last night: $sleepHours
            Water intake: $water
            Food intake: $food
            Physical activity: $activity
            Screen time: $screenTime
            Stress level: $lifestyleStress
            Exposure factors: $exposure
            Recent travel or sick contact: $travelContact

            Mental Wellness:
            Mood: $mood
            Stress: $mentalStress
            Energy: $energy
            Focus: $focus
            Emotional trigger: $emotionalTrigger

            Sleep Tracker:
            Bedtime: $bedtime
            Wake time: $wakeTime
            Sleep duration: $sleepDuration
            Sleep quality: $sleepQuality
            Screen before sleep: $screenBeforeSleep
            Caffeine before sleep: $caffeineBeforeSleep
            Woke up during sleep: $wokeUpDuringSleep
            Sleep consistency: $sleepConsistency
            Sleep notes: $sleepNotes

            Possible Wellness Factors:
            Your symptoms may be affected by sleep, stress, hydration, screen time, exposure to irritants, recent contact with sick people, or daily activity level.

            Recommendations:
            Rest, drink enough water, reduce screen time, monitor your symptoms, avoid smoke or dust, and try slow breathing if stressed.

            Recovery Tracker:
            Day 1 of monitoring. Check your symptoms again after 6 to 12 hours. Mark if symptoms are improving, the same, or worsening.

            Warning Signs:
            Seek medical help if you have chest pain, difficulty breathing, fainting, confusion, severe headache, severe dehydration, persistent vomiting, or symptoms that are getting worse.

            This is not a medical diagnosis. Please consult a healthcare professional for severe, persistent, or worsening symptoms.
        """.trimIndent()
    }
}