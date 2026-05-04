package com.example.wellnessguide.ui.results

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.data.report.PdfReportLogItem
import com.example.wellnessguide.data.report.WellnessPdfReportGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ResultsFeatureFragment : Fragment() {

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    private var pendingPdfLogs: List<WellnessLogItem> = emptyList()
    private var pendingPdfFilterLabel: String = "All dates"

    private val createPdfLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(requireContext(), "PDF export cancelled.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            try {
                val pdfLogs = pendingPdfLogs.map { log ->
                    PdfReportLogItem(
                        logType = log.logType,
                        title = log.title,
                        status = log.status,
                        summary = log.summary,
                        recommendations = log.recommendations,
                        createdAt = log.createdAt
                    )
                }

                WellnessPdfReportGenerator.write(
                    context = requireContext(),
                    uri = uri,
                    logs = pdfLogs,
                    filterLabel = pendingPdfFilterLabel
                )

                Toast.makeText(
                    requireContext(),
                    "PDF report saved successfully.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    e.message ?: "Failed to create PDF report.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    data class WellnessLogItem(
        val logType: String,
        val title: String,
        val status: String,
        val summary: String,
        val recommendations: String,
        val createdAt: Long
    )

    data class QuadItem(
        val first: String,
        val second: String,
        val third: String,
        val fourth: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pageType = arguments?.getString("pageType") ?: "latest_result"

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
            "latest_result" -> buildLatestResult(root)
            "recovery_plan" -> buildRecoveryPlan(root)
            "health_summary" -> buildHealthSummary(root)
            "download_report" -> buildDownloadReport(root)
            else -> buildLatestResult(root)
        }

        scroll.addView(root)
        return scroll
    }

    private fun buildLatestResult(root: LinearLayout) {
        root.addView(
            paragraph(
                "View your most recent wellness result, recommendations, recovery tracker, and warning reminders."
            )
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(container)

        loadLogs { logs ->
            container.removeAllViews()

            val latestFullAssessment = logs.firstOrNull {
                it.logType == "full_assessment"
            }

            val latest = latestFullAssessment ?: logs.firstOrNull()

            if (latest == null) {
                container.addView(
                    emptyState(
                        "No result yet",
                        "Complete a wellness check or save any wellness log to see your latest result here."
                    )
                )

                val startButton = actionButton("Start Wellness Check")
                startButton.setOnClickListener {
                    findNavController().navigate(R.id.startWellnessCheckFragment)
                }
                container.addView(startButton)

                return@loadLogs
            }

            container.addView(statusOverviewCard(latest))

            container.addView(
                infoCard(
                    icon = "📋",
                    title = "Summary",
                    body = latest.summary,
                    accentColor = primaryColor
                )
            )

            container.addView(
                infoCard(
                    icon = "💡",
                    title = "Recommendations",
                    body = latest.recommendations.ifBlank {
                        "Rest, hydrate, monitor your symptoms, and follow your wellness plan."
                    },
                    accentColor = Color.rgb(5, 150, 105)
                )
            )

            container.addView(
                infoCard(
                    icon = "🕒",
                    title = "Recovery Tracker",
                    body = "Check again after 6-12 hours. Notice if your symptoms are improving, staying the same, or getting worse.",
                    accentColor = Color.rgb(217, 119, 6)
                )
            )

            container.addView(
                infoCard(
                    icon = "⚠",
                    title = "Warning Signs",
                    body = "Seek help immediately for chest pain, difficulty breathing, fainting, confusion, severe headache, severe dehydration, or symptoms getting worse.",
                    accentColor = Color.rgb(220, 38, 38)
                )
            )

            val historyButton = secondaryButton("View Assessment History")
            historyButton.setOnClickListener {
                findNavController().navigate(R.id.historyFragment)
            }
            container.addView(historyButton)
        }
    }

    private fun buildRecoveryPlan(root: LinearLayout) {
        root.addView(
            paragraph(
                "Use this page after an assessment to track whether you are improving, the same, or getting worse."
            )
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(container)

        loadLogs { logs ->
            container.removeAllViews()

            val latest = logs.firstOrNull { it.logType == "full_assessment" }
                ?: logs.firstOrNull()

            if (latest == null) {
                container.addView(
                    emptyState(
                        "No recovery plan yet",
                        "Complete a wellness check first so the app can create a recovery plan."
                    )
                )
                return@loadLogs
            }

            container.addView(statusOverviewCard(latest))
            container.addView(sectionTitle("Recommended Recovery Plan"))

            recoveryPlanItems(latest.status).forEach { item ->
                container.addView(
                    infoCard(
                        icon = item.first,
                        title = item.second,
                        body = item.third,
                        accentColor = item.fourth
                    )
                )
            }

            container.addView(sectionTitle("How are you now?"))

            val improvingButton = actionButton("Improving")
            improvingButton.setOnClickListener {
                saveRecoveryUpdate("Improving", "Green - Improving")
            }
            container.addView(improvingButton)

            val sameButton = secondaryButton("Same")
            sameButton.setOnClickListener {
                saveRecoveryUpdate("Same", "Yellow - Needs monitoring")
            }
            container.addView(sameButton)

            val worseButton = warningButton("Getting Worse")
            worseButton.setOnClickListener {
                saveRecoveryUpdate("Getting Worse", "Red - Seek medical advice")
            }
            container.addView(worseButton)

            container.addView(
                infoCard(
                    icon = "⚠",
                    title = "Important",
                    body = "If symptoms are severe, persistent, or getting worse, consult a healthcare professional. This app is not a medical diagnosis.",
                    accentColor = Color.rgb(220, 38, 38)
                )
            )
        }
    }

    private fun buildHealthSummary(root: LinearLayout) {
        root.addView(
            paragraph(
                "This page summarizes your recent wellness logs from the last 7 days."
            )
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(container)

        loadLogs { logs ->
            container.removeAllViews()

            if (logs.isEmpty()) {
                container.addView(
                    emptyState(
                        "No health summary yet",
                        "Save assessments, sleep logs, mood checks, or physical activity logs to generate your health summary."
                    )
                )
                return@loadLogs
            }

            val sevenDaysAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
            val recentLogs = logs.filter { it.createdAt >= sevenDaysAgo }
            val targetLogs = if (recentLogs.isEmpty()) logs.take(10) else recentLogs

            val symptomCount = targetLogs.count {
                it.logType == "symptom" || it.logType == "full_assessment"
            }
            val sleepCount = targetLogs.count { it.logType == "sleep" }
            val moodCount = targetLogs.count { it.logType == "mental" }
            val lifestyleCount = targetLogs.count { it.logType == "lifestyle" }
            val activityCount = targetLogs.count {
                it.logType == "physical_activity" || it.logType == "route_activity"
            }

            container.addView(
                summaryCard(
                    totalLogs = targetLogs.size,
                    latestStatus = targetLogs.firstOrNull()?.status ?: "No status",
                    symptomCount = symptomCount,
                    sleepCount = sleepCount,
                    moodCount = moodCount,
                    lifestyleCount = lifestyleCount,
                    activityCount = activityCount
                )
            )

            container.addView(sectionTitle("Recent Highlights"))

            targetLogs.take(6).forEach { log ->
                container.addView(compactLogCard(log))
            }

            container.addView(
                infoCard(
                    icon = "💡",
                    title = "Health Insight",
                    body = healthInsight(targetLogs),
                    accentColor = primaryColor
                )
            )
        }
    }

    private fun buildDownloadReport(root: LinearLayout) {
        root.addView(
            paragraph(
                "Choose a date filter first, review what will be included, then generate a designed PDF wellness report."
            )
        )

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(container)

        loadLogs { logs ->
            container.removeAllViews()

            if (logs.isEmpty()) {
                container.addView(
                    emptyState(
                        "No report available",
                        "Save wellness logs first before generating a PDF report."
                    )
                )
                return@loadLogs
            }

            var selectedLogs = logs.sortedByDescending { it.createdAt }
            var selectedLabel = "All dates"

            container.addView(
                reportInfoCard(
                    "PDF Report",
                    "Your report will include the latest result, status guide, disclaimer, wellness summary, recommendations, and saved logs."
                )
            )

            container.addView(sectionTitle("Date Filter"))

            val previewContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            val allButton = reportFilterButton("All Logs")
            val todayButton = reportFilterButton("Today")
            val sevenDaysButton = reportFilterButton("Last 7 Days")
            val thirtyDaysButton = reportFilterButton("Last 30 Days")

            val filterButtons = listOf(
                allButton,
                todayButton,
                sevenDaysButton,
                thirtyDaysButton
            )

            fun setSelectedFilter(selectedButton: Button) {
                filterButtons.forEach { button ->
                    val isSelected = button == selectedButton

                    button.setTextColor(
                        if (isSelected) {
                            Color.WHITE
                        } else {
                            textPrimary
                        }
                    )

                    button.background = if (isSelected) {
                        roundedBg(
                            primaryColor,
                            primaryColor,
                            2,
                            22f
                        )
                    } else {
                        roundedBg(
                            surfaceColor,
                            borderColor,
                            2,
                            22f
                        )
                    }
                }
            }

            fun updatePreview(
                filteredLogs: List<WellnessLogItem>,
                label: String,
                selectedButton: Button
            ) {
                selectedLogs = filteredLogs.sortedByDescending { it.createdAt }
                selectedLabel = label

                setSelectedFilter(selectedButton)

                previewContainer.removeAllViews()

                previewContainer.addView(
                    reportPreviewCard(
                        logs = selectedLogs,
                        filterLabel = selectedLabel
                    )
                )

                Toast.makeText(
                    requireContext(),
                    "$label selected. ${selectedLogs.size} log(s) found.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            allButton.setOnClickListener {
                updatePreview(
                    filteredLogs = logs,
                    label = "All dates",
                    selectedButton = allButton
                )
            }

            todayButton.setOnClickListener {
                updatePreview(
                    filteredLogs = filterToday(logs),
                    label = "Today",
                    selectedButton = todayButton
                )
            }

            sevenDaysButton.setOnClickListener {
                updatePreview(
                    filteredLogs = filterLastDays(logs, 7),
                    label = "Last 7 days",
                    selectedButton = sevenDaysButton
                )
            }

            thirtyDaysButton.setOnClickListener {
                updatePreview(
                    filteredLogs = filterLastDays(logs, 30),
                    label = "Last 30 days",
                    selectedButton = thirtyDaysButton
                )
            }

            container.addView(allButton)
            container.addView(todayButton)
            container.addView(sevenDaysButton)
            container.addView(thirtyDaysButton)

            container.addView(sectionTitle("Custom Date Range"))

            container.addView(
                paragraph(
                    "Use this format: yyyy-MM-dd. Example: 2026-05-02"
                )
            )

            val startDateInput = reportInput("Start date: yyyy-MM-dd")
            val endDateInput = reportInput("End date: yyyy-MM-dd")

            container.addView(startDateInput)
            container.addView(endDateInput)

            val applyCustomButton = secondaryButton("Apply Custom Range")
            applyCustomButton.setOnClickListener {
                val startText = startDateInput.text.toString().trim()
                val endText = endDateInput.text.toString().trim()

                val startMillis = parseDateMillis(startText, endOfDay = false)
                val endMillis = parseDateMillis(endText, endOfDay = true)

                if (startMillis == null || endMillis == null) {
                    Toast.makeText(
                        requireContext(),
                        "Please use valid dates in yyyy-MM-dd format.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                if (endMillis < startMillis) {
                    Toast.makeText(
                        requireContext(),
                        "End date must be after start date.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val filtered = logs.filter { log ->
                    log.createdAt >= startMillis && log.createdAt <= endMillis
                }

                selectedLogs = filtered.sortedByDescending { it.createdAt }
                selectedLabel = "$startText to $endText"

                filterButtons.forEach { button ->
                    button.setTextColor(textPrimary)
                    button.background = roundedBg(
                        surfaceColor,
                        borderColor,
                        2,
                        22f
                    )
                }

                previewContainer.removeAllViews()
                previewContainer.addView(
                    reportPreviewCard(
                        logs = selectedLogs,
                        filterLabel = selectedLabel
                    )
                )

                Toast.makeText(
                    requireContext(),
                    "Custom range selected. ${selectedLogs.size} log(s) found.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            container.addView(applyCustomButton)

            container.addView(sectionTitle("Report Preview"))
            container.addView(previewContainer)

            val generateButton = actionButton("Generate PDF Report")
            generateButton.setOnClickListener {
                startPdfExport(
                    logs = selectedLogs,
                    label = selectedLabel
                )
            }

            container.addView(generateButton)

            updatePreview(
                filteredLogs = logs,
                label = "All dates",
                selectedButton = allButton
            )
        }
    }
    private fun startPdfExport(
        logs: List<WellnessLogItem>,
        label: String
    ) {
        if (logs.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No logs found for this date filter.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        pendingPdfLogs = logs.sortedByDescending { it.createdAt }
        pendingPdfFilterLabel = label

        val fileDate = SimpleDateFormat(
            "yyyyMMdd_HHmm",
            Locale.getDefault()
        ).format(Date())

        createPdfLauncher.launch("Wellness_Report_$fileDate.pdf")
    }

    private fun filterToday(logs: List<WellnessLogItem>): List<WellnessLogItem> {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val end = calendar.timeInMillis

        return logs.filter { log ->
            log.createdAt >= start && log.createdAt <= end
        }
    }

    private fun filterLastDays(
        logs: List<WellnessLogItem>,
        days: Int
    ): List<WellnessLogItem> {
        val start = System.currentTimeMillis() - days.toLong() * 24L * 60L * 60L * 1000L

        return logs.filter { log ->
            log.createdAt >= start
        }
    }

    private fun parseDateMillis(
        value: String,
        endOfDay: Boolean
    ): Long? {
        return try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val date = formatter.parse(value) ?: return null

            val calendar = Calendar.getInstance().apply {
                time = date

                if (endOfDay) {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                } else {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }

            calendar.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun loadLogs(onLoaded: (List<WellnessLogItem>) -> Unit) {
        val user = auth.currentUser

        if (user == null) {
            onLoaded(emptyList())
            return
        }

        db.collection("wellness_logs")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                val logs = result.documents.map { document ->
                    WellnessLogItem(
                        logType = document.getString("logType") ?: "log",
                        title = document.getString("title") ?: "Wellness Log",
                        status = document.getString("status") ?: "No status",
                        summary = document.getString("summary") ?: "No summary available.",
                        recommendations = document.getString("recommendations") ?: "No recommendations available.",
                        createdAt = document.getLong("createdAt") ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                onLoaded(logs)
            }
            .addOnFailureListener {
                onLoaded(emptyList())
            }
    }

    private fun saveRecoveryUpdate(
        recoveryStatus: String,
        status: String
    ) {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please login to save recovery update.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val recommendations = when (recoveryStatus) {
            "Improving" -> "Continue resting, hydrating, and monitoring your symptoms."
            "Same" -> "Monitor closely. If symptoms continue, consider consulting a healthcare professional."
            else -> "Symptoms getting worse may need medical attention. Please seek professional help if needed."
        }

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to "recovery_update",
            "title" to "Recovery Update",
            "status" to status,
            "summary" to "Recovery status: $recoveryStatus",
            "recommendations" to recommendations,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("wellness_logs")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Recovery update saved.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to save recovery update.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun recoveryPlanItems(status: String): List<QuadItem> {
        val isRed = status.contains("Red", true)
        val isYellow = status.contains("Yellow", true)

        return when {
            isRed -> listOf(
                QuadItem("🔴", "Seek Medical Advice", "Your latest status is high concern. It is safer to consult a healthcare professional.", Color.rgb(220, 38, 38)),
                QuadItem("🕒", "Monitor Closely", "Check your symptoms frequently and do not ignore worsening signs.", Color.rgb(217, 119, 6)),
                QuadItem("💧", "Hydrate and Rest", "Drink water and avoid heavy activity while monitoring your condition.", Color.rgb(5, 150, 105))
            )

            isYellow -> listOf(
                QuadItem("🟡", "Monitor Symptoms", "Check again after 6-12 hours and see if symptoms improve or worsen.", Color.rgb(217, 119, 6)),
                QuadItem("🌿", "Rest and Recover", "Prioritize sleep, water intake, and reducing stress.", Color.rgb(5, 150, 105)),
                QuadItem("📋", "Follow Recommendations", "Review your previous tips and avoid possible triggers.", primaryColor)
            )

            else -> listOf(
                QuadItem("🟢", "Continue Healthy Routine", "Your latest status is low concern. Keep maintaining your healthy habits.", Color.rgb(5, 150, 105)),
                QuadItem("💧", "Stay Hydrated", "Drink enough water and monitor energy levels.", primaryColor),
                QuadItem("🛌", "Protect Sleep", "Keep a consistent sleep schedule and reduce screen time before bed.", Color.rgb(217, 119, 6))
            )
        }
    }

    private fun healthInsight(logs: List<WellnessLogItem>): String {
        val hasRed = logs.any { it.status.contains("Red", true) }
        val hasYellow = logs.any { it.status.contains("Yellow", true) }
        val hasSleep = logs.any { it.logType == "sleep" }
        val hasMental = logs.any { it.logType == "mental" }
        val hasActivity = logs.any {
            it.logType == "physical_activity" || it.logType == "route_activity"
        }

        return when {
            hasRed -> "You have at least one high concern log. Review warning signs and consider seeking medical advice if symptoms are severe or worsening."
            hasYellow -> "Some recent logs need monitoring. Continue tracking your symptoms, sleep, stress, and lifestyle factors."
            hasSleep && hasMental -> "You are tracking both sleep and mental wellness. This can help you notice patterns between rest, mood, stress, and energy."
            hasActivity -> "You are tracking physical activity. Movement, hydration, and recovery can support your overall wellness."
            else -> "Continue saving daily check-ins and assessments to build a more complete health summary."
        }
    }

    private fun statusOverviewCard(log: WellnessLogItem): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = roundedBg(surfaceColor, borderColor, 2, 28f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(8)
            }
        }

        val type = TextView(requireContext()).apply {
            text = displayLogType(log.logType)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
        }

        val title = TextView(requireContext()).apply {
            text = log.title
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(8), 0, 0)
        }

        val status = TextView(requireContext()).apply {
            text = shortStatus(log.status)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(statusColor(log.status))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedBg(statusBgColor(log.status), statusColor(log.status), 1, 30f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val time = TextView(requireContext()).apply {
            text = formatFullDate(log.createdAt)
            textSize = 13f
            setTextColor(textSecondary)
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(type)
        card.addView(title)
        card.addView(status)
        card.addView(time)

        return card
    }

    private fun summaryCard(
        totalLogs: Int,
        latestStatus: String,
        symptomCount: Int,
        sleepCount: Int,
        moodCount: Int,
        lifestyleCount: Int,
        activityCount: Int
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = roundedBg(surfaceColor, borderColor, 2, 28f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        card.addView(summaryRow("Total Logs", totalLogs.toString()))
        card.addView(summaryRow("Latest Status", shortStatus(latestStatus)))
        card.addView(summaryRow("Symptoms / Assessments", symptomCount.toString()))
        card.addView(summaryRow("Sleep Logs", sleepCount.toString()))
        card.addView(summaryRow("Mood & Stress Logs", moodCount.toString()))
        card.addView(summaryRow("Lifestyle Logs", lifestyleCount.toString()))
        card.addView(summaryRow("Physical Activity Logs", activityCount.toString()))

        return card
    }

    private fun summaryRow(label: String, value: String): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(textSecondary)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val valueView = TextView(requireContext()).apply {
            text = value
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        row.addView(labelView)
        row.addView(valueView)

        return row
    }

    private fun compactLogCard(log: WellnessLogItem): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(surfaceColor, borderColor, 2, 24f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val title = TextView(requireContext()).apply {
            text = "${logIcon(log.logType)} ${log.title}"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val status = TextView(requireContext()).apply {
            text = "${shortStatus(log.status)} • ${formatShortDate(log.createdAt)}"
            textSize = 12f
            setTextColor(statusColor(log.status))
            setPadding(0, dp(6), 0, 0)
        }

        val summary = TextView(requireContext()).apply {
            text = shortText(log.summary, 130)
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(title)
        card.addView(status)
        card.addView(summary)

        return card
    }

    private fun infoCard(
        icon: String,
        title: String,
        body: String,
        accentColor: Int
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(surfaceColor, borderColor, 2, 24f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }

        val iconView = TextView(requireContext()).apply {
            text = icon
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(accentColor)
            background = roundedBg(backgroundColor, borderColor, 1, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
        }

        val textColumn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(14)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val bodyView = TextView(requireContext()).apply {
            text = body
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(6), 0, 0)
        }

        textColumn.addView(titleView)
        textColumn.addView(bodyView)

        card.addView(iconView)
        card.addView(textColumn)

        return card
    }

    private fun reportInfoCard(
        title: String,
        body: String
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                24f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(8)
            }
        }

        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val bodyView = TextView(requireContext()).apply {
            text = body
            textSize = 13f
            setTextColor(textSecondary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(8), 0, 0)
        }

        card.addView(titleView)
        card.addView(bodyView)

        return card
    }

    private fun reportInput(hintValue: String): EditText {
        return EditText(requireContext()).apply {
            hint = hintValue
            inputType = InputType.TYPE_CLASS_DATETIME
            textSize = 14f
            setTextColor(textPrimary)
            setHintTextColor(Color.rgb(120, 120, 120))
            setPadding(dp(16), 0, dp(16), 0)

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                18f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun reportFilterButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), 0, dp(18), 0)

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                22f
            )

            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun reportPreviewCard(
        logs: List<WellnessLogItem>,
        filterLabel: String
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                24f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
        }

        if (logs.isEmpty()) {
            val emptyTitle = TextView(requireContext()).apply {
                text = "No logs found"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(textPrimary)
            }

            val emptyBody = TextView(requireContext()).apply {
                text = "There are no saved wellness logs for this date filter. Try All Logs or another date range."
                textSize = 13f
                setTextColor(textSecondary)
                setLineSpacing(4f, 1f)
                setPadding(0, dp(8), 0, 0)
            }

            card.addView(emptyTitle)
            card.addView(emptyBody)

            return card
        }

        val latest = logs.maxByOrNull { it.createdAt }
        val latestStatus = latest?.status ?: "No status"

        val symptomCount = logs.count {
            it.logType == "symptom" || it.logType == "full_assessment"
        }

        val sleepCount = logs.count {
            it.logType == "sleep"
        }

        val mentalCount = logs.count {
            it.logType == "mental"
        }

        val activityCount = logs.count {
            it.logType == "physical_activity" || it.logType == "route_activity"
        }

        val title = TextView(requireContext()).apply {
            text = "Report Preview"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        val summary = TextView(requireContext()).apply {
            text = """
                Date Filter: $filterLabel
                Total Logs: ${logs.size}
                Latest Status: ${shortStatus(latestStatus)}
                Symptoms / Assessments: $symptomCount
                Sleep Logs: $sleepCount
                Mood & Stress Logs: $mentalCount
                Physical Activity Logs: $activityCount
            """.trimIndent()

            textSize = 14f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(10), 0, 0)
        }

        val sections = TextView(requireContext()).apply {
            text = """
                Included Sections:
                ✓ Latest Result
                ✓ Status Guide
                ✓ Wellness Summary
                ✓ Recommendations
                ✓ Saved Logs
                ✓ Safety Disclaimer
            """.trimIndent()

            textSize = 14f
            setTextColor(textPrimary)
            setLineSpacing(5f, 1f)
            setPadding(0, dp(14), 0, 0)
        }

        val recentTitle = TextView(requireContext()).apply {
            text = "Recent Included Logs"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(16), 0, dp(6))
        }

        card.addView(title)
        card.addView(summary)
        card.addView(sections)
        card.addView(recentTitle)

        logs.take(3).forEach { log ->
            val item = TextView(requireContext()).apply {
                text = "${logIcon(log.logType)} ${log.title} • ${shortStatus(log.status)}"
                textSize = 13f
                setTextColor(textSecondary)
                setPadding(0, dp(4), 0, 0)
            }

            card.addView(item)
        }

        if (logs.size > 3) {
            val more = TextView(requireContext()).apply {
                text = "+ ${logs.size - 3} more logs included"
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(primaryColor)
                setPadding(0, dp(8), 0, 0)
            }

            card.addView(more)
        }

        return card
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

    private fun emptyState(
        title: String,
        message: String
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(26), dp(22), dp(26))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        }

        val icon = TextView(requireContext()).apply {
            text = "🌿"
            textSize = 34f
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
            }
        }
    }

    private fun secondaryButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(textPrimary)
            background = roundedBg(surfaceColor, borderColor, 2, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(12)
            }
        }
    }

    private fun warningButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg(
                Color.rgb(220, 38, 38),
                Color.rgb(220, 38, 38),
                2,
                22f
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(12)
            }
        }
    }

    private fun pageTitle(type: String): String {
        return when (type) {
            "latest_result" -> "Latest Result"
            "recovery_plan" -> "Recovery Plan"
            "health_summary" -> "Health Summary"
            "download_report" -> "Download Report"
            else -> "Results"
        }
    }

    private fun displayLogType(type: String): String {
        return when (type.lowercase()) {
            "daily_checkin" -> "Daily Check-In"
            "symptom" -> "Symptom Assessment"
            "lifestyle" -> "Lifestyle Check"
            "mental" -> "Mental Wellness"
            "sleep" -> "Sleep Tracker"
            "physical_activity" -> "Physical Activity"
            "route_activity" -> "Walk Route"
            "recovery_update" -> "Recovery Update"
            "full_assessment" -> "Full Wellness Assessment"
            else -> "Wellness Log"
        }
    }

    private fun logIcon(type: String): String {
        return when (type.lowercase()) {
            "daily_checkin" -> "📋"
            "symptom" -> "📝"
            "lifestyle" -> "🌿"
            "mental" -> "🧠"
            "sleep" -> "🌙"
            "physical_activity" -> "👣"
            "route_activity" -> "🗺"
            "recovery_update" -> "🕒"
            "full_assessment" -> "📋"
            else -> "💙"
        }
    }

    private fun shortStatus(status: String): String {
        return when {
            status.contains("Red", true) -> "Red"
            status.contains("Yellow", true) -> "Yellow"
            status.contains("Green", true) -> "Green"
            else -> status
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

    private fun shortText(text: String, max: Int): String {
        val clean = text.replace("\n", " ").replace("  ", " ").trim()
        return if (clean.length > max) clean.take(max) + "..." else clean
    }

    private fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatFullDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"
        return SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(timestamp))
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