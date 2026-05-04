package com.example.wellnessguide.ui.activity

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.notifications.ReminderNotificationStore
import com.example.wellnessguide.notifications.ReminderReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class PhysicalActivityFragment : Fragment(), SensorEventListener {

    private val primaryColor = Color.rgb(26, 107, 114)
    private val backgroundColor = Color.rgb(238, 248, 247)
    private val surfaceColor = Color.WHITE
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)
    private val borderColor = Color.rgb(221, 237, 234)

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private lateinit var stepsText: TextView
    private lateinit var goalText: TextView
    private lateinit var progressText: TextView
    private lateinit var statusText: TextView
    private lateinit var distanceText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var weeklySummaryText: TextView
    private lateinit var chartView: WeeklyStepsChartView

    private var stepsToday = 0
    private var stepGoal = 8000

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val activityPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startStepCounter()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Activity recognition permission is needed for step counter.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                turnOnInactiveReminder()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notification permission is needed for inactive reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(42))
        }

        stepGoal = getGoal()

        sensorManager =
            requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        addTopBar(root)

        root.addView(bigTitle("Physical Activity"))

        root.addView(
            paragraph(
                "Track your daily movement, weekly activity summary, and walking route."
            )
        )

        root.addView(buildStepsCard())

        val goalInput = input("Set step goal. Example: 8000")
        root.addView(goalInput)

        val setGoalButton = secondaryButton("Set Goal")
        setGoalButton.setOnClickListener {
            val newGoal = goalInput.text.toString().toIntOrNull()

            if (newGoal == null || newGoal <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Enter a valid step goal.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            setGoal(newGoal)
            stepGoal = newGoal
            updateStepUi()

            Toast.makeText(
                requireContext(),
                "Step goal updated.",
                Toast.LENGTH_SHORT
            ).show()
        }
        root.addView(setGoalButton)

        val saveButton = actionButton("Save Daily Activity Summary")
        saveButton.setOnClickListener {
            saveDailyStepLog()
        }
        root.addView(saveButton)

        val inactiveButton = secondaryButton("Turn On Inactive Reminder")
        inactiveButton.setOnClickListener {
            requestNotificationPermissionThenTurnOnInactive()
        }
        root.addView(inactiveButton)

        val routeButton = actionButton("Start Walk Route Tracker")
        routeButton.setOnClickListener {
            findNavController().navigate(R.id.routeTrackerFragment)
        }
        root.addView(routeButton)

        root.addView(sectionTitle("This Week"))

        weeklySummaryText = resultCard("Loading weekly summary...")
        root.addView(weeklySummaryText)

        chartView = WeeklyStepsChartView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(230)
            ).apply {
                topMargin = dp(12)
            }

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                24f
            )
        }
        root.addView(chartView)

        scroll.addView(root)

        loadWeeklySummary()
        checkStepPermission()
        updateStepUi()

        return scroll
    }

    private fun buildStepsCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                28f
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        }

        val label = TextView(requireContext()).apply {
            text = "Today's Movement"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        stepsText = bigMetric("Steps Today: 0")
        goalText = smallMetric("Goal: $stepGoal steps")
        progressText = smallMetric("Progress: 0%")
        statusText = smallMetric("Status: Low Activity")
        distanceText = smallMetric("Distance: 0.00 km")
        caloriesText = smallMetric("Calories: 0 kcal estimate")

        card.addView(label)
        card.addView(stepsText)
        card.addView(goalText)
        card.addView(progressText)
        card.addView(statusText)
        card.addView(distanceText)
        card.addView(caloriesText)

        return card
    }

    private fun updateStepUi() {
        if (!::stepsText.isInitialized) return

        val progress = if (stepGoal > 0) {
            ((stepsToday.toDouble() / stepGoal.toDouble()) * 100).roundToInt()
        } else {
            0
        }

        val distance = stepsToday * 0.0008
        val calories = (stepsToday * 0.04).roundToInt()
        val status = activityStatus(stepsToday)

        stepsText.text = "Steps Today: $stepsToday"
        goalText.text = "Goal: $stepGoal steps"
        progressText.text = "Progress: $progress%"
        statusText.text = "Status: $status"
        distanceText.text = "Distance: ${"%.2f".format(distance)} km"
        caloriesText.text = "Calories: $calories kcal estimate"

        maybeSendGoalReachedNotification(progress)
    }

    private fun activityStatus(steps: Int): String {
        return when {
            steps < 2000 -> "Low Activity"
            steps < 5000 -> "Light Activity"
            steps < 8000 -> "Moderate Activity"
            else -> "Active / Goal Reached"
        }
    }

    private fun saveDailyStepLog() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please login to save activity.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progress = if (stepGoal > 0) {
            ((stepsToday.toDouble() / stepGoal.toDouble()) * 100).roundToInt()
        } else {
            0
        }

        val distance = stepsToday * 0.0008
        val calories = (stepsToday * 0.04).roundToInt()

        val status = if (stepsToday >= stepGoal) {
            "Green - Goal reached"
        } else if (stepsToday < 2000) {
            "Yellow - Low activity"
        } else {
            "Green - Active"
        }

        val summary = """
            Steps Today: $stepsToday
            Goal: $stepGoal steps
            Progress: $progress%
            Status: ${activityStatus(stepsToday)}
            Distance: ${"%.2f".format(distance)} km
            Calories: $calories kcal estimate
        """.trimIndent()

        val recommendations = when {
            stepsToday < 2000 -> "You have been inactive. Take a short walk or stretch if you feel well."
            stepsToday < stepGoal -> "Good progress. Try a short walk to move closer to your goal."
            else -> "Great job reaching your step goal. Hydrate and rest if needed."
        }

        val data = hashMapOf<String, Any>(
            "userId" to user.uid,
            "logType" to "physical_activity",
            "title" to "Physical Activity",
            "status" to status,
            "summary" to summary,
            "recommendations" to recommendations,
            "createdAt" to System.currentTimeMillis(),
            "dayKey" to todayKey(),
            "details" to mapOf(
                "steps" to stepsToday,
                "goal" to stepGoal,
                "progress" to progress,
                "distanceKm" to distance,
                "calories" to calories,
                "activityStatus" to activityStatus(stepsToday)
            )
        )

        db.collection("wellness_logs")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Activity summary saved.",
                    Toast.LENGTH_SHORT
                ).show()

                loadWeeklySummary()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to save activity.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadWeeklySummary() {
        val user = auth.currentUser

        if (user == null) {
            if (::weeklySummaryText.isInitialized) {
                weeklySummaryText.text = "Please login to view weekly activity."
            }

            if (::chartView.isInitialized) {
                chartView.setData(emptyList())
            }

            return
        }

        db.collection("wellness_logs")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { result ->
                val days = lastSevenDayKeys()
                val stepsByDay = days.associateWith { 0 }.toMutableMap()

                result.documents.forEach { doc ->
                    val logType = doc.getString("logType") ?: ""

                    if (logType != "physical_activity") {
                        return@forEach
                    }

                    val dayKey = doc.getString("dayKey") ?: dateKeyFromTimestamp(
                        doc.getLong("createdAt") ?: 0L
                    )

                    if (!stepsByDay.containsKey(dayKey)) {
                        return@forEach
                    }

                    val details = doc.get("details") as? Map<*, *>
                    val steps = (details?.get("steps") as? Number)?.toInt() ?: 0

                    if (steps > (stepsByDay[dayKey] ?: 0)) {
                        stepsByDay[dayKey] = steps
                    }
                }

                val values = days.map { stepsByDay[it] ?: 0 }
                val total = values.sum()
                val average = if (values.isNotEmpty()) total / values.size else 0
                val best = values.maxOrNull() ?: 0
                val goalDays = values.count { it >= stepGoal }

                weeklySummaryText.text = """
                    Weekly Total: $total steps
                    Daily Average: $average steps
                    Best Day: $best steps
                    Goal Reached: $goalDays out of 7 days
                """.trimIndent()

                chartView.setData(values)
            }
            .addOnFailureListener {
                weeklySummaryText.text = "Unable to load weekly summary."
                chartView.setData(emptyList())
            }
    }

    private fun requestNotificationPermissionThenTurnOnInactive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                turnOnInactiveReminder()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            turnOnInactiveReminder()
        }
    }

    private fun turnOnInactiveReminder() {
        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("title", "Inactive Reminder")
            putExtra("message", "You have been inactive. Take a short walk or stretch.")
            putExtra("notificationId", 4201)
            putExtra("ongoing", false)
            putExtra("oneTime", false)
            putExtra("statusKey", "inactive_activity_enabled")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            4201,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 2 * 60 * 60 * 1000L,
            2 * 60 * 60 * 1000L,
            pendingIntent
        )

        Toast.makeText(
            requireContext(),
            "Inactive reminder turned on. It will notify every 2 hours.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun maybeSendGoalReachedNotification(progress: Int) {
        if (progress < 100) return

        val prefs = requireContext().getSharedPreferences(
            "activity_prefs",
            Context.MODE_PRIVATE
        )

        val lastSent = prefs.getString("goal_notification_date", "")

        if (lastSent == todayKey()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        createNotificationChannel()

        val notification = NotificationCompat.Builder(
            requireContext(),
            "activity_channel"
        )
            .setSmallIcon(R.drawable.ic_activity)
            .setContentTitle("Step Goal Reached")
            .setContentText("Great job! You reached your daily step goal.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.notify(4301, notification)

        ReminderNotificationStore.add(
            context = requireContext(),
            title = "Step Goal Reached",
            message = "Great job! You reached your daily step goal.",
            type = "activity"
        )

        prefs.edit()
            .putString("goal_notification_date", todayKey())
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                "activity_channel",
                "Activity Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(channel)
        }
    }

    private fun checkStepPermission() {
        if (stepSensor == null) {
            stepsText.text = "Steps Today: No step sensor"
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
                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
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

        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return

        val totalStepsSinceReboot = event.values[0]
        val prefs = requireContext().getSharedPreferences(
            "step_prefs",
            Context.MODE_PRIVATE
        )

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

        stepsToday = (totalStepsSinceReboot - baseSteps).toInt().coerceAtLeast(0)
        updateStepUi()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun getGoal(): Int {
        return requireContext()
            .getSharedPreferences("activity_prefs", Context.MODE_PRIVATE)
            .getInt("step_goal", 8000)
    }

    private fun setGoal(goal: Int) {
        requireContext()
            .getSharedPreferences("activity_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("step_goal", goal)
            .apply()
    }

    private fun todayKey(): String {
        return SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date())
    }

    private fun dateKeyFromTimestamp(timestamp: Long): String {
        return SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    private fun lastSevenDayKeys(): List<String> {
        val result = mutableListOf<String>()
        val calendar = java.util.Calendar.getInstance()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)

            result.add(
                SimpleDateFormat(
                    "yyyyMMdd",
                    Locale.getDefault()
                ).format(calendar.time)
            )
        }

        return result
    }

    private fun addTopBar(root: LinearLayout) {
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
            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                40f
            )

            layoutParams = LinearLayout.LayoutParams(
                dp(44),
                dp(44)
            )

            setOnClickListener {
                (requireActivity() as MainActivity).openDrawer()
            }
        }

        val title = TextView(requireContext()).apply {
            text = "Physical Activity"
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
        header.addView(title)
        root.addView(header)
    }

    private fun bigMetric(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, dp(14), 0, 0)
        }
    }

    private fun smallMetric(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(textSecondary)
            setPadding(0, dp(6), 0, 0)
        }
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

    private fun resultCard(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(textSecondary)
            setLineSpacing(5f, 1f)
            setPadding(dp(18), dp(16), dp(18), dp(16))

            background = roundedBg(
                surfaceColor,
                borderColor,
                2,
                24f
            )
        }
    }

    private fun input(hintValue: String): EditText {
        return EditText(requireContext()).apply {
            hint = hintValue
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
                topMargin = dp(14)
            }
        }
    }

    private fun actionButton(textValue: String): Button {
        return Button(requireContext()).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)

            background = roundedBg(
                primaryColor,
                primaryColor,
                2,
                22f
            )

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

            background = roundedBg(
                surfaceColor,
                borderColor,
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

    class WeeklyStepsChartView(context: Context) : View(context) {
        private var values: List<Int> = emptyList()

        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 107, 114)
            style = Paint.Style.FILL
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(93, 122, 126)
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 43, 45)
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }

        fun setData(newValues: List<Int>) {
            values = newValues
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawText(
                "7-Day Steps Chart",
                40f,
                55f,
                titlePaint
            )

            if (values.isEmpty()) {
                canvas.drawText(
                    "No data yet",
                    width / 2f,
                    height / 2f,
                    textPaint
                )
                return
            }

            val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
            val chartTop = 80f
            val chartBottom = height - 45f
            val chartHeight = chartBottom - chartTop
            val barWidth = width / 10f
            val gap = barWidth / 2f

            values.forEachIndexed { index, value ->
                val left = gap + index * (barWidth + gap)
                val right = left + barWidth
                val barHeight = (value.toFloat() / max.toFloat()) * chartHeight
                val top = chartBottom - barHeight

                canvas.drawRoundRect(
                    left,
                    top,
                    right,
                    chartBottom,
                    12f,
                    12f,
                    barPaint
                )

                canvas.drawText(
                    value.toString(),
                    left + barWidth / 2f,
                    top - 8f,
                    textPaint
                )
            }
        }
    }
}