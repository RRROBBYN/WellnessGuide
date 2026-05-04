package com.example.wellnessguide.ui.profile

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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFeatureFragment : Fragment() {

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

    data class SingleSelectState(
        var value: String = "",
        var refresh: () -> Unit = {}
    )

    data class MultiSelectState(
        val values: MutableSet<String> = mutableSetOf(),
        var refresh: () -> Unit = {}
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val pageType = arguments?.getString("pageType") ?: "my_profile"

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
        root.addView(paragraph(pageDescription(pageType)))

        when (pageType) {
            "my_profile" -> buildMyProfile(root)
            "health_background" -> buildHealthBackground(root)
            "lifestyle_profile" -> buildLifestyleProfile(root)
            "reminder_settings" -> buildReminderSettings(root)
            else -> buildMyProfile(root)
        }

        scroll.addView(root)
        return scroll
    }

    private fun buildMyProfile(root: LinearLayout) {
        val user = auth.currentUser

        if (user == null) {
            root.addView(emptyState("Please login to view your profile."))
            return
        }

        val card = cardContainer()
        root.addView(card)

        card.addView(sectionTitle("Personal Wellness Info"))

        val ageInput = numberInput("Age")
        val genderInput = input("Gender / Sex optional")
        val heightInput = numberInput("Height in cm")
        val weightInput = numberInput("Weight in kg")
        val bloodTypeInput = input("Blood type optional, example: O+")

        card.addView(ageInput)
        card.addView(genderInput)
        card.addView(heightInput)
        card.addView(weightInput)
        card.addView(bloodTypeInput)

        card.addView(sectionTitle("Emergency Contact"))

        val emergencyNameInput = input("Emergency contact name")
        val emergencyPhoneInput = input("Emergency contact phone")
        val emergencyRelationInput = input("Relationship, example: Parent, sibling, guardian")

        card.addView(emergencyNameInput)
        card.addView(emergencyPhoneInput)
        card.addView(emergencyRelationInput)

        card.addView(sectionTitle("Wellness Notes"))

        val wellnessNotesInput = multiLineInput(
            "Add notes that may help your wellness guide. Example: I often get headaches when I lack sleep."
        )

        card.addView(wellnessNotesInput)

        root.addView(
            infoCard(
                "Purpose",
                "This page stores personal wellness information only. Account details like name, email, avatar, password, and logout stay in Settings."
            )
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("my_profile")

        docRef.get()
            .addOnSuccessListener { document ->
                ageInput.setText(document.getString("age") ?: "")
                genderInput.setText(document.getString("gender") ?: "")
                heightInput.setText(document.getString("height") ?: "")
                weightInput.setText(document.getString("weight") ?: "")
                bloodTypeInput.setText(document.getString("bloodType") ?: "")

                emergencyNameInput.setText(document.getString("emergencyContactName") ?: "")
                emergencyPhoneInput.setText(document.getString("emergencyContactPhone") ?: "")
                emergencyRelationInput.setText(document.getString("emergencyContactRelation") ?: "")

                wellnessNotesInput.setText(document.getString("wellnessNotes") ?: "")
            }

        val saveButton = actionButton("Save My Profile")
        saveButton.setOnClickListener {
            val data = hashMapOf<String, Any>(
                "age" to ageInput.text.toString().trim(),
                "gender" to genderInput.text.toString().trim(),
                "height" to heightInput.text.toString().trim(),
                "weight" to weightInput.text.toString().trim(),
                "bloodType" to bloodTypeInput.text.toString().trim(),
                "emergencyContactName" to emergencyNameInput.text.toString().trim(),
                "emergencyContactPhone" to emergencyPhoneInput.text.toString().trim(),
                "emergencyContactRelation" to emergencyRelationInput.text.toString().trim(),
                "wellnessNotes" to wellnessNotesInput.text.toString().trim(),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "My Profile saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to save profile.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        root.addView(saveButton)
    }

    private fun buildHealthBackground(root: LinearLayout) {
        val user = auth.currentUser

        if (user == null) {
            root.addView(emptyState("Please login to save your health background."))
            return
        }

        val card = cardContainer()
        root.addView(card)

        val conditions = addMultiSelect(
            card,
            "Existing Conditions",
            listOf(
                "Asthma",
                "Diabetes",
                "Hypertension",
                "Migraine",
                "Heart Condition",
                "None"
            )
        )

        val allergies = addMultiSelect(
            card,
            "Allergies",
            listOf(
                "Food Allergy",
                "Dust Allergy",
                "Pollen Allergy",
                "Medicine Allergy",
                "None"
            )
        )

        card.addView(sectionTitle("Current Medications"))
        val medicationsInput = input("Example: Paracetamol, inhaler, vitamins")
        card.addView(medicationsInput)

        card.addView(sectionTitle("Medical Notes"))
        val medicalNotesInput = multiLineInput("Previous surgery, special notes, or important health background")
        card.addView(medicalNotesInput)

        val riskFactors = addMultiSelect(
            card,
            "Risk Background",
            listOf(
                "Smoker",
                "Pregnant",
                "Senior Citizen",
                "Immunocompromised",
                "None"
            )
        )

        root.addView(
            infoCard(
                "Purpose",
                "Health background helps the app understand possible risk factors during wellness checks. Example: asthma plus breathing difficulty may need more caution."
            )
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("health_background")

        docRef.get()
            .addOnSuccessListener { document ->
                conditions.values.clear()
                conditions.values.addAll(readStringList(document, "conditions"))
                conditions.refresh()

                allergies.values.clear()
                allergies.values.addAll(readStringList(document, "allergies"))
                allergies.refresh()

                medicationsInput.setText(document.getString("medications") ?: "")
                medicalNotesInput.setText(document.getString("medicalNotes") ?: "")

                riskFactors.values.clear()
                riskFactors.values.addAll(readStringList(document, "riskFactors"))
                riskFactors.refresh()
            }

        val saveButton = actionButton("Save Health Background")
        saveButton.setOnClickListener {
            val data = hashMapOf<String, Any>(
                "conditions" to conditions.values.toList(),
                "allergies" to allergies.values.toList(),
                "medications" to medicationsInput.text.toString().trim(),
                "medicalNotes" to medicalNotesInput.text.toString().trim(),
                "riskFactors" to riskFactors.values.toList(),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Health background saved.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to save health background.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        root.addView(saveButton)
    }

    private fun buildLifestyleProfile(root: LinearLayout) {
        val user = auth.currentUser

        if (user == null) {
            root.addView(emptyState("Please login to save your lifestyle profile."))
            return
        }

        val card = cardContainer()
        root.addView(card)

        val typicalSleep = addSingleSelect(
            card,
            "Typical Sleep Hours",
            listOf("Less than 4", "4-5", "6-7", "8+")
        )

        val waterIntake = addSingleSelect(
            card,
            "Typical Water Intake",
            listOf("Low", "Enough", "More than usual")
        )

        val activityLevel = addSingleSelect(
            card,
            "Usual Activity Level",
            listOf("Sedentary", "Light", "Moderate", "Active")
        )

        val screenTime = addSingleSelect(
            card,
            "Average Screen Time",
            listOf("Less than 2 hours", "2-4 hours", "5-7 hours", "8+ hours")
        )

        val stressBaseline = addSingleSelect(
            card,
            "Stress Baseline",
            listOf("Low", "Moderate", "High")
        )

        val commonTriggers = addMultiSelect(
            card,
            "Common Triggers",
            listOf(
                "Lack of Sleep",
                "Stress",
                "Screen Time",
                "Dust",
                "Weather Change",
                "Food",
                "None"
            )
        )

        root.addView(
            infoCard(
                "Purpose",
                "Lifestyle profile is your usual baseline. During assessment, it can help compare today’s habits with your normal routine."
            )
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("lifestyle_profile")

        docRef.get()
            .addOnSuccessListener { document ->
                typicalSleep.value = document.getString("typicalSleep") ?: ""
                typicalSleep.refresh()

                waterIntake.value = document.getString("waterIntake") ?: ""
                waterIntake.refresh()

                activityLevel.value = document.getString("activityLevel") ?: ""
                activityLevel.refresh()

                screenTime.value = document.getString("screenTime") ?: ""
                screenTime.refresh()

                stressBaseline.value = document.getString("stressBaseline") ?: ""
                stressBaseline.refresh()

                commonTriggers.values.clear()
                commonTriggers.values.addAll(readStringList(document, "commonTriggers"))
                commonTriggers.refresh()
            }

        val saveButton = actionButton("Save Lifestyle Profile")
        saveButton.setOnClickListener {
            val data = hashMapOf<String, Any>(
                "typicalSleep" to typicalSleep.value,
                "waterIntake" to waterIntake.value,
                "activityLevel" to activityLevel.value,
                "screenTime" to screenTime.value,
                "stressBaseline" to stressBaseline.value,
                "commonTriggers" to commonTriggers.values.toList(),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Lifestyle profile saved.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to save lifestyle profile.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        root.addView(saveButton)
    }

    private fun buildReminderSettings(root: LinearLayout) {
        val user = auth.currentUser

        if (user == null) {
            root.addView(emptyState("Please login to save reminder settings."))
            return
        }

        val card = cardContainer()
        root.addView(card)

        card.addView(sectionTitle("Reminder Preferences"))

        val waterEnabled = checkBox("Water Reminder")
        val waterInterval = numberInput("Water interval in hours, example: 2")

        val sleepEnabled = checkBox("Sleep Reminder")
        val sleepTime = input("Sleep reminder time, example: 21:30")

        val eyeRestEnabled = checkBox("Eye Rest Reminder")
        val eyeRestInterval = numberInput("Eye rest interval in minutes, example: 20")

        val dailyCheckinEnabled = checkBox("Daily Check-in Reminder")
        val dailyCheckinTime = input("Daily check-in time, example: 20:00")

        val activityEnabled = checkBox("Inactivity Reminder")
        val inactiveHours = numberInput("Notify if inactive for hours, example: 2")

        card.addView(waterEnabled)
        card.addView(waterInterval)

        card.addView(sleepEnabled)
        card.addView(sleepTime)

        card.addView(eyeRestEnabled)
        card.addView(eyeRestInterval)

        card.addView(dailyCheckinEnabled)
        card.addView(dailyCheckinTime)

        card.addView(activityEnabled)
        card.addView(inactiveHours)

        root.addView(
            infoCard(
                "Reminder Settings vs Reminders Page",
                "Reminder Settings stores your preferences. The Reminders page shows active reminders and notifications inside the app."
            )
        )

        val docRef = db.collection("users")
            .document(user.uid)
            .collection("profile")
            .document("reminder_settings")

        docRef.get()
            .addOnSuccessListener { document ->
                waterEnabled.isChecked = document.getBoolean("waterEnabled") ?: false
                waterInterval.setText(document.getString("waterIntervalHours") ?: "2")

                sleepEnabled.isChecked = document.getBoolean("sleepEnabled") ?: false
                sleepTime.setText(document.getString("sleepTime") ?: "21:30")

                eyeRestEnabled.isChecked = document.getBoolean("eyeRestEnabled") ?: false
                eyeRestInterval.setText(document.getString("eyeRestIntervalMinutes") ?: "20")

                dailyCheckinEnabled.isChecked = document.getBoolean("dailyCheckinEnabled") ?: false
                dailyCheckinTime.setText(document.getString("dailyCheckinTime") ?: "20:00")

                activityEnabled.isChecked = document.getBoolean("activityEnabled") ?: false
                inactiveHours.setText(document.getString("inactiveHours") ?: "2")
            }

        val saveButton = actionButton("Save Reminder Settings")
        saveButton.setOnClickListener {
            val data = hashMapOf<String, Any>(
                "waterEnabled" to waterEnabled.isChecked,
                "waterIntervalHours" to waterInterval.text.toString().trim().ifBlank { "2" },
                "sleepEnabled" to sleepEnabled.isChecked,
                "sleepTime" to sleepTime.text.toString().trim().ifBlank { "21:30" },
                "eyeRestEnabled" to eyeRestEnabled.isChecked,
                "eyeRestIntervalMinutes" to eyeRestInterval.text.toString().trim().ifBlank { "20" },
                "dailyCheckinEnabled" to dailyCheckinEnabled.isChecked,
                "dailyCheckinTime" to dailyCheckinTime.text.toString().trim().ifBlank { "20:00" },
                "activityEnabled" to activityEnabled.isChecked,
                "inactiveHours" to inactiveHours.text.toString().trim().ifBlank { "2" },
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Reminder settings saved.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "Failed to save reminder settings.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        root.addView(saveButton)
    }

    private fun addSingleSelect(
        root: LinearLayout,
        title: String,
        options: List<String>
    ): SingleSelectState {
        val state = SingleSelectState()

        root.addView(sectionTitle(title))

        val optionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(optionContainer)

        val buttons = mutableListOf<TextView>()

        fun refresh() {
            buttons.forEach { button ->
                val selected = button.text.toString() == state.value

                button.setTextColor(
                    if (selected) {
                        Color.WHITE
                    } else {
                        textPrimary
                    }
                )

                button.background = if (selected) {
                    roundedBg(primaryColor, primaryColor, 2, 20f)
                } else {
                    roundedBg(surfaceColor, borderColor, 2, 20f)
                }
            }
        }

        options.forEach { option ->
            val optionView = optionButton(option)
            optionView.setOnClickListener {
                state.value = option
                refresh()
            }

            buttons.add(optionView)
            optionContainer.addView(optionView)
        }

        state.refresh = { refresh() }

        return state
    }

    private fun addMultiSelect(
        root: LinearLayout,
        title: String,
        options: List<String>
    ): MultiSelectState {
        val state = MultiSelectState()

        root.addView(sectionTitle(title))

        val optionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(optionContainer)

        val buttons = mutableListOf<TextView>()

        fun refresh() {
            buttons.forEach { button ->
                val selected = state.values.contains(button.text.toString())

                button.setTextColor(
                    if (selected) {
                        Color.WHITE
                    } else {
                        textPrimary
                    }
                )

                button.background = if (selected) {
                    roundedBg(primaryColor, primaryColor, 2, 20f)
                } else {
                    roundedBg(surfaceColor, borderColor, 2, 20f)
                }
            }
        }

        options.forEach { option ->
            val optionView = optionButton(option)
            optionView.setOnClickListener {
                if (option == "None") {
                    state.values.clear()
                    state.values.add("None")
                } else {
                    state.values.remove("None")

                    if (state.values.contains(option)) {
                        state.values.remove(option)
                    } else {
                        state.values.add(option)
                    }
                }

                refresh()
            }

            buttons.add(optionView)
            optionContainer.addView(optionView)
        }

        state.refresh = { refresh() }

        return state
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

    private fun pageTitle(pageType: String): String {
        return when (pageType) {
            "my_profile" -> "My Profile"
            "health_background" -> "Health Background"
            "lifestyle_profile" -> "Lifestyle Profile"

            else -> "Profile"
        }
    }

    private fun pageDescription(pageType: String): String {
        return when (pageType) {
            "my_profile" -> "Manage your basic profile, wellness information, and emergency contact."
            "health_background" -> "Save conditions, allergies, medications, and risk factors that may affect wellness guidance."
            "lifestyle_profile" -> "Set your usual sleep, water, activity, screen time, stress, and common triggers."

            else -> "Manage your wellness profile."
        }
    }

    private fun cardContainer(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        }
    }

    private fun optionButton(textValue: String): TextView {
        return TextView(requireContext()).apply {
            text = textValue
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            background = roundedBg(surfaceColor, borderColor, 2, 20f)
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                topMargin = dp(8)
            }
        }
    }

    private fun input(hintValue: String): EditText {
        return EditText(requireContext()).apply {
            hint = hintValue
            textSize = 14f
            setTextColor(textPrimary)
            setHintTextColor(Color.rgb(130, 145, 145))
            setPadding(dp(16), 0, dp(16), 0)
            background = roundedBg(Color.rgb(247, 250, 250), borderColor, 2, 18f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun numberInput(hintValue: String): EditText {
        return input(hintValue).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun multiLineInput(hintValue: String): EditText {
        return EditText(requireContext()).apply {
            hint = hintValue
            textSize = 14f
            gravity = Gravity.TOP
            minLines = 4
            setTextColor(textPrimary)
            setHintTextColor(Color.rgb(130, 145, 145))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(Color.rgb(247, 250, 250), borderColor, 2, 18f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(120)
            ).apply {
                topMargin = dp(10)
            }
        }
    }

    private fun checkBox(textValue: String): CheckBox {
        return CheckBox(requireContext()).apply {
            text = textValue
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            buttonTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            setPadding(0, dp(8), 0, dp(4))
        }
    }

    private fun infoCard(title: String, body: String): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(surfaceColor, borderColor, 2, 24f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
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

    private fun emptyState(message: String): LinearLayout {
        val card = cardContainer()
        card.gravity = Gravity.CENTER

        val icon = TextView(requireContext()).apply {
            text = "🌿"
            textSize = 34f
            gravity = Gravity.CENTER
        }

        val messageView = TextView(requireContext()).apply {
            text = message
            textSize = 15f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1f)
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(icon)
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
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(18), 0, dp(6))
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
                topMargin = dp(16)
            }
        }
    }

    private fun readStringList(document: DocumentSnapshot, key: String): List<String> {
        val raw = document.get(key) as? List<*> ?: return emptyList()

        return raw.mapNotNull {
            it as? String
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
}