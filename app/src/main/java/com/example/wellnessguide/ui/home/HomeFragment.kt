package com.example.wellnessguide.ui.home
import android.view.WindowManager
import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.notifications.AppNotificationItem
import com.example.wellnessguide.notifications.AppNotificationStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.wellnessguide.recent.RecentActivityItem
import com.example.wellnessguide.recent.RecentActivityStore
class HomeFragment : Fragment(), SensorEventListener {
    private lateinit var recentActivitiesContainer: LinearLayout
    private lateinit var recentActivitiesSubtitle: TextView
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var stepsText: TextView

    private lateinit var todayStatusText: TextView
    private lateinit var todayMessageText: TextView
    private lateinit var todayUpdatedText: TextView
    private lateinit var todaySleepText: TextView
    private lateinit var todayWaterText: TextView
    private lateinit var todayStressText: TextView

    private var wellnessListener: ListenerRegistration? = null

    private var latestStatus = "Green"
    private var latestTitle = "No wellness check yet"
    private var latestRecommendation = "Start a wellness check to see your latest status."
    private var latestUpdatedAt = 0L

    private var todaySleep = "No log"
    private var todayStress = "No log"
    private var todayWaterCups = 0

    private var sleepTimer: CountDownTimer? = null
    private var sleepTimerState = "idle" // idle, running, paused, stopped
    private var sleepStartMillis = 0L
    private var sleepElapsedBeforeStart = 0L
    private val sleepGoalMillis = 8L * 60L * 60L * 1000L
    private lateinit var sleepQuickButton: TextView

    private val stepPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startStepCounter()
            } else {
                stepsText.text = "Permission needed"
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        fun saveSymptom(type: String, severity: String) {
            val user = auth.currentUser ?: return

            val data = hashMapOf(
                "userId" to user.uid,
                "type" to type,
                "severity" to severity,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("symptoms")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "$type saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }

        val greetingText = view.findViewById<TextView>(R.id.txtGreeting)
        loadHomeGreeting(auth, db, greetingText)

        stepsText = view.findViewById(R.id.txtStepsToday)

        todayStatusText = view.findViewById(R.id.txtTodayWellnessStatus)
        todayMessageText = view.findViewById(R.id.txtTodayWellnessMessage)
        todayUpdatedText = view.findViewById(R.id.txtTodayWellnessUpdated)
        todaySleepText = view.findViewById(R.id.txtTodaySleep)
        todayWaterText = view.findViewById(R.id.txtTodayWater)
        todayStressText = view.findViewById(R.id.txtTodayStress)
        recentActivitiesContainer = view.findViewById(R.id.recentActivitiesContainer)
        recentActivitiesSubtitle = view.findViewById(R.id.txtRecentActivitiesSubtitle)

        view.findViewById<TextView>(R.id.btnViewAssessmentHistory).setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        loadRecentActivities()
        resetSleepTimerIfNewDay()
        restoreSleepTimer()

        todayWaterCups = getTodayWaterCups()
        updateTodayWellnessCard()
        startLiveWellnessStatus(auth, db)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        checkStepPermission()

        view.findViewById<Button>(R.id.btnHeadache).setOnClickListener {
            saveSymptom("Headache", "Assessment opened")

            addRecentActivity(
                "Headache assessment opened",
                "You opened the headache assessment.",
                "headache"
            )

            showHeadacheDialog()
        }

        view.findViewById<Button>(R.id.btnFever).setOnClickListener {
            saveSymptom("Fever", "Assessment opened")

            addRecentActivity(
                "Fever assessment opened",
                "You opened the fever assessment.",
                "fever"
            )

            showFeverDialog()
        }

        view.findViewById<Button>(R.id.btnCough).setOnClickListener {
            saveSymptom("Cough", "Assessment opened")

            addRecentActivity(
                "Cough assessment opened",
                "You opened the cough assessment.",
                "cough"
            )

            showCoughDialog()
        }

        view.findViewById<TextView>(R.id.btnMenuHome).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        view.findViewById<TextView>(R.id.btnNotifications).setOnClickListener {
            showNotificationsPanel()
        }

        view.findViewById<TextView>(R.id.btnQuickWater).setOnClickListener {
            todayWaterCups += 1
            saveTodayWaterCups(todayWaterCups)
            updateTodayWellnessCard()

            addRecentActivity(
                "Water logged",
                "You added 1 cup of water. Total today: $todayWaterCups cup(s).",
                "water"
            )

            showQuickActionDialog(
                "Water Added",
                "Great! You logged 1 cup of water.\n\nToday’s water intake: $todayWaterCups cup(s).",
                "💧",
                Color.rgb(238, 248, 247),
                Color.rgb(205, 232, 228)
            )
        }

        view.findViewById<TextView>(R.id.btnQuickEyes).setOnClickListener {
            addRecentActivity(
                "Eye rest opened",
                "You viewed the 20-20-20 eye rest guide.",
                "eyes"
            )

            showQuickActionDialog(
                "Rest Your Eyes",
                "Try the 20-20-20 rule:\n\nEvery 20 minutes, look at something 20 feet away for 20 seconds.",
                "👁",
                Color.rgb(255, 247, 232),
                Color.rgb(243, 217, 164)
            )
        }

        view.findViewById<TextView>(R.id.btnQuickLog).setOnClickListener {
            addRecentActivity(
                "Symptom assessment opened",
                "You opened the symptom assessment page.",
                "symptom"
            )

            findNavController().navigate(R.id.assessmentFragment)
        }

        sleepQuickButton = view.findViewById(R.id.btnQuickSleep)
        updateSleepQuickButtonText()

        sleepQuickButton.setOnClickListener {
            handleSleepTimerClick()
        }

        return view
    }

    private fun startLiveWellnessStatus(
        auth: FirebaseAuth,
        db: FirebaseFirestore
    ) {
        val user = auth.currentUser ?: return

        wellnessListener?.remove()

        wellnessListener = db.collection("wellness_logs")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                val logs = snapshot.documents.sortedByDescending {
                    it.getLong("createdAt") ?: it.getLong("timestamp") ?: 0L
                }

                val latestLog = logs.firstOrNull()

                if (latestLog != null) {
                    latestStatus = latestLog.getString("status") ?: "Green"
                    latestTitle = latestLog.getString("title") ?: "Wellness Log"
                    latestRecommendation =
                        latestLog.getString("recommendations")
                            ?: latestLog.getString("summary")
                                    ?: "Keep monitoring your wellness."
                    latestUpdatedAt =
                        latestLog.getLong("createdAt")
                            ?: latestLog.getLong("timestamp")
                                    ?: 0L

                    notifyForLatestStatusOnce(
                        status = latestStatus,
                        title = latestTitle,
                        createdAt = latestUpdatedAt
                    )
                }

                val todayLogs = logs.filter {
                    isToday(it.getLong("createdAt") ?: it.getLong("timestamp") ?: 0L)
                }

                val sleepLog = todayLogs.firstOrNull {
                    it.getString("logType") == "sleep"
                }

                val mentalLog = todayLogs.firstOrNull {
                    it.getString("logType") == "mental"
                }

                todaySleep = sleepLog?.let {
                    extractSleepValue(it.getString("summary") ?: "")
                } ?: "No log"

                todayStress = mentalLog?.let {
                    extractStressValue(it.getString("summary") ?: "")
                } ?: "No log"

                updateTodayWellnessCard()
            }
    }

    private fun updateTodayWellnessCard() {
        if (!::todayStatusText.isInitialized) return

        val statusLabel = when {
            latestStatus.contains("Red", true) -> "🔴 High Concern"
            latestStatus.contains("Yellow", true) -> "🟡 Monitor"
            latestStatus.contains("Green", true) -> "🟢 Low Concern"
            else -> "🟢 Low Concern"
        }

        todayStatusText.text = statusLabel
        todayStatusText.setTextColor(statusColor(latestStatus))

        todayMessageText.text = "$latestTitle\n$latestRecommendation"

        todayUpdatedText.text = if (latestUpdatedAt > 0L) {
            "Updated ${formatSmartTime(latestUpdatedAt)}"
        } else {
            "No wellness log yet"
        }

        todaySleepText.text = "💤 ${currentSleepDisplay()}"
        todayWaterText.text = "💧 $todayWaterCups cups"
        todayStressText.text = "😌 $todayStress"
    }

    private fun extractSleepValue(summary: String): String {
        val lower = summary.lowercase()

        val durationRegex = Regex("(\\d+)\\s*hours?(\\s*\\d+\\s*minutes?)?")
        val match = durationRegex.find(lower)

        if (match != null) {
            return match.value
                .replaceFirstChar { it.uppercase() }
                .replace("hours", "hrs")
                .replace("hour", "hr")
                .replace("minutes", "min")
                .replace("minute", "min")
        }

        return when {
            lower.contains("poor") -> "Poor"
            lower.contains("fair") -> "Fair"
            lower.contains("good") -> "Good"
            lower.contains("excellent") -> "Excellent"
            else -> "Logged"
        }
    }

    private fun extractStressValue(summary: String): String {
        val lower = summary.lowercase()

        return when {
            lower.contains("high stress") || lower.contains("stress: high") -> "High"
            lower.contains("moderate stress") || lower.contains("stress: moderate") -> "Moderate"
            lower.contains("low stress") || lower.contains("stress: low") -> "Low"
            lower.contains("anxious") -> "Anxious"
            lower.contains("tired") -> "Tired"
            lower.contains("sad") -> "Sad"
            lower.contains("overwhelmed") -> "Overwhelmed"
            lower.contains("irritable") -> "Irritable"
            else -> "Logged"
        }
    }

    private fun statusColor(status: String): Int {
        return when {
            status.contains("Red", true) -> Color.rgb(220, 38, 38)
            status.contains("Yellow", true) -> Color.rgb(217, 119, 6)
            status.contains("Green", true) -> Color.rgb(5, 150, 105)
            else -> Color.rgb(5, 150, 105)
        }
    }

    private fun getTodayWaterCups(): Int {
        val prefs = requireContext().getSharedPreferences("home_wellness_prefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("water_date", "")
        val today = todayKey()

        if (savedDate != today) {
            prefs.edit()
                .putString("water_date", today)
                .putInt("water_cups", 0)
                .apply()

            return 0
        }

        return prefs.getInt("water_cups", 0)
    }

    private fun saveTodayWaterCups(cups: Int) {
        val prefs = requireContext().getSharedPreferences("home_wellness_prefs", Context.MODE_PRIVATE)

        prefs.edit()
            .putString("water_date", todayKey())
            .putInt("water_cups", cups)
            .apply()
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false

        val today = todayKey()
        val itemDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))

        return today == itemDate
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    private fun formatSmartTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minute = 60 * 1000L
        val hour = 60 * minute
        val day = 24 * hour

        return when {
            diff < minute -> "just now"
            diff < hour -> "${diff / minute} min ago"
            diff < day -> "${diff / hour} hr ago"
            else -> SimpleDateFormat(
                "MMM dd, hh:mm a",
                Locale.getDefault()
            ).format(Date(timestamp))
        }
    }

    private fun notifyForLatestStatusOnce(
        status: String,
        title: String,
        createdAt: Long
    ) {
        if (createdAt <= 0L) return

        val prefs = requireContext().getSharedPreferences("home_wellness_prefs", Context.MODE_PRIVATE)
        val lastNotified = prefs.getLong("last_status_notification", 0L)

        if (lastNotified == createdAt) return

        when {
            status.contains("Red", true) -> {
                AppNotificationStore.add(
                    requireContext(),
                    "High concern wellness status",
                    "$title is marked Red. Please review warning signs or seek medical advice if symptoms are severe or worsening.",
                    "warning"
                )
            }

            status.contains("Yellow", true) -> {
                AppNotificationStore.add(
                    requireContext(),
                    "Wellness status needs monitoring",
                    "$title is marked Yellow. Check again after a few hours and monitor if symptoms improve or worsen.",
                    "status"
                )
            }
        }

        prefs.edit()
            .putLong("last_status_notification", createdAt)
            .apply()
    }

    private fun handleSleepTimerClick() {
        when (sleepTimerState) {
            "running" -> {
                pauseSleepTimer()
            }

            "paused" -> {
                stopSleepTimer()
            }

            else -> {
                startSleepTimer()
            }
        }
    }

    private fun startSleepTimer() {
        sleepTimer?.cancel()

        sleepTimerState = "running"
        sleepElapsedBeforeStart = 0L
        sleepStartMillis = System.currentTimeMillis()

        saveSleepTimerPrefs()

        addRecentActivity(
            "Sleep timer started",
            "Your sleep timer is now running.",
            "sleep"
        )

        Toast.makeText(
            requireContext(),
            "Sleep timer started.",
            Toast.LENGTH_SHORT
        ).show()

        runSleepTimerLoop()
        updateSleepQuickButtonText()
        updateTodayWellnessCard()
    }

    private fun pauseSleepTimer() {
        sleepTimer?.cancel()

        val now = System.currentTimeMillis()
        sleepElapsedBeforeStart =
            (sleepElapsedBeforeStart + (now - sleepStartMillis)).coerceAtLeast(0L)

        sleepTimerState = "paused"
        sleepStartMillis = 0L

        saveSleepTimerPrefs()

        addRecentActivity(
            "Sleep timer paused",
            "Paused at ${formatDuration(sleepElapsedBeforeStart)}.",
            "sleep"
        )

        Toast.makeText(
            requireContext(),
            "Sleep timer paused at ${formatDuration(sleepElapsedBeforeStart)}.",
            Toast.LENGTH_SHORT
        ).show()

        updateSleepQuickButtonText()
        updateTodayWellnessCard()
    }

    private fun stopSleepTimer() {
        sleepTimer?.cancel()

        if (sleepTimerState == "running" && sleepStartMillis > 0L) {
            val now = System.currentTimeMillis()
            sleepElapsedBeforeStart =
                (sleepElapsedBeforeStart + (now - sleepStartMillis)).coerceAtLeast(0L)
        }

        sleepTimerState = "stopped"
        sleepStartMillis = 0L

        saveSleepTimerPrefs()

        addRecentActivity(
            "Sleep duration saved",
            "Saved sleep duration: ${formatDuration(sleepElapsedBeforeStart)}.",
            "sleep"
        )

        saveQuickSleepLogToFirebase(sleepElapsedBeforeStart)

        Toast.makeText(
            requireContext(),
            "Sleep duration saved: ${formatDuration(sleepElapsedBeforeStart)}.",
            Toast.LENGTH_SHORT
        ).show()

        updateSleepQuickButtonText()
        updateTodayWellnessCard()
    }

    private fun saveQuickSleepLogToFirebase(durationMillis: Long) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please login to save your sleep log.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (durationMillis <= 0L) {
            Toast.makeText(
                requireContext(),
                "Sleep duration is too short to save.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val durationText = formatDuration(durationMillis)

        val status = when {
            durationMillis >= 7L * 60L * 60L * 1000L -> "Green - Good sleep"
            durationMillis >= 4L * 60L * 60L * 1000L -> "Yellow - Below sleep goal"
            else -> "Yellow - Needs monitoring"
        }

        val recommendations = when {
            durationMillis >= 7L * 60L * 60L * 1000L ->
                "Good job reaching a healthy sleep duration. Keep a consistent sleep schedule."

            durationMillis >= 4L * 60L * 60L * 1000L ->
                "Try to sleep earlier and aim for 7 to 8 hours of rest."

            else ->
                "Your sleep duration is low. Try to rest more and avoid screens before bedtime."
        }

        val summary = """
        Source: Quick Sleep Timer
        Sleep duration: $durationText
        Sleep goal: 08:00:00
    """.trimIndent()

        val details = mapOf(
            "source" to "Quick Sleep Timer",
            "sleepDuration" to durationText,
            "durationMillis" to durationMillis,
            "goal" to "08:00:00"
        )

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to "sleep",
            "title" to "Sleep Log",
            "status" to status,
            "summary" to summary,
            "recommendations" to recommendations,
            "createdAt" to System.currentTimeMillis(),
            "details" to details
        )

        FirebaseFirestore.getInstance()
            .collection("wellness_logs")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Saved to Sleep Log.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to save sleep log.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun runSleepTimerLoop() {
        sleepTimer?.cancel()

        val remaining = (sleepGoalMillis - currentSleepElapsedMillis()).coerceAtLeast(0L)

        sleepTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                updateTodayWellnessCard()
            }

            override fun onFinish() {
                sleepTimerState = "stopped"
                sleepElapsedBeforeStart = sleepGoalMillis
                sleepStartMillis = 0L

                saveSleepTimerPrefs()
                saveQuickSleepLogToFirebase(sleepElapsedBeforeStart)
                AppNotificationStore.add(
                    requireContext(),
                    "Sleep goal complete",
                    "You completed an 8-hour sleep timer.",
                    "sleep"
                )
                addRecentActivity(
                    "Sleep goal completed",
                    "You completed an 8-hour sleep timer.",
                    "sleep"
                )
                Toast.makeText(
                    requireContext(),
                    "Sleep goal complete.",
                    Toast.LENGTH_LONG
                ).show()

                updateSleepQuickButtonText()
                updateTodayWellnessCard()
            }
        }.start()
    }

    private fun restoreSleepTimer() {
        val prefs = requireContext().getSharedPreferences("sleep_timer_prefs", Context.MODE_PRIVATE)

        sleepTimerState = prefs.getString("state", "idle") ?: "idle"
        sleepStartMillis = prefs.getLong("start_millis", 0L)
        sleepElapsedBeforeStart = prefs.getLong("elapsed_before_start", 0L)

        if (sleepTimerState == "running" && sleepStartMillis > 0L) {
            runSleepTimerLoop()
        }

        updateSleepQuickButtonText()
        updateTodayWellnessCard()
    }

    private fun resetSleepTimerIfNewDay() {
        val prefs = requireContext().getSharedPreferences("sleep_timer_prefs", Context.MODE_PRIVATE)
        val savedDate = prefs.getString("sleep_date", "")

        if (savedDate != todayKey()) {
            sleepTimer?.cancel()

            sleepTimerState = "idle"
            sleepStartMillis = 0L
            sleepElapsedBeforeStart = 0L

            saveSleepTimerPrefs()
        }
    }

    private fun saveSleepTimerPrefs() {
        requireContext()
            .getSharedPreferences("sleep_timer_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("sleep_date", todayKey())
            .putString("state", sleepTimerState)
            .putLong("start_millis", sleepStartMillis)
            .putLong("elapsed_before_start", sleepElapsedBeforeStart)
            .apply()
    }

    private fun currentSleepElapsedMillis(): Long {
        return if (sleepTimerState == "running" && sleepStartMillis > 0L) {
            sleepElapsedBeforeStart + (System.currentTimeMillis() - sleepStartMillis)
        } else {
            sleepElapsedBeforeStart
        }.coerceAtLeast(0L)
    }

    private fun currentSleepDisplay(): String {
        val elapsed = currentSleepElapsedMillis()

        return when (sleepTimerState) {
            "running" -> "Sleep running ${formatDuration(elapsed)}"
            "paused" -> "Paused ${formatDuration(elapsed)}"
            "stopped" -> {
                if (elapsed > 0L) {
                    "Saved ${formatDuration(elapsed)}"
                } else {
                    todaySleep
                }
            }
            else -> {
                if (todaySleep != "No log") {
                    todaySleep
                } else {
                    "No log"
                }
            }
        }
    }

    private fun updateSleepQuickButtonText() {
        if (!::sleepQuickButton.isInitialized) return

        sleepQuickButton.text = when (sleepTimerState) {
            "running" -> "⏸\nPause"
            "paused" -> "⏹\nStop"
            "stopped" -> "🌙\nStart Sleep"
            else -> "🌙\nStart Sleep"
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
    }

    private fun showHeadacheDialog() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 20, 36, 20)
        }

        val instruction = TextView(context).apply {
            text = "Click on the area where you feel pain"
            textSize = 13f
            setTextColor(Color.rgb(75, 85, 99))
            gravity = Gravity.CENTER
        }

        val headView = HeadacheView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                480
            )
        }

        val infoCard = TextView(context).apply {
            text = "Select a region on the head illustration to see possible causes and advice"
            textSize = 14f
            setTextColor(Color.rgb(100, 116, 139))
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(248, 250, 252))
        }

        val note = TextView(context).apply {
            text = "Note: This is not a medical diagnosis. Consult a healthcare professional for persistent or severe symptoms."
            textSize = 12f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(18, 18, 18, 18)
            background = roundedBg(
                Color.rgb(255, 250, 220),
                Color.rgb(245, 185, 65),
                2,
                14f
            )
        }

        headView.onRegionSelected = { region ->
            val info = when (region) {
                "forehead" -> "ⓘ Headache + pressure → Possible tension or sinus issues\n\nAdvice: Try relaxation exercises, ensure good air quality, and stay hydrated."
                "top" -> "ⓘ Top head pain → Possible stress, dehydration, or lack of sleep\n\nAdvice: Drink water, rest, and reduce screen time."
                "left", "right" -> "ⓘ Side headache → Possible migraine, eye strain, or stress\n\nAdvice: Rest in a quiet room and monitor triggers."
                else -> "ⓘ Lower head pain → Possible neck tension or posture strain\n\nAdvice: Stretch gently, rest your neck, and improve posture."
            }

            infoCard.text = info
            infoCard.setTextColor(Color.rgb(30, 64, 175))
            infoCard.setBackgroundColor(Color.rgb(239, 246, 255))
        }

        container.addView(instruction)
        container.addView(headView)
        container.addView(infoCard)
        container.addView(makeSpacer(14))
        container.addView(note)

        val scroll = ScrollView(context)
        scroll.addView(container)

        showRoundedDialog("Headache Assessment", scroll)
    }

    private fun showFeverDialog() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = roundedBg(
                Color.rgb(255, 255, 255),
                Color.rgb(227, 238, 238),
                1,
                28f
            )
        }

        val headerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(255, 241, 241),
                Color.rgb(245, 194, 194),
                2,
                24f
            )
        }

        val headerTitle = TextView(context).apply {
            text = "🌡  Fever Assessment"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
        }

        val headerSubtitle = TextView(context).apply {
            text = "Choose your temperature range so we can give safer wellness guidance."
            textSize = 13f
            setTextColor(Color.rgb(93, 122, 126))
            setPadding(0, 8, 0, 0)
        }

        headerCard.addView(headerTitle)
        headerCard.addView(headerSubtitle)

        val instruction = TextView(context).apply {
            text = "Temperature range"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            setPadding(0, 20, 0, 12)
        }

        val resultCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(26, 22, 26, 22)
            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(72, 201, 176),
                2,
                22f
            )
        }

        val resultTitle = TextView(context).apply {
            textSize = 15f
            setTextColor(Color.rgb(26, 107, 114))
            setTypeface(null, Typeface.BOLD)
        }

        val resultDesc = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.rgb(55, 65, 81))
            setPadding(0, 8, 0, 0)
        }

        resultCard.addView(resultTitle)
        resultCard.addView(resultDesc)

        lateinit var lowCard: TextView
        lateinit var moderateCard: TextView
        lateinit var highCard: TextView

        fun styleCard(card: TextView, selected: Boolean) {
            card.setPadding(26, 20, 26, 20)
            card.textSize = 15f
            card.setTextColor(Color.rgb(28, 43, 45))
            card.background = if (selected) {
                roundedBg(
                    Color.rgb(238, 248, 247),
                    Color.rgb(26, 107, 114),
                    3,
                    22f
                )
            } else {
                roundedBg(
                    Color.WHITE,
                    Color.rgb(221, 237, 234),
                    2,
                    22f
                )
            }
        }

        fun select(level: String) {
            styleCard(lowCard, level == "low")
            styleCard(moderateCard, level == "moderate")
            styleCard(highCard, level == "high")

            resultCard.visibility = View.VISIBLE

            when (level) {
                "low" -> {
                    resultTitle.text = "🟡 Low-grade fever"
                    resultDesc.text = "Mild temperature increase.\n\nRecommendation: Rest, drink water, monitor condition."
                }
                "moderate" -> {
                    resultTitle.text = "🟠 Moderate fever"
                    resultDesc.text = "Possible infection or illness.\n\nRecommendation: Hydrate, rest, monitor symptoms."
                }
                else -> {
                    resultTitle.text = "🔴 High fever"
                    resultDesc.text = "Needs attention.\n\nRecommendation: Seek medical advice immediately."
                }
            }
        }

        lowCard = TextView(context).apply {
            text = "🌡  Low-grade fever\n37.0°C - 37.5°C"
            setOnClickListener { select("low") }
        }

        moderateCard = TextView(context).apply {
            text = "🌡  Moderate fever\n37.6°C - 38.5°C"
            setOnClickListener { select("moderate") }
        }

        highCard = TextView(context).apply {
            text = "🌡  High fever\n38.6°C+"
            setOnClickListener { select("high") }
        }

        styleCard(lowCard, false)
        styleCard(moderateCard, false)
        styleCard(highCard, false)

        val symptoms = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(247, 250, 250),
                Color.rgb(230, 238, 238),
                1,
                22f
            )

            addView(TextView(context).apply {
                text = "⚠  Common Symptoms"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(28, 43, 45))
            })

            addView(TextView(context).apply {
                text = "• Chills or sweating\n• Body aches\n• Weakness or fatigue\n• Headache\n• Loss of appetite\n• Dehydration"
                textSize = 14f
                setTextColor(Color.rgb(93, 122, 126))
                setPadding(0, 10, 0, 0)
                setLineSpacing(4f, 1f)
            })
        }

        val note = TextView(context).apply {
            text = "Note: This is not a medical diagnosis. Seek medical attention if fever persists or worsens."
            textSize = 12f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(18, 18, 18, 18)
            background = roundedBg(
                Color.rgb(255, 250, 220),
                Color.rgb(245, 185, 65),
                2,
                16f
            )
        }

        container.addView(headerCard)
        container.addView(instruction)
        container.addView(lowCard)
        container.addView(makeSpacer(12))
        container.addView(moderateCard)
        container.addView(makeSpacer(12))
        container.addView(highCard)
        container.addView(makeSpacer(16))
        container.addView(resultCard)
        container.addView(makeSpacer(16))
        container.addView(symptoms)
        container.addView(makeSpacer(16))
        container.addView(note)

        val scroll = ScrollView(context)
        scroll.addView(container)

        showRoundedDialog("Fever Assessment", scroll)
    }

    private fun showCoughDialog() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = roundedBg(
                Color.rgb(255, 255, 255),
                Color.rgb(227, 238, 238),
                1,
                28f
            )
        }

        val headerCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(205, 232, 228),
                2,
                24f
            )
        }

        val headerTitle = TextView(context).apply {
            text = "💨  Cough Assessment"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
        }

        val headerSubtitle = TextView(context).apply {
            text = "Select the cough type that best matches what you feel."
            textSize = 13f
            setTextColor(Color.rgb(93, 122, 126))
            setPadding(0, 8, 0, 0)
        }

        headerCard.addView(headerTitle)
        headerCard.addView(headerSubtitle)

        val instruction = TextView(context).apply {
            text = "Cough type"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            setPadding(0, 20, 0, 12)
        }

        val resultCard = TextView(context).apply {
            visibility = View.GONE
            textSize = 14f
            setTextColor(Color.rgb(26, 107, 114))
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(72, 201, 176),
                2,
                22f
            )
        }

        lateinit var dryCard: TextView
        lateinit var wetCard: TextView
        lateinit var persistentCard: TextView

        fun styleCoughCard(card: TextView, selected: Boolean) {
            card.setPadding(26, 20, 26, 20)
            card.textSize = 15f
            card.setTextColor(Color.rgb(28, 43, 45))
            card.background = if (selected) {
                roundedBg(
                    Color.rgb(238, 248, 247),
                    Color.rgb(26, 107, 114),
                    3,
                    22f
                )
            } else {
                roundedBg(
                    Color.WHITE,
                    Color.rgb(221, 237, 234),
                    2,
                    22f
                )
            }
        }

        fun select(type: String) {
            styleCoughCard(dryCard, type == "dry")
            styleCoughCard(wetCard, type == "wet")
            styleCoughCard(persistentCard, type == "persistent")

            resultCard.visibility = View.VISIBLE
            resultCard.text = when (type) {
                "dry" -> "Dry cough with throat irritation may be related to allergies, dust, or dry air.\n\nRecommendation: Drink warm water, avoid irritants, and rest your throat."
                "wet" -> "Wet cough with mucus may be related to congestion or respiratory irritation.\n\nRecommendation: Stay hydrated, rest, and monitor if fever or chest discomfort appears."
                else -> "Persistent cough needs closer monitoring, especially if it lasts for weeks.\n\nRecommendation: Consult a healthcare professional if it continues or worsens."
            }
        }

        dryCard = TextView(context).apply {
            text = "💨  Dry Cough\nNo mucus or phlegm"
            setOnClickListener { select("dry") }
        }

        wetCard = TextView(context).apply {
            text = "💧  Wet Cough\nProduces mucus or phlegm"
            setOnClickListener { select("wet") }
        }

        persistentCard = TextView(context).apply {
            text = "⏱  Persistent Cough\nLasting more than 3 weeks"
            setOnClickListener { select("persistent") }
        }

        styleCoughCard(dryCard, false)
        styleCoughCard(wetCard, false)
        styleCoughCard(persistentCard, false)

        val triggers = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(255, 247, 237),
                Color.rgb(251, 146, 60),
                2,
                22f
            )

            addView(TextView(context).apply {
                text = "⚠  Common Triggers"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(28, 43, 45))
            })

            addView(TextView(context).apply {
                text = "• Smoke or pollution\n• Dust or allergens\n• Cold air\n• Strong odors\n• Post-nasal drip"
                textSize = 14f
                setTextColor(Color.rgb(93, 122, 126))
                setPadding(0, 10, 0, 0)
                setLineSpacing(4f, 1f)
            })
        }

        val remedies = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 22)
            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(205, 232, 228),
                2,
                22f
            )

            addView(TextView(context).apply {
                text = "✨  Helpful Tips"
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(28, 43, 45))
            })

            addView(TextView(context).apply {
                text = "• Drink warm liquids\n• Use a humidifier\n• Avoid smoke and dust\n• Stay hydrated\n• Rest your voice"
                textSize = 14f
                setTextColor(Color.rgb(93, 122, 126))
                setPadding(0, 10, 0, 0)
                setLineSpacing(4f, 1f)
            })
        }

        val note = TextView(context).apply {
            text = "Note: This is not a medical diagnosis. Consult a healthcare professional if symptoms persist or worsen."
            textSize = 12f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(18, 18, 18, 18)
            background = roundedBg(
                Color.rgb(255, 250, 220),
                Color.rgb(245, 185, 65),
                2,
                16f
            )
        }

        container.addView(headerCard)
        container.addView(instruction)
        container.addView(dryCard)
        container.addView(makeSpacer(12))
        container.addView(wetCard)
        container.addView(makeSpacer(12))
        container.addView(persistentCard)
        container.addView(makeSpacer(16))
        container.addView(resultCard)
        container.addView(makeSpacer(16))
        container.addView(triggers)
        container.addView(makeSpacer(16))
        container.addView(remedies)
        container.addView(makeSpacer(16))
        container.addView(note)

        val scroll = ScrollView(context)
        scroll.addView(container)

        showRoundedDialog("Cough Assessment", scroll)
    }

    private fun showNotificationsPanel() {
        val context = requireContext()
        val dialog = Dialog(context)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notifications)

        val notificationList =
            dialog.findViewById<LinearLayout>(R.id.notificationListContainer)

        val subtitle =
            dialog.findViewById<TextView>(R.id.txtNotificationSubtitle)

        val btnClose =
            dialog.findViewById<TextView>(R.id.btnCloseNotifications)

        val btnClear =
            dialog.findViewById<TextView>(R.id.btnClearNotifications)

        val notifications = AppNotificationStore.getAll(context)

        notificationList.removeAllViews()

        subtitle.text = if (notifications.isEmpty()) {
            "No notifications yet."
        } else {
            "${notifications.size} recent notification(s)"
        }

        if (notifications.isEmpty()) {
            notificationList.addView(
                notificationEmptyCard(
                    "Your water reminders, sleep reminders, eye rest reminders, inactivity alerts, and wellness warnings will appear here."
                )
            )
        } else {
            notifications.forEach { item ->
                notificationList.addView(notificationCard(item))
            }
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        btnClear.setOnClickListener {
            AppNotificationStore.clear(context)
            Toast.makeText(context, "Notifications cleared.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawable(
                roundedBg(
                    Color.WHITE,
                    null,
                    0,
                    36f
                )
            )

            setLayout(
                (resources.displayMetrics.widthPixels * 0.82).toInt(),
                WindowManager.LayoutParams.MATCH_PARENT
            )

            setGravity(Gravity.END)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            attributes = attributes.apply {
                dimAmount = 0.45f
            }
        }

        AppNotificationStore.markAllRead(context)
    }
    private fun notificationCard(item: AppNotificationItem): LinearLayout {
        val context = requireContext()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)

            background = roundedBg(
                notificationBgColor(item.type),
                notificationBorderColor(item.type),
                2,
                22f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        val title = TextView(context).apply {
            text = "${notificationIcon(item.type)}  ${item.title}"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
        }

        val message = TextView(context).apply {
            text = item.message
            textSize = 13f
            setTextColor(Color.rgb(93, 122, 126))
            setLineSpacing(4f, 1f)
            setPadding(0, 6, 0, 0)
        }

        val time = TextView(context).apply {
            text = formatSmartTime(item.createdAt)
            textSize = 12f
            setTextColor(Color.rgb(120, 140, 140))
            setPadding(0, 8, 0, 0)
        }

        card.addView(title)
        card.addView(message)
        card.addView(time)

        return card
    }

    private fun notificationEmptyCard(messageValue: String): TextView {
        return TextView(requireContext()).apply {
            text = messageValue
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(93, 122, 126))
            setPadding(20, 30, 20, 30)

            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(221, 237, 234),
                2,
                22f
            )
        }
    }

    private fun notificationIcon(type: String): String {
        return when (type) {
            "water" -> "💧"
            "sleep" -> "🌙"
            "eyes" -> "👁"
            "activity" -> "👣"
            "warning" -> "⚠"
            "status" -> "📋"
            else -> "🔔"
        }
    }

    private fun notificationBgColor(type: String): Int {
        return when (type) {
            "water" -> Color.rgb(238, 248, 247)
            "sleep" -> Color.rgb(238, 242, 255)
            "eyes" -> Color.rgb(255, 247, 232)
            "activity" -> Color.rgb(236, 253, 245)
            "warning" -> Color.rgb(254, 242, 242)
            else -> Color.rgb(247, 250, 250)
        }
    }

    private fun notificationBorderColor(type: String): Int {
        return when (type) {
            "water" -> Color.rgb(205, 232, 228)
            "sleep" -> Color.rgb(199, 210, 254)
            "eyes" -> Color.rgb(243, 217, 164)
            "activity" -> Color.rgb(167, 243, 208)
            "warning" -> Color.rgb(252, 165, 165)
            else -> Color.rgb(221, 237, 234)
        }
    }

    private fun showRoundedDialog(
        title: String,
        content: View,
        bgColor: Int = Color.WHITE
    ) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(content)
            .setNegativeButton("Close", null)
            .show()

        dialog.window?.setBackgroundDrawable(
            roundedBg(
                bgColor,
                null,
                0,
                34f
            )
        )
    }

    private fun showQuickActionDialog(
        title: String,
        subtitle: String,
        emoji: String,
        bgColor: Int,
        borderColor: Int
    ) {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 28, 30, 28)
            setBackgroundColor(Color.TRANSPARENT)
        }

        val icon = TextView(context).apply {
            text = emoji
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
        }

        val subtitleView = TextView(context).apply {
            text = subtitle
            textSize = 14f
            setTextColor(Color.rgb(55, 65, 81))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
            setLineSpacing(4f, 1f)
        }

        container.addView(icon)
        container.addView(titleView)
        container.addView(subtitleView)

        showRoundedDialog(title, container, bgColor)
    }

    private fun makeSpacer(height: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }

    private fun roundedBg(
        bgColor: Int,
        strokeColor: Int? = null,
        strokeWidth: Int = 2,
        radius: Float = 22f
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)

            if (strokeColor != null) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }
    private fun addRecentActivity(
        title: String,
        message: String,
        type: String = "general"
    ) {
        RecentActivityStore.add(
            requireContext(),
            title,
            message,
            type
        )

        loadRecentActivities()
    }

    private fun loadRecentActivities() {
        if (!::recentActivitiesContainer.isInitialized) return

        val activities = RecentActivityStore.getAll(requireContext()).take(4)

        recentActivitiesContainer.removeAllViews()

        recentActivitiesSubtitle.text = if (activities.isEmpty()) {
            "Your latest app actions will appear here."
        } else {
            "${activities.size} latest action(s)"
        }

        if (activities.isEmpty()) {
            recentActivitiesContainer.addView(
                recentActivityEmptyCard(
                    "No recent activities yet. Use Quick Actions or complete a wellness check."
                )
            )
            return
        }

        activities.forEach { item ->
            recentActivitiesContainer.addView(
                recentActivityCard(item)
            )
        }
    }

    private fun recentActivityCard(item: RecentActivityItem): LinearLayout {
        val context = requireContext()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)

            background = roundedBg(
                Color.rgb(247, 250, 250),
                Color.rgb(221, 237, 234),
                2,
                22f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }

        val title = TextView(context).apply {
            text = "${recentActivityIcon(item.type)}  ${item.title}"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
        }

        val message = TextView(context).apply {
            text = item.message
            textSize = 13f
            setTextColor(Color.rgb(93, 122, 126))
            setLineSpacing(4f, 1f)
            setPadding(0, 5, 0, 0)
        }

        val time = TextView(context).apply {
            text = formatSmartTime(item.createdAt)
            textSize = 12f
            setTextColor(Color.rgb(120, 140, 140))
            setPadding(0, 8, 0, 0)
        }

        card.addView(title)
        card.addView(message)
        card.addView(time)

        return card
    }

    private fun recentActivityEmptyCard(messageValue: String): TextView {
        return TextView(requireContext()).apply {
            text = messageValue
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(93, 122, 126))
            setPadding(20, 26, 20, 26)

            background = roundedBg(
                Color.rgb(238, 248, 247),
                Color.rgb(221, 237, 234),
                2,
                22f
            )
        }
    }

    private fun recentActivityIcon(type: String): String {
        return when (type) {
            "water" -> "💧"
            "sleep" -> "🌙"
            "eyes" -> "👁"
            "symptom" -> "📝"
            "fever" -> "🌡"
            "cough" -> "💨"
            "headache" -> "🤕"
            "assessment" -> "📋"
            else -> "✨"
        }
    }
    private fun checkStepPermission() {
        if (stepSensor == null) {
            stepsText.text = "No sensor"
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startStepCounter()
            } else {
                stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            startStepCounter()
        }
    }

    private fun startStepCounter() {
        stepSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onResume() {
        super.onResume()

        resetSleepTimerIfNewDay()
        restoreSleepTimer()

        todayWaterCups = getTodayWaterCups()
        updateTodayWellnessCard()

        if (::sensorManager.isInitialized && stepSensor != null) {
            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startStepCounter()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        sleepTimer?.cancel()

        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        wellnessListener?.remove()
        wellnessListener = null
        sleepTimer?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsSinceReboot = event.values[0]
        val prefs = requireContext().getSharedPreferences("step_prefs", Context.MODE_PRIVATE)

        val today = todayKey()
        val savedDate = prefs.getString("date", null)
        var baseSteps = prefs.getFloat("base_steps", -1f)

        if (savedDate != today || baseSteps < 0f || totalStepsSinceReboot < baseSteps) {
            baseSteps = totalStepsSinceReboot

            prefs.edit()
                .putString("date", today)
                .putFloat("base_steps", baseSteps)
                .apply()
        }

        val stepsToday = (totalStepsSinceReboot - baseSteps).toInt().coerceAtLeast(0)
        stepsText.text = stepsToday.toString()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
private fun loadHomeGreeting(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    greetingText: TextView
) {
    val user = auth.currentUser

    if (user == null) {
        greetingText.text = "Hello, User"
        return
    }

    db.collection("users")
        .document(user.uid)
        .get()
        .addOnSuccessListener { document ->
            val name = cleanFullName(
                document.getString("name"),
                user.email
            ) ?: cleanFullName(
                document.getString("fullName"),
                user.email
            ) ?: cleanFullName(
                user.displayName,
                user.email
            ) ?: "User"

            greetingText.text = "Hello, $name"
        }
        .addOnFailureListener {
            val name = cleanFullName(user.displayName, user.email) ?: "User"
            greetingText.text = "Hello, $name"
        }
}

private fun cleanFullName(
    value: String?,
    email: String?
): String? {
    val text = value?.trim().orEmpty()

    if (text.isBlank()) return null
    if (text.contains("@")) return null

    val emailUsername = email?.substringBefore("@")?.trim()

    if (!emailUsername.isNullOrBlank() && text.equals(emailUsername, ignoreCase = true)) {
        return null
    }

    return text
}