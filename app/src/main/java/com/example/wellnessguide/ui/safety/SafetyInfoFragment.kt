package com.example.wellnessguide.ui.safety

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

class SafetyInfoFragment : Fragment() {

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
        val pageType = arguments?.getString("pageType") ?: "warning"

        val scroll = ScrollView(requireContext()).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(40))
        }

        addTopBar(root, pageTitle(pageType))

        when (pageType) {
            "warning" -> buildWarningSigns(root)
            "disclaimer" -> buildDisclaimer(root)
            "consult" -> buildConsultProfessional(root)
            "credits" -> buildCredits(root)
            else -> buildWarningSigns(root)
        }

        scroll.addView(root)
        return scroll
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

    private fun buildWarningSigns(root: LinearLayout) {
        root.addView(bigTitle("Warning Signs"))

        root.addView(
            paragraph(
                "These signs may need urgent attention. If you experience any of these, it is safer to seek help from a healthcare professional or emergency service immediately."
            )
        )

        root.addView(
            safetyNote(
                "This guide gives general wellness information only. It is not a medical diagnosis."
            )
        )

        val signs = listOf(
            Triple("Chest Pain", "Chest pain or pressure can be serious and should not be ignored.", "🔴"),
            Triple("Difficulty Breathing", "Trouble breathing, shortness of breath, or wheezing may need urgent help.", "🔴"),
            Triple("Fainting", "Fainting or feeling like you may pass out needs closer attention.", "🟠"),
            Triple("Confusion", "Sudden confusion, disorientation, or unusual behavior may be a warning sign.", "🟠"),
            Triple("Severe Headache", "A sudden or severe headache, especially with vision problems, weakness, or confusion, should be checked.", "🟠"),
            Triple("Severe Dehydration", "Very low urine, extreme thirst, dizziness, or dry mouth may indicate dehydration.", "🟡"),
            Triple("Persistent Vomiting", "Vomiting that does not stop can cause dehydration and needs monitoring.", "🟡"),
            Triple("Symptoms Getting Worse", "If symptoms are worsening over time, it is safer to consult a professional.", "🟡")
        )

        signs.forEach { item ->
            root.addView(
                infoCard(
                    icon = item.third,
                    title = item.first,
                    body = item.second,
                    accentColor = warningColor(item.third)
                )
            )
        }
    }

    private fun buildDisclaimer(root: LinearLayout) {
        root.addView(bigTitle("Disclaimer"))

        root.addView(
            paragraph(
                "Please read this before using the wellness guide."
            )
        )

        root.addView(
            infoCard(
                icon = "ℹ️",
                title = "General Wellness Guide Only",
                body = "Wellness Guide provides general wellness information, symptom tracking, lifestyle insights, and recovery reminders.",
                accentColor = primaryColor
            )
        )

        root.addView(
            infoCard(
                icon = "⚕️",
                title = "Not a Medical Diagnosis",
                body = "This app does not diagnose, treat, cure, or prevent any medical condition. It should not replace professional medical advice.",
                accentColor = Color.rgb(217, 119, 6)
            )
        )

        root.addView(
            infoCard(
                icon = "🔴",
                title = "For Severe Symptoms",
                body = "Please consult a healthcare professional for severe, persistent, or worsening symptoms. For urgent symptoms, contact emergency services immediately.",
                accentColor = Color.rgb(220, 38, 38)
            )
        )

        root.addView(
            infoCard(
                icon = "🌿",
                title = "Purpose of the App",
                body = "The app helps you organize wellness logs, notice lifestyle patterns, review tips, and remember when to monitor symptoms.",
                accentColor = Color.rgb(5, 150, 105)
            )
        )
    }

    private fun buildConsultProfessional(root: LinearLayout) {
        root.addView(bigTitle("Consult a Professional"))

        root.addView(
            paragraph(
                "Use this page as a simple guide for deciding when it may be safer to ask for professional help."
            )
        )

        root.addView(
            statusGuideCard()
        )

        val symptoms = listOf(
            "Symptoms are severe",
            "Symptoms are getting worse",
            "Symptoms last longer than expected",
            "Chest pain or difficulty breathing",
            "Feeling faint, confused, or extremely weak",
            "High fever that does not improve",
            "Persistent vomiting or signs of dehydration",
            "Symptoms suddenly change while taking medication",
            "You are unsure whether your symptoms are serious"
        )

        root.addView(
            checklistCard(
                icon = "✓",
                title = "Symptoms that may need professional help",
                items = symptoms,
                footer = "If one or more of these applies to you, consider contacting a healthcare professional for safer guidance.",
                accentColor = primaryColor
            )
        )

        val existingConditions = listOf(
            "Asthma or breathing problems",
            "Diabetes or blood sugar concerns",
            "High blood pressure or heart problems",
            "Frequent migraines or severe headaches",
            "History of seizures or fainting",
            "Kidney, liver, or immune system conditions",
            "Pregnancy or recently gave birth",
            "Taking maintenance medicine or multiple medications"
        )

        root.addView(
            checklistCard(
                icon = "⚕️",
                title = "Existing Health Conditions",
                items = existingConditions,
                footer = "If you have an existing condition, it may be safer to ask a professional when symptoms appear, worsen, or suddenly change.",
                accentColor = Color.rgb(217, 119, 6)
            )
        )

        val urgentWarningSigns = listOf(
            "Severe chest pain or pressure",
            "Severe difficulty breathing",
            "Sudden weakness, numbness, or trouble speaking",
            "Loss of consciousness or repeated fainting",
            "Severe allergic reaction such as swelling of the face or throat",
            "Heavy bleeding or serious injury",
            "Severe confusion, drowsiness, or unusual behavior"
        )

        root.addView(
            checklistCard(
                icon = "!",
                title = "Urgent Warning Signs",
                items = urgentWarningSigns,
                footer = "If any of these warning signs are present, seek urgent medical help or emergency care as soon as possible.",
                accentColor = Color.rgb(220, 38, 38)
            )
        )

        val whenToMonitor = listOf(
            "Symptoms are mild and improving",
            "No severe pain or breathing difficulty",
            "You can eat, drink, and rest normally",
            "Temperature is going down",
            "You feel better after rest or basic self-care",
            "Symptoms do not interfere with daily activities"
        )

        root.addView(
            checklistCard(
                icon = "i",
                title = "When Monitoring May Be Okay",
                items = whenToMonitor,
                footer = "Continue to observe your symptoms. If they worsen, last longer than expected, or make you worried, consult a professional.",
                accentColor = Color.rgb(37, 99, 235)
            )
        )

        val beforeConsulting = listOf(
            "When did the symptoms start?",
            "What symptoms are you feeling?",
            "Are the symptoms getting better or worse?",
            "Do you have existing health conditions?",
            "Are you taking any medicine right now?",
            "Do you have allergies to medicine or food?",
            "Have you experienced this before?"
        )

        root.addView(
            checklistCard(
                icon = "📝",
                title = "Before Consulting, Prepare These Details",
                items = beforeConsulting,
                footer = "Preparing these details can help the doctor, nurse, or health professional understand your situation faster.",
                accentColor = Color.rgb(22, 163, 74)
            )
        )
    }

    private fun buildCredits(root: LinearLayout) {
        root.addView(bigTitle("Credits"))

        root.addView(
            paragraph(
                "This wellness guide was created through the effort, teamwork, and dedication of the following contributors."
            )
        )

        root.addView(
            creditsHeaderCard()
        )

        val members = listOf(
            "Agtas, Gideon",
            "Ciubal, Terence Vincent",
            "Clemente, Robyn Paul",
            "Espanol, Earl Justine",
            "Masong, Dan Julian",
            "Tapulgo, Carl",
            "Yumang, Kurt"
        )

        members.forEachIndexed { index, name ->
            root.addView(
                creditMemberCard(
                    number = index + 1,
                    name = name
                )
            )
        }

        root.addView(
            infoCard(
                icon = "🌿",
                title = "Wellness Guide Team",
                body = "Thank you for using Wellness Guide. This app is designed to support wellness awareness, symptom tracking, and safer health decisions.",
                accentColor = Color.rgb(5, 150, 105)
            )
        )
    }
    private fun creditsHeaderCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
                bottomMargin = dp(6)
            }
        }

        val icon = TextView(requireContext()).apply {
            text = "👥"
            textSize = 34f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(8))
        }

        val title = TextView(requireContext()).apply {
            text = "Project Contributors"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(requireContext()).apply {
            text = "The people behind the Wellness Guide application."
            textSize = 14f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1f)
            setPadding(0, dp(6), 0, 0)
        }

        card.addView(icon)
        card.addView(title)
        card.addView(subtitle)

        return card
    }

    private fun creditMemberCard(
        number: Int,
        name: String
    ): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(surfaceColor, borderColor, 2, 22f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }

        val badge = TextView(requireContext()).apply {
            text = number.toString()
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(238, 248, 247), primaryColor, 1, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }

        val nameView = TextView(requireContext()).apply {
            text = name
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(14)
            }
        }

        card.addView(badge)
        card.addView(nameView)

        return card
    }
    private fun checklistCard(
        icon: String,
        title: String,
        items: List<String>,
        footer: String,
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
            background = roundedBg(Color.rgb(250, 253, 252), borderColor, 1, 40f)
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

        textColumn.addView(titleView)

        items.forEach { item ->
            val row = TextView(requireContext()).apply {
                text = "✓  $item"
                textSize = 13f
                setTextColor(textSecondary)
                setLineSpacing(4f, 1f)
                setPadding(0, dp(8), 0, 0)
            }
            textColumn.addView(row)
        }

        val footerView = TextView(requireContext()).apply {
            text = footer
            textSize = 13f
            setTextColor(textPrimary)
            setLineSpacing(4f, 1f)
            setPadding(0, dp(12), 0, 0)
        }

        textColumn.addView(footerView)

        card.addView(iconView)
        card.addView(textColumn)

        return card
    }

    private fun statusGuideCard(): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(surfaceColor, borderColor, 2, 26f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
                bottomMargin = dp(8)
            }
        }

        val title = TextView(requireContext()).apply {
            text = "Wellness Status Guide"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
        }

        card.addView(title)
        card.addView(statusRow("Green", "Low concern / normal monitoring", Color.rgb(5, 150, 105), Color.rgb(236, 253, 245)))
        card.addView(statusRow("Yellow", "Needs monitoring / mild to moderate concern", Color.rgb(217, 119, 6), Color.rgb(255, 251, 235)))
        card.addView(statusRow("Red", "Seek medical advice / high concern", Color.rgb(220, 38, 38), Color.rgb(254, 242, 242)))

        return card
    }

    private fun statusRow(
        label: String,
        meaning: String,
        color: Int,
        bgColor: Int
    ): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }

        val badge = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color)
            gravity = Gravity.CENTER
            background = roundedBg(bgColor, color, 1, 30f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            layoutParams = LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val desc = TextView(requireContext()).apply {
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
        row.addView(desc)
        return row
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
            background = roundedBg(Color.rgb(250, 253, 252), borderColor, 1, 40f)
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

    private fun bigTitle(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(textPrimary)
            setPadding(0, dp(18), 0, dp(4))
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

    private fun safetyNote(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 13f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(
                Color.rgb(255, 250, 220),
                Color.rgb(245, 185, 65),
                2,
                20f
            )
        }
    }

    private fun pageTitle(type: String): String {
        return when (type) {
            "warning" -> "Warning Signs"
            "disclaimer" -> "Disclaimer"
            "consult" -> "Consult a Professional"
            "credits" -> "Credits"
            else -> "Safety"
        }
    }

    private fun warningColor(icon: String): Int {
        return when (icon) {
            "🔴" -> Color.rgb(220, 38, 38)
            "🟠" -> Color.rgb(217, 119, 6)
            "🟡" -> Color.rgb(202, 138, 4)
            else -> primaryColor
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