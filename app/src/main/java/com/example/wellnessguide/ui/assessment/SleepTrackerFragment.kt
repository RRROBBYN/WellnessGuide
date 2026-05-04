package com.example.wellnessguide.ui.assessment

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R
import com.example.wellnessguide.data.repository.WellnessLogRepository
import com.example.wellnessguide.notifications.SleepReminderReceiver
import com.example.wellnessguide.ui.common.WellnessUi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SleepTrackerFragment : Fragment() {

    private var sleepQuality = ""
    private var screenBeforeSleep = ""
    private var caffeine = ""
    private var wokeUp = ""
    private var consistency = ""
    private var pendingReminderTime: String? = null

    private lateinit var bedtimeInput: EditText
    private lateinit var wakeTimeInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var durationText: TextView
    private lateinit var insight: TextView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingReminderTime?.let {
                    scheduleSleepReminder(it)
                }
                pendingReminderTime = null
            } else {
                Toast.makeText(
                    requireContext(),
                    "Notification permission is needed to set sleep reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()

        val pair = WellnessUi.screen(
            context,
            "Sleep Tracker",
            "Progress: Step 5 of 5"
        ) {
            (requireActivity() as MainActivity).openDrawer()
        }

        val scroll = pair.first
        val root = pair.second

        root.addView(
            WellnessUi.paragraph(
                context,
                "Enter time using numbers only. Example: 2230 means 10:30 PM, 0630 means 6:30 AM."
            )
        )

        bedtimeInput = WellnessUi.input(
            context,
            "Bedtime HHMM only, example: 2230"
        )

        wakeTimeInput = WellnessUi.input(
            context,
            "Wake time HHMM only, example: 0630"
        )

        setupNumberOnlyTimeInput(bedtimeInput)
        setupNumberOnlyTimeInput(wakeTimeInput)

        root.addView(bedtimeInput)
        root.addView(wakeTimeInput)

        durationText = WellnessUi.resultCard(
            context,
            "Sleep Duration: Not calculated yet"
        )
        root.addView(durationText)

        val liveWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                updateLiveSleepDuration()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }

        bedtimeInput.addTextChangedListener(liveWatcher)
        wakeTimeInput.addTextChangedListener(liveWatcher)

        addSingleQuestion(
            root,
            "How was your sleep quality?",
            listOf("Poor", "Fair", "Good", "Excellent")
        ) {
            sleepQuality = it
        }

        addSingleQuestion(
            root,
            "Did you use your phone or screen before sleeping?",
            listOf("Yes", "No")
        ) {
            screenBeforeSleep = it
        }

        addSingleQuestion(
            root,
            "Did you drink coffee, tea, or energy drinks before sleeping?",
            listOf("Yes", "No")
        ) {
            caffeine = it
        }

        addSingleQuestion(
            root,
            "Did you wake up during the night?",
            listOf("No", "Yes, once", "Yes, many times")
        ) {
            wokeUp = it
        }

        addSingleQuestion(
            root,
            "Was your sleep schedule similar to your usual bedtime?",
            listOf("Yes", "No")
        ) {
            consistency = it
        }

        notesInput = WellnessUi.input(
            context,
            "Add sleep notes: Example: I slept late because I used my phone.",
            120
        )
        notesInput.gravity = Gravity.TOP
        root.addView(notesInput)

        insight = WellnessUi.resultCard(
            context,
            "Sleep insight will appear here after saving."
        )
        root.addView(insight)

        val reminderButton = WellnessUi.secondaryButton(
            context,
            "Set Sleep Reminder"
        )

        reminderButton.setOnClickListener {
            val bedtime = bedtimeInput.text.toString()

            if (!isValidFourDigitTime(bedtime)) {
                Toast.makeText(
                    context,
                    "Enter a valid bedtime using HHMM. Example: 2230",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            requestNotificationPermissionThenSchedule(bedtime)
        }

        root.addView(reminderButton)

        val resultButton = WellnessUi.actionButton(
            context,
            "Save & View Result"
        )

        resultButton.setOnClickListener {
            if (!validateSleepTracker()) return@setOnClickListener

            saveToSession()

            durationText.text = "Sleep Duration: ${AssessmentSession.sleepDuration}"

            insight.text = """
                Sleep Insight

                You slept for ${AssessmentSession.sleepDuration} with ${AssessmentSession.sleepQuality} sleep quality.

                Low sleep, poor sleep quality, screen use before sleep, or caffeine before sleep may contribute to fatigue, headache, low energy, stress, or poor focus.

                Sleep Tips:
                - Avoid screens before sleep
                - Keep your room cool and dark
                - Try to sleep and wake up at the same time daily
                - Avoid caffeine late in the day
            """.trimIndent()

            WellnessLogRepository.saveSleepLog(
                onSuccess = {
                    WellnessLogRepository.saveFullAssessmentLog(
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Assessment saved.",
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().navigate(
                                R.id.resultFragment,
                                bundleOf("resultText" to AssessmentSession.finalSummary())
                            )
                        },
                        onFailure = { message ->
                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
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

        root.addView(resultButton)

        return scroll
    }

    private fun setupNumberOnlyTimeInput(input: EditText) {
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.filters = arrayOf(InputFilter.LengthFilter(4))
        input.gravity = Gravity.CENTER_VERTICAL
    }

    private fun updateLiveSleepDuration() {
        val bedtime = bedtimeInput.text.toString()
        val wakeTime = wakeTimeInput.text.toString()

        durationText.text = if (bedtime.length == 4 && wakeTime.length == 4) {
            val duration = calculateSleepDuration(bedtime, wakeTime)

            if (duration.contains("Enter time", true)) {
                "Sleep Duration: Invalid time"
            } else {
                "Sleep Duration: $duration"
            }
        } else {
            "Sleep Duration: Not calculated yet"
        }
    }

    private fun requestNotificationPermissionThenSchedule(bedtime: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                scheduleSleepReminder(bedtime)
            } else {
                pendingReminderTime = bedtime
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleSleepReminder(bedtime)
        }
    }

    private fun scheduleSleepReminder(bedtime: String) {
        val minutes = toMinutes(bedtime)

        if (minutes == null) {
            Toast.makeText(
                requireContext(),
                "Invalid bedtime. Use HHMM format like 2230.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val hour = minutes / 60
        val minute = minutes % 60

        val reminderTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -30)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(requireContext(), SleepReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            reminderTime.timeInMillis,
            pendingIntent
        )

        val formatted = SimpleDateFormat(
            "MMM dd, hh:mm a",
            Locale.getDefault()
        ).format(reminderTime.time)

        Toast.makeText(
            requireContext(),
            "Sleep reminder set for $formatted.",
            Toast.LENGTH_LONG
        ).show()
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

    private fun validateSleepTracker(): Boolean {
        val missing = mutableListOf<String>()

        if (bedtimeInput.text.toString().isBlank()) missing.add("Bedtime")
        if (wakeTimeInput.text.toString().isBlank()) missing.add("Wake time")
        if (sleepQuality.isBlank()) missing.add("Sleep quality")
        if (screenBeforeSleep.isBlank()) missing.add("Screen before sleep")
        if (caffeine.isBlank()) missing.add("Caffeine before sleep")
        if (wokeUp.isBlank()) missing.add("Woke up during sleep")
        if (consistency.isBlank()) missing.add("Sleep consistency")
        if (notesInput.text.toString().isBlank()) missing.add("Sleep notes")

        return if (missing.isNotEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please answer: ${missing.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
            false
        } else if (!isValidFourDigitTime(bedtimeInput.text.toString())) {
            Toast.makeText(
                requireContext(),
                "Please enter valid bedtime using HHMM. Example: 2230",
                Toast.LENGTH_LONG
            ).show()
            false
        } else if (!isValidFourDigitTime(wakeTimeInput.text.toString())) {
            Toast.makeText(
                requireContext(),
                "Please enter valid wake time using HHMM. Example: 0630",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            true
        }
    }

    private fun saveToSession() {
        AssessmentSession.bedtime = formatTimeForDisplay(bedtimeInput.text.toString())
        AssessmentSession.wakeTime = formatTimeForDisplay(wakeTimeInput.text.toString())

        AssessmentSession.sleepDuration = calculateSleepDuration(
            bedtimeInput.text.toString(),
            wakeTimeInput.text.toString()
        )

        AssessmentSession.sleepQuality = sleepQuality
        AssessmentSession.screenBeforeSleep = screenBeforeSleep
        AssessmentSession.caffeineBeforeSleep = caffeine
        AssessmentSession.wokeUpDuringSleep = wokeUp
        AssessmentSession.sleepConsistency = consistency
        AssessmentSession.sleepNotes = notesInput.text.toString()
    }

    private fun isValidFourDigitTime(value: String): Boolean {
        return toMinutes(value) != null
    }

    private fun calculateSleepDuration(
        bedtime: String,
        wakeTime: String
    ): String {
        val start = toMinutes(bedtime)
        val endRaw = toMinutes(wakeTime)

        if (start == null || endRaw == null) {
            return "Enter time as HHMM"
        }

        var end = endRaw

        if (end <= start) {
            end += 24 * 60
        }

        val diff = end - start
        val hours = diff / 60
        val minutes = diff % 60

        return "$hours hours $minutes minutes"
    }

    private fun toMinutes(value: String): Int? {
        val clean = value.trim()

        if (clean.length != 4) {
            return null
        }

        val hour = clean.substring(0, 2).toIntOrNull() ?: return null
        val minute = clean.substring(2, 4).toIntOrNull() ?: return null

        if (hour !in 0..23 || minute !in 0..59) {
            return null
        }

        return hour * 60 + minute
    }

    private fun formatTimeForDisplay(value: String): String {
        return if (value.length == 4) {
            value.substring(0, 2) + ":" + value.substring(2, 4)
        } else {
            value
        }
    }
}