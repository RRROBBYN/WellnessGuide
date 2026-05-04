package com.example.wellnessguide.ui.activity

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityFeatureFragment : Fragment() {

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pageType = arguments?.getString("pageType") ?: "daily_checkin"

        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(42))
        }

        addTopBar(root, pageTitle(pageType))
        root.addView(bigTitle(pageTitle(pageType)))

        when (pageType) {
            "daily_checkin" -> buildDailyCheckIn(root)
            "symptom_log" -> buildFilteredLogPage(
                root = root,
                description = "View your saved symptom assessments only.",
                logTypes = listOf("symptom"),
                emptyMessage = "No symptom logs yet.",
                addButtonText = "Add Symptom",
                destinationId = R.id.assessmentFragment
            )
            "sleep_log" -> buildFilteredLogPage(
                root = root,
                description = "View your saved sleep records and sleep recommendations.",
                logTypes = listOf("sleep"),
                emptyMessage = "No sleep logs yet.",
                addButtonText = "Add Sleep Log",
                destinationId = R.id.sleepTrackerFragment
            )
            "mood_stress" -> buildFilteredLogPage(
                root = root,
                description = "View your saved mood, stress, energy, and focus logs.",
                logTypes = listOf("mental"),
                emptyMessage = "No mood and stress logs yet.",
                addButtonText = "Add Mood Check",
                destinationId = R.id.mentalWellnessFragment
            )
            "physical_activity" -> buildPhysicalActivity(root)
            else -> buildDailyCheckIn(root)
        }

        scroll.addView(root)
        return scroll
    }

    private fun buildDailyCheckIn(root: LinearLayout) {
        root.addView(
            paragraph(
                "Use this quick check-in to save your daily wellness condition even if you do not want to complete the full assessment."
            )
        )

        var mood = ""
        var energy = ""
        var sleep = ""
        var water = ""
        var stress = ""
        var symptomsToday = ""

        addSingleQuestion(
            root,
            "How are you feeling today?",
            listOf("Good", "Okay", "Tired", "Stressed", "Sick")
        ) {
            mood = it
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
            "How was your sleep?",
            listOf("Good", "Fair", "Poor")
        ) {
            sleep = it
        }

        addSingleQuestion(
            root,
            "How is your water intake today?",
            listOf("Low", "Enough", "More than usual")
        ) {
            water = it
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
            "Do you have symptoms today?",
            listOf("No", "Yes", "Not sure")
        ) {
            symptomsToday = it
        }

        val notesInput = input("Daily notes. Example: I feel tired because I slept late.", 120)
        notesInput.gravity = Gravity.TOP
        root.addView(notesInput)

        val recentContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val saveButton = actionButton("Save Daily Check-In")
        saveButton.setOnClickListener {
            val missing = mutableListOf<String>()

            if (mood.isBlank()) missing.add("Mood")
            if (energy.isBlank()) missing.add("Energy")
            if (sleep.isBlank()) missing.add("Sleep")
            if (water.isBlank()) missing.add("Water intake")
            if (stress.isBlank()) missing.add("Stress level")
            if (symptomsToday.isBlank()) missing.add("Symptoms today")
            if (notesInput.text.toString().isBlank()) missing.add("Daily notes")

            if (missing.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please answer: ${missing.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val status = when {
                symptomsToday == "Yes" -> "Yellow - Needs monitoring"
                stress == "High" -> "Yellow - Needs monitoring"
                energy == "Very Low" -> "Yellow - Needs monitoring"
                sleep == "Poor" -> "Yellow - Needs monitoring"
                water == "Low" -> "Yellow - Needs monitoring"
                else -> "Green - Low concern"
            }

            val summary = """
                Mood: $mood
                Energy: $energy
                Sleep: $sleep
                Water intake: $water
                Stress: $stress
                Symptoms today: $symptomsToday
                Notes: ${notesInput.text}
            """.trimIndent()

            val recommendations = buildString {
                if (sleep == "Poor") append("Try to rest earlier tonight and keep a consistent bedtime. ")
                if (water == "Low") append("Drink enough water throughout the day. ")
                if (stress == "High") append("Try slow breathing and take short breaks. ")
                if (energy == "Low" || energy == "Very Low") append("Rest, hydrate, and avoid overexertion. ")
                if (symptomsToday == "Yes") append("Monitor your symptoms and consider completing a symptom assessment. ")
                append("Continue tracking your wellness daily.")
            }

            saveLog(
                logType = "daily_checkin",
                title = "Daily Check-In",
                status = status,
                summary = summary,
                recommendations = recommendations,
                details = mapOf(
                    "mood" to mood,
                    "energy" to energy,
                    "sleep" to sleep,
                    "water" to water,
                    "stress" to stress,
                    "symptomsToday" to symptomsToday,
                    "notes" to notesInput.text.toString()
                ),
                onSuccess = {
                    Toast.makeText(requireContext(), "Daily check-in saved.", Toast.LENGTH_SHORT).show()
                    notesInput.text.clear()
                    loadLogList(
                        logTypes = listOf("daily_checkin"),
                        container = recentContainer,
                        emptyMessage = "No daily check-ins yet."
                    )
                }
            )
        }

        root.addView(saveButton)

        root.addView(sectionTitle("Recent Daily Check-Ins"))
        root.addView(recentContainer)

        loadLogList(
            logTypes = listOf("daily_checkin"),
            container = recentContainer,
            emptyMessage = "No daily check-ins yet."
        )
    }

    private fun buildPhysicalActivity(root: LinearLayout) {
        root.addView(
            paragraph(
                "Track your steps and movement level. This can help connect activity patterns with energy, sleep, stress, and recovery."
            )
        )

        var activityLevel = ""
        var activityType = ""

        val stepsInput = input("Steps today. Example: 4250")
        stepsInput.inputType = InputType.TYPE_CLASS_NUMBER
        root.addView(stepsInput)

        val goalInput = input("Daily step goal. Example: 8000")
        goalInput.inputType = InputType.TYPE_CLASS_NUMBER
        root.addView(goalInput)

        addSingleQuestion(
            root,
            "What was your activity level today?",
            listOf("Resting", "Light Activity", "Moderate Activity", "Heavy Activity")
        ) {
            activityLevel = it
        }

        addSingleQuestion(
            root,
            "What activity did you do today?",
            listOf("Walking", "Workout", "Sports", "House chores", "Stretching", "None")
        ) {
            activityType = it
        }

        val notesInput = input("Activity notes. Example: I walked after lunch.", 120)
        notesInput.gravity = Gravity.TOP
        root.addView(notesInput)

        val recentContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val saveButton = actionButton("Save Physical Activity")
        saveButton.setOnClickListener {
            val missing = mutableListOf<String>()

            if (stepsInput.text.toString().isBlank()) missing.add("Steps today")
            if (goalInput.text.toString().isBlank()) missing.add("Daily step goal")
            if (activityLevel.isBlank()) missing.add("Activity level")
            if (activityType.isBlank()) missing.add("Activity type")
            if (notesInput.text.toString().isBlank()) missing.add("Activity notes")

            if (missing.isNotEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please answer: ${missing.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val steps = stepsInput.text.toString().toIntOrNull() ?: 0
            val goal = goalInput.text.toString().toIntOrNull() ?: 0

            val status = when {
                goal > 0 && steps >= goal -> "Green - Goal reached"
                steps < 2000 -> "Yellow - Low activity"
                activityLevel == "Heavy Activity" -> "Yellow - Recovery needed"
                else -> "Green - Active"
            }

            val summary = """
                Steps today: $steps
                Step goal: $goal
                Activity level: $activityLevel
                Activity type: $activityType
                Notes: ${notesInput.text}
            """.trimIndent()

            val recommendations = buildString {
                if (goal > 0 && steps < goal) append("Try a short walk or light movement if you feel well. ")
                if (steps < 2000) append("Low movement may affect energy and mood. Consider gentle activity. ")
                if (activityLevel == "Heavy Activity") append("Rest and hydrate after heavy activity. ")
                append("Balance movement with enough sleep, water, and recovery.")
            }

            saveLog(
                logType = "physical_activity",
                title = "Physical Activity",
                status = status,
                summary = summary,
                recommendations = recommendations,
                details = mapOf(
                    "steps" to steps,
                    "goal" to goal,
                    "activityLevel" to activityLevel,
                    "activityType" to activityType,
                    "notes" to notesInput.text.toString()
                ),
                onSuccess = {
                    Toast.makeText(requireContext(), "Physical activity saved.", Toast.LENGTH_SHORT).show()
                    stepsInput.text.clear()
                    goalInput.text.clear()
                    notesInput.text.clear()
                    loadLogList(
                        logTypes = listOf("physical_activity"),
                        container = recentContainer,
                        emptyMessage = "No physical activity logs yet."
                    )
                }
            )
        }

        root.addView(saveButton)

        root.addView(sectionTitle("Recent Physical Activity"))
        root.addView(recentContainer)

        loadLogList(
            logTypes = listOf("physical_activity"),
            container = recentContainer,
            emptyMessage = "No physical activity logs yet."
        )
    }

    private fun buildFilteredLogPage(
        root: LinearLayout,
        description: String,
        logTypes: List<String>,
        emptyMessage: String,
        addButtonText: String,
        destinationId: Int
    ) {
        root.addView(paragraph(description))

        val addButton = actionButton(addButtonText)
        addButton.setOnClickListener {
            findNavController().navigate(destinationId)
        }
        root.addView(addButton)

        root.addView(sectionTitle("Saved Logs"))

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(listContainer)

        loadLogList(
            logTypes = logTypes,
            container = listContainer,
            emptyMessage = emptyMessage
        )
    }

    private fun loadLogList(
        logTypes: List<String>,
        container: LinearLayout,
        emptyMessage: String
    ) {
        container.removeAllViews()

        val user = auth.currentUser

        if (user == null) {
            container.addView(emptyState("Login required", "Please login to view your logs."))
            return
        }

        db.collection("wellness_logs")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents
                    .filter { logTypes.contains(it.getString("logType") ?: "") }
                    .sortedByDescending { it.getLong("createdAt") ?: 0L }

                if (items.isEmpty()) {
                    container.addView(emptyState("No logs yet", emptyMessage))
                    return@addOnSuccessListener
                }

                items.forEach { document ->
                    val title = document.getString("title") ?: "Wellness Log"
                    val status = document.getString("status") ?: "No status"
                    val summary = document.getString("summary") ?: "No summary available."
                    val recommendations =
                        document.getString("recommendations") ?: "No recommendations available."
                    val createdAt = document.getLong("createdAt") ?: 0L
                    val logType = document.getString("logType") ?: "log"

                    container.addView(
                        logCard(
                            logType = logType,
                            title = title,
                            status = status,
                            summary = summary,
                            recommendations = recommendations,
                            timestamp = createdAt
                        )
                    )
                }
            }
            .addOnFailureListener {
                container.addView(
                    emptyState(
                        "Unable to load logs",
                        "Please check your connection and try again."
                    )
                )
            }
    }

    private fun saveLog(
        logType: String,
        title: String,
        status: String,
        summary: String,
        recommendations: String,
        details: Map<String, Any?>,
        onSuccess: () -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please login to save this log.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to logType,
            "title" to title,
            "status" to status,
            "summary" to summary,
            "recommendations" to recommendations,
            "createdAt" to System.currentTimeMillis(),
            "details" to details.mapValues { it.value ?: "" }
        )

        db.collection("wellness_logs")
            .add(data)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to save log.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun logCard(
        logType: String,
        title: String,
        status: String,
        summary: String,
        recommendations: String,
        timestamp: Long
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }

            setOnClickListener {
                showFullLogDialog(
                    logType = logType,
                    title = title,
                    status = status,
                    summary = summary,
                    recommendations = recommendations,
                    timestamp = timestamp
                )
            }
        }

        val topRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = TextView(requireContext()).apply {
            text = getLogIcon(logType)
            textSize = 22f
            gravity = Gravity.CENTER
            background = roundedBg(backgroundColor, Color.rgb(205, 232, 228), 1, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }

        val titleColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val timeView = TextView(requireContext()).apply {
            text = formatFullDate(timestamp)
            textSize = 12f
            setTextColor(textSecondary)
            setPadding(0, dp(3), 0, 0)
        }

        titleColumn.addView(titleView)
        titleColumn.addView(timeView)

        val statusBadge = TextView(requireContext()).apply {
            text = shortStatus(status)
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(statusColor(status))
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(statusBgColor(status), statusColor(status), 1, 30f)
        }

        topRow.addView(icon)
        topRow.addView(titleColumn)
        topRow.addView(statusBadge)

        val summaryView = TextView(requireContext()).apply {
            text = shortText(summary)
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(14), 0, 0)
        }

        val recommendationView = TextView(requireContext()).apply {
            text = "Recommendation: ${shortText(recommendations, 110)}"
            textSize = 13f
            setTextColor(textPrimary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(12), 0, 0)
        }

        val hint = TextView(requireContext()).apply {
            text = "Tap to view full details"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, dp(14), 0, 0)
        }

        card.addView(topRow)
        card.addView(summaryView)
        card.addView(recommendationView)
        card.addView(hint)

        return card
    }

    private fun showFullLogDialog(
        logType: String,
        title: String,
        status: String,
        summary: String,
        recommendations: String,
        timestamp: Long
    ) {
        val context = requireContext()

        val scrollView = ScrollView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(12))
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                30f
            )
        }

        val icon = TextView(context).apply {
            text = getLogIcon(logType)
            textSize = 28f
            gravity = Gravity.CENTER
            background = roundedBg(
                backgroundColor,
                Color.rgb(205, 232, 228),
                1,
                50f
            )
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            gravity = Gravity.CENTER
        }

        val dateView = TextView(context).apply {
            text = formatFullDate(timestamp)
            textSize = 13f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(12))
        }

        val statusView = TextView(context).apply {
            text = status
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(statusColor(status))
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedBg(
                statusBgColor(status),
                statusColor(status),
                1,
                30f
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(18)
            }
        }

        content.addView(icon)
        content.addView(titleView)
        content.addView(dateView)
        content.addView(statusView)

        val cleanedSummary = summary.trim()

        content.addView(
            detailSectionCard(
                title = "Summary",
                body = removeExtraSections(cleanedSummary),
                accentColor = primaryColor
            )
        )

        content.addView(
            detailSectionCard(
                title = "Recommendations",
                body = recommendations.ifBlank {
                    extractSection(cleanedSummary, "Recommendations")
                        ?: "No recommendations available."
                },
                accentColor = Color.rgb(5, 150, 105)
            )
        )

        content.addView(
            detailSectionCard(
                title = "Recovery Tracker",
                body = extractSection(cleanedSummary, "Recovery Tracker")
                    ?: "Continue monitoring your wellness and update your log regularly.",
                accentColor = Color.rgb(37, 99, 235)
            )
        )

        content.addView(
            detailSectionCard(
                title = "Warning Signs",
                body = extractSection(cleanedSummary, "Warning Signs")
                    ?: "Seek medical help if symptoms are severe, persistent, or worsening.",
                accentColor = Color.rgb(217, 119, 6)
            )
        )

        content.addView(
            detailSectionCard(
                title = "Safety Reminder",
                body = "This is not a medical diagnosis. Please consult a healthcare professional for severe, persistent, or worsening symptoms.",
                accentColor = Color.rgb(220, 38, 38)
            )
        )

        scrollView.addView(content)

        AlertDialog.Builder(context)
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun detailSectionCard(
        title: String,
        body: String,
        accentColor: Int
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(
                Color.rgb(250, 253, 252),
                borderColor,
                1,
                22f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val label = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(accentColor)
        }

        val text = TextView(requireContext()).apply {
            this.text = body.trim().ifBlank { "No details available." }
            textSize = 14f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(label)
        card.addView(text)

        return card
    }

    private fun removeExtraSections(text: String): String {
        val stopLabels = listOf(
            "Recommendations:",
            "Recovery Tracker:",
            "Warning Signs:",
            "Safety Reminder:",
            "This is not a medical diagnosis."
        )

        var result = text

        for (label in stopLabels) {
            val index = result.indexOf(label, ignoreCase = true)
            if (index >= 0) {
                result = result.substring(0, index).trim()
            }
        }

        return result.ifBlank { "No summary available." }
    }

    private fun extractSection(
        text: String,
        label: String
    ): String? {
        val startLabel = "$label:"
        val startIndex = text.indexOf(startLabel, ignoreCase = true)

        if (startIndex < 0) return null

        val afterStart = startIndex + startLabel.length

        val possibleEndLabels = listOf(
            "Recommendations:",
            "Recovery Tracker:",
            "Warning Signs:",
            "Safety Reminder:",
            "This is not a medical diagnosis."
        ).filterNot {
            it.equals(startLabel, ignoreCase = true)
        }

        var endIndex = text.length

        for (endLabel in possibleEndLabels) {
            val nextIndex = text.indexOf(endLabel, afterStart, ignoreCase = true)
            if (nextIndex >= 0 && nextIndex < endIndex) {
                endIndex = nextIndex
            }
        }

        return text.substring(afterStart, endIndex).trim().ifBlank { null }
    }

    private fun addTopBar(root: LinearLayout, title: String) {
        val header = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            )
        }

        val menu = TextView(requireContext()).apply {
            text = "☰"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(textPrimary)
            background = roundedBg(surfaceColor, borderColor, 2, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            setOnClickListener {
                (requireActivity() as MainActivity).openDrawer()
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        header.addView(menu)
        header.addView(titleView)
        root.addView(header)
    }

    private fun addSingleQuestion(
        root: LinearLayout,
        title: String,
        options: List<String>,
        onPick: (String) -> Unit
    ) {
        root.addView(sectionTitle(title))

        val optionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(optionContainer)

        var selectedView: TextView? = null

        options.forEach { option ->
            val item = optionView(option)

            item.setOnClickListener {
                selectedView?.background = optionBg(false)
                selectedView = item
                item.background = optionBg(true)
                onPick(option)
            }

            optionContainer.addView(item)
        }
    }

    private fun optionView(textValue: String): TextView {
        return TextView(requireContext()).apply {
            text = textValue
            textSize = 15f
            setTextColor(textPrimary)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = optionBg(false)
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        }
    }

    private fun optionBg(selected: Boolean): GradientDrawable {
        return roundedBg(
            if (selected) backgroundColor else surfaceColor,
            if (selected) primaryColor else borderColor,
            if (selected) 4 else 2,
            22f
        )
    }

    private fun input(hintValue: String, heightDp: Int = 56): EditText {
        return EditText(requireContext()).apply {
            hint = hintValue
            textSize = 14f
            setTextColor(textPrimary)
            setHintTextColor(Color.rgb(120, 120, 120))
            setPadding(dp(16), 0, dp(16), 0)
            background = roundedBg(surfaceColor, borderColor, 2, 18f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun actionButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg(primaryColor, primaryColor, 2, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(8)
            }
        }
    }

    private fun emptyState(title: String, message: String): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(26), dp(22), dp(26))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)
        }

        val icon = TextView(requireContext()).apply {
            text = "🌿"
            textSize = 32f
            gravity = Gravity.CENTER
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val messageView = TextView(requireContext()).apply {
            text = message
            textSize = 14f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(icon)
        card.addView(titleView)
        card.addView(messageView)

        return card
    }

    private fun bigTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(18), 0, dp(4))
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(20), 0, dp(10))
        }
    }

    private fun paragraph(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 15f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, dp(8))
        }
    }

    private fun pageTitle(type: String): String {
        return when (type) {
            "daily_checkin" -> "Daily Check-In"
            "symptom_log" -> "Symptom Log"
            "sleep_log" -> "Sleep Log"
            "mood_stress" -> "Mood & Stress"
            "physical_activity" -> "Physical Activity"
            else -> "Activity"
        }
    }

    private fun getLogIcon(type: String): String {
        return when (type.lowercase()) {
            "daily_checkin" -> "📋"
            "symptom" -> "📝"
            "sleep" -> "🌙"
            "mental" -> "🧠"
            "physical_activity" -> "👣"
            else -> "💙"
        }
    }

    private fun shortStatus(status: String): String {
        return when {
            status.contains("Red", true) -> "Red"
            status.contains("Yellow", true) -> "Yellow"
            status.contains("Green", true) -> "Green"
            else -> "Status"
        }
    }

    private fun statusColor(status: String): Int {
        return when {
            status.contains("Red", true) -> Color.rgb(220, 38, 38)
            status.contains("Yellow", true) -> Color.rgb(217, 119, 6)
            status.contains("Green", true) -> Color.rgb(5, 150, 105)
            else -> primaryColor
        }
    }

    private fun statusBgColor(status: String): Int {
        return when {
            status.contains("Red", true) -> Color.rgb(254, 242, 242)
            status.contains("Yellow", true) -> Color.rgb(255, 251, 235)
            status.contains("Green", true) -> Color.rgb(236, 253, 245)
            else -> backgroundColor
        }
    }

    private fun shortText(text: String, max: Int = 150): String {
        val clean = text.replace("\n", " ").replace("  ", " ").trim()

        return if (clean.length > max) {
            clean.take(max) + "..."
        } else {
            clean
        }
    }

    private fun formatFullDate(timestamp: Long): String {
        return SimpleDateFormat(
            "MMM dd, yyyy - hh:mm a",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    private fun roundedBg(
        bgColor: Int,
        strokeColor: Int,
        strokeWidth: Int,
        radius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}