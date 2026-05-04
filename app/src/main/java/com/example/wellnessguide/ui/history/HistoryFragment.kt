package com.example.wellnessguide.ui.history

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var historyContainer: LinearLayout

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        view.setBackgroundColor(backgroundColor)

        view.findViewById<TextView>(R.id.btnMenuHistory).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        historyContainer = view.findViewById(R.id.historyContainer)

        updatePageText()
        addStatusLegend()
        loadHistory()

        return view
    }

    private fun updatePageText() {
        updateTextRecursively(
            historyContainer,
            "History",
            "Assessment History"
        )

        updateTextRecursively(
            historyContainer,
            "Your saved wellness activity will appear here.",
            "Your saved assessments, wellness logs, tips, and recommendations appear here."
        )
    }

    private fun updateTextRecursively(
        view: View,
        oldText: String,
        newText: String
    ) {
        if (view is TextView && view.text.toString() == oldText) {
            view.text = newText
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextRecursively(view.getChildAt(i), oldText, newText)
            }
        }
    }
    private fun addStatusLegend() {
        val legendCard = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                26f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
                bottomMargin = dp(8)
            }
        }

        val title = TextView(requireContext()).apply {
            text = "Status Guide"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Use these colors to understand your wellness result."
            textSize = 13f
            setTextColor(textSecondary)
            setPadding(0, dp(4), 0, dp(14))
        }

        legendCard.addView(title)
        legendCard.addView(subtitle)

        legendCard.addView(
            legendItem(
                label = "Green",
                meaning = "Low concern / normal monitoring",
                color = Color.rgb(5, 150, 105),
                bgColor = Color.rgb(236, 253, 245)
            )
        )

        legendCard.addView(
            legendItem(
                label = "Yellow",
                meaning = "Needs monitoring / mild to moderate concern",
                color = Color.rgb(217, 119, 6),
                bgColor = Color.rgb(255, 251, 235)
            )
        )

        legendCard.addView(
            legendItem(
                label = "Red",
                meaning = "Seek medical advice / high concern",
                color = Color.rgb(220, 38, 38),
                bgColor = Color.rgb(254, 242, 242)
            )
        )

        historyContainer.addView(legendCard)
    }

    private fun legendItem(
        label: String,
        meaning: String,
        color: Int,
        bgColor: Int
    ): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val badge = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(
                bgColor,
                color,
                1,
                30f
            )

            layoutParams = LinearLayout.LayoutParams(
                dp(76),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val description = TextView(requireContext()).apply {
            text = meaning
            textSize = 13f
            setTextColor(textSecondary)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        }

        row.addView(badge)
        row.addView(description)

        return row
    }
    private fun loadHistory() {
        val user = auth.currentUser

        if (user == null) {
            addEmptyState(
                title = "Login required",
                message = "Please login to view your assessment history."
            )
            return
        }

        db.collection("wellness_logs")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.sortedByDescending {
                    it.getLong("createdAt") ?: 0L
                }

                if (items.isEmpty()) {
                    addEmptyState(
                        title = "No assessment history yet",
                        message = "Complete an assessment or save a wellness check to see it here."
                    )
                    return@addOnSuccessListener
                }

                var lastDate = ""

                for (document in items) {
                    val logType = document.getString("logType") ?: "log"
                    val title = document.getString("title") ?: "Wellness Log"
                    val status = document.getString("status") ?: "No status"
                    val summary = document.getString("summary") ?: "No summary available."
                    val recommendations =
                        document.getString("recommendations") ?: "No recommendations available."
                    val createdAt = document.getLong("createdAt") ?: 0L

                    val dateLabel = formatDateLabel(createdAt)

                    if (dateLabel != lastDate) {
                        addDateHeader(dateLabel)
                        lastDate = dateLabel
                    }

                    addHistoryCard(
                        logType = logType,
                        title = title,
                        status = status,
                        summary = summary,
                        recommendations = recommendations,
                        timestamp = createdAt
                    )
                }
            }
            .addOnFailureListener {
                addEmptyState(
                    title = "Unable to load history",
                    message = "Please check your connection and try again."
                )
            }
    }

    private fun addDateHeader(title: String) {
        val header = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, dp(24), 0, dp(10))
            letterSpacing = 0.02f
        }

        historyContainer.addView(header)
    }

    private fun addHistoryCard(
        logType: String,
        title: String,
        status: String,
        summary: String,
        recommendations: String,
        timestamp: Long
    ) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                26f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }

            elevation = dp(1).toFloat()

            setOnClickListener {
                showFullLogModal(
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

        val iconView = TextView(requireContext()).apply {
            text = getLogIcon(logType)
            textSize = 22f
            gravity = Gravity.CENTER
            background = roundedBg(
                backgroundColor,
                Color.rgb(205, 232, 228),
                1,
                40f
            )
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
            text = formatTime(timestamp)
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
            background = roundedBg(
                statusBgColor(status),
                statusColor(status),
                1,
                30f
            )
        }

        topRow.addView(iconView)
        topRow.addView(titleColumn)
        topRow.addView(statusBadge)

        val summaryView = TextView(requireContext()).apply {
            text = shortText(summary)
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(14), 0, 0)
        }

        val recommendationPreview = TextView(requireContext()).apply {
            text = "Recommendation: ${shortText(recommendations, 110)}"
            textSize = 13f
            setTextColor(textPrimary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(12), 0, 0)
        }

        val tapHint = TextView(requireContext()).apply {
            text = "View full summary"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, dp(14), 0, 0)
        }

        card.addView(topRow)
        card.addView(summaryView)
        card.addView(recommendationPreview)
        card.addView(tapHint)

        historyContainer.addView(card)
    }

    private fun showFullLogModal(
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
            sectionCard(
                title = "Summary",
                body = removeAssessmentExtraSections(cleanedSummary),
                accentColor = primaryColor
            )
        )

        content.addView(
            sectionCard(
                title = "Recommendations",
                body = recommendations.ifBlank {
                    extractSection(cleanedSummary, "Recommendations")
                        ?: "No recommendations available."
                },
                accentColor = Color.rgb(5, 150, 105)
            )
        )

        content.addView(
            sectionCard(
                title = "Recovery Tracker",
                body = extractSection(cleanedSummary, "Recovery Tracker")
                    ?: "Monitor your symptoms and check again after 6 to 12 hours.",
                accentColor = Color.rgb(37, 99, 235)
            )
        )

        content.addView(
            sectionCard(
                title = "Warning Signs",
                body = extractSection(cleanedSummary, "Warning Signs")
                    ?: "Seek medical help if symptoms are severe, persistent, or worsening.",
                accentColor = Color.rgb(217, 119, 6)
            )
        )

        content.addView(
            sectionCard(
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

    private fun sectionCard(
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
            this.text = body.trim()
            textSize = 14f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(label)
        card.addView(text)

        return card
    }

    private fun removeAssessmentExtraSections(text: String): String {
        val stopLabels = listOf(
            "Recommendations:",
            "Recovery Tracker:",
            "Warning Signs:",
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

    private fun addEmptyState(
        title: String,
        message: String
    ) {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                26f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        }

        val icon = TextView(requireContext()).apply {
            text = "🌿"
            textSize = 34f
            gravity = Gravity.CENTER
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 18f
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

        historyContainer.addView(card)
    }

    private fun getLogIcon(type: String): String {
        return when (type.lowercase()) {
            "symptom" -> "📝"
            "lifestyle" -> "🌿"
            "mental" -> "🧠"
            "sleep" -> "🌙"
            "full_assessment" -> "📋"
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

    private fun shortText(
        text: String,
        max: Int = 150
    ): String {
        val clean = text
            .replace("\n", " ")
            .replace("  ", " ")
            .trim()

        return if (clean.length > max) {
            clean.take(max) + "..."
        } else {
            clean
        }
    }

    private fun formatDateLabel(timestamp: Long): String {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val itemDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))

        return if (today == itemDate) {
            "Today"
        } else {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatFullDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun roundedBg(
        bgColor: Int,
        strokeColor: Int,
        strokeWidth: Int = 2,
        radius: Float = 22f
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