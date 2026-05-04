package com.example.wellnessguide.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object WellnessUi {

    fun screen(
        context: Context,
        pageTitle: String,
        progressText: String,
        onMenuClick: () -> Unit
    ): Pair<ScrollView, LinearLayout> {
        val scrollView = ScrollView(context).apply {
            setBackgroundColor(Color.rgb(238, 248, 247))
            isFillViewport = true
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 22), dp(context, 18), dp(context, 22), dp(context, 40))
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 56)
            )
        }

        val menu = TextView(context).apply {
            text = "☰"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(28, 43, 45))
            background = roundedBg(Color.WHITE, Color.rgb(221, 237, 234), 2, 40f)
            layoutParams = LinearLayout.LayoutParams(dp(context, 44), dp(context, 44))
            setOnClickListener { onMenuClick() }
        }

        val topTitle = TextView(context).apply {
            text = pageTitle
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(context, 12)
            }
        }

        header.addView(menu)
        header.addView(topTitle)

        root.addView(header)
        root.addView(bigTitle(context, pageTitle))
        root.addView(progress(context, progressText))

        scrollView.addView(root)
        return Pair(scrollView, root)
    }

    fun bigTitle(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            setPadding(0, dp(context, 18), 0, 0)
        }
    }

    fun progress(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(26, 107, 114))
            setPadding(0, dp(context, 6), 0, dp(context, 10))
        }
    }

    fun sectionTitle(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(28, 43, 45))
            setPadding(0, dp(context, 20), 0, dp(context, 10))
        }
    }

    fun paragraph(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 15f
            setTextColor(Color.rgb(93, 122, 126))
            setLineSpacing(5f, 1f)
            setPadding(0, dp(context, 8), 0, dp(context, 8))
        }
    }

    fun disclaimer(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 13f
            setTextColor(Color.rgb(145, 75, 0))
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
            background = roundedBg(Color.rgb(255, 250, 220), Color.rgb(245, 185, 65), 2, 18f)
        }
    }

    fun resultCard(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 14f
            setTextColor(Color.rgb(28, 43, 45))
            setLineSpacing(5f, 1f)
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16))
            background = roundedBg(Color.WHITE, Color.rgb(221, 237, 234), 2, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(context, 16)
            }
        }
    }

    fun input(context: Context, hintValue: String, heightDp: Int = 56): EditText {
        return EditText(context).apply {
            hint = hintValue
            textSize = 14f
            setTextColor(Color.rgb(28, 43, 45))
            setHintTextColor(Color.rgb(120, 120, 120))
            setPadding(dp(context, 16), 0, dp(context, 16), 0)
            background = roundedBg(Color.WHITE, Color.rgb(221, 237, 234), 2, 18f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, heightDp)
            ).apply {
                topMargin = dp(context, 10)
            }
        }
    }

    fun actionButton(context: Context, textValue: String): Button {
        return Button(context).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = roundedBg(Color.rgb(26, 107, 114), Color.rgb(26, 107, 114), 2, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 56)
            ).apply {
                topMargin = dp(context, 10)
            }
        }
    }

    fun secondaryButton(context: Context, textValue: String): Button {
        return Button(context).apply {
            text = textValue
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.rgb(28, 43, 45))
            background = roundedBg(Color.WHITE, Color.rgb(221, 237, 234), 2, 22f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 56)
            ).apply {
                topMargin = dp(context, 10)
            }
        }
    }

    fun optionsContainer(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
    }

    fun addSingleOptions(
        container: LinearLayout,
        options: List<String>,
        onPick: (String) -> Unit
    ) {
        container.removeAllViews()
        var selectedView: TextView? = null

        options.forEach { option ->
            val item = optionView(container.context, option)

            item.setOnClickListener {
                selectedView?.background = optionBg(false)
                selectedView = item
                item.background = optionBg(true)
                onPick(option)
            }

            container.addView(item)
        }
    }

    fun addMultiOptions(
        container: LinearLayout,
        options: List<String>,
        selected: MutableSet<String>,
        onChange: () -> Unit = {}
    ) {
        container.removeAllViews()

        options.forEach { option ->
            val item = optionView(container.context, option)

            item.setOnClickListener {
                if (selected.contains(option)) {
                    selected.remove(option)
                    item.background = optionBg(false)
                } else {
                    selected.add(option)
                    item.background = optionBg(true)
                }

                onChange()
            }

            container.addView(item)
        }
    }

    private fun optionView(context: Context, textValue: String): TextView {
        return TextView(context).apply {
            text = textValue
            textSize = 15f
            setTextColor(Color.rgb(28, 43, 45))
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            background = optionBg(false)
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 10)
            }
        }
    }

    private fun optionBg(selected: Boolean): GradientDrawable {
        return roundedBg(
            if (selected) Color.rgb(238, 248, 247) else Color.WHITE,
            if (selected) Color.rgb(26, 107, 114) else Color.rgb(221, 237, 234),
            if (selected) 4 else 2,
            22f
        )
    }

    fun roundedBg(
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

    fun spacer(context: Context, heightDp: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, heightDp)
            )
        }
    }

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}