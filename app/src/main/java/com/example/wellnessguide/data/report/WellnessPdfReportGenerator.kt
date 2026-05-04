package com.example.wellnessguide.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfReportLogItem(
    val logType: String,
    val title: String,
    val status: String,
    val summary: String,
    val recommendations: String,
    val createdAt: Long
)

object WellnessPdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 38f

    private val primaryColor = Color.rgb(26, 107, 114)
    private val softGreen = Color.rgb(238, 248, 247)
    private val borderColor = Color.rgb(221, 237, 234)
    private val textPrimary = Color.rgb(28, 43, 45)
    private val textSecondary = Color.rgb(93, 122, 126)

    fun write(
        context: Context,
        uri: Uri,
        logs: List<PdfReportLogItem>,
        filterLabel: String
    ) {
        val pdf = PdfDocument()
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Unable to open PDF file.")

        var pageNumber = 1
        var page = pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )

        var canvas: Canvas = page.canvas
        var y = 0f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val whiteSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
        }

        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11.5f
        }

        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 10f
        }

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        val softPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = softGreen
            style = Paint.Style.FILL
        }

        fun drawFooter() {
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(150, 160, 160)
                textSize = 9f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                "Wellness Guide Report - Page $pageNumber",
                PAGE_WIDTH / 2f,
                PAGE_HEIGHT - 18f,
                footerPaint
            )
        }

        fun finishCurrentPage() {
            drawFooter()
            pdf.finishPage(page)
        }

        fun startNewPage() {
            finishCurrentPage()

            pageNumber += 1
            page = pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )

            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - 55f) {
                startNewPage()
            }
        }

        fun cleanText(value: String): String {
            return value
                .replace("\t", " ")
                .replace("•", "-")
                .replace("–", "-")
                .replace("—", "-")
                .trim()
        }

        fun drawWrappedText(
            text: String,
            x: Float,
            startY: Float,
            maxWidth: Float,
            paint: Paint,
            lineGap: Float = 15f,
            maxLines: Int = 999
        ): Float {
            val words = cleanText(text)
                .replace("\n", " \n ")
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            var line = ""
            var localY = startY
            var linesDrawn = 0

            for (word in words) {
                if (word == "\n") {
                    if (line.isNotBlank()) {
                        canvas.drawText(line, x, localY, paint)
                        localY += lineGap
                        linesDrawn++
                        line = ""
                    }

                    if (linesDrawn >= maxLines) break
                    continue
                }

                val testLine = if (line.isBlank()) word else "$line $word"

                if (paint.measureText(testLine) <= maxWidth) {
                    line = testLine
                } else {
                    if (line.isNotBlank()) {
                        canvas.drawText(line, x, localY, paint)
                        localY += lineGap
                        linesDrawn++
                    }

                    line = word

                    if (linesDrawn >= maxLines) break
                }
            }

            if (line.isNotBlank() && linesDrawn < maxLines) {
                canvas.drawText(line, x, localY, paint)
                localY += lineGap
            }

            return localY
        }

        fun drawStatusCircle(x: Float, circleY: Float, color: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

            canvas.drawCircle(x, circleY, 6f, paint)
        }

        fun drawBadge(
            text: String,
            x: Float,
            badgeY: Float,
            color: Int
        ) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = statusBgColor(text)
                style = Paint.Style.FILL
            }

            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 1.1f
            }

            val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val rect = RectF(x, badgeY, x + 72f, badgeY + 24f)

            canvas.drawRoundRect(rect, 12f, 12f, fill)
            canvas.drawRoundRect(rect, 12f, 12f, border)
            canvas.drawText(text, rect.centerX(), badgeY + 16f, badgeText)
        }

        fun drawHeader() {
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                style = Paint.Style.FILL
            }

            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 124f, headerPaint)

            canvas.drawText("Wellness Guide Report", MARGIN, 50f, titlePaint)

            val generatedDate = SimpleDateFormat(
                "MMM dd, yyyy - hh:mm a",
                Locale.getDefault()
            ).format(Date())

            canvas.drawText("Generated: $generatedDate", MARGIN, 76f, whiteSmallPaint)
            canvas.drawText("Filter: $filterLabel", MARGIN, 96f, whiteSmallPaint)

            y = 150f
        }

        fun drawSectionTitle(title: String) {
            ensureSpace(38f)
            canvas.drawText(title, MARGIN, y, headingPaint)
            y += 22f
        }

        fun drawOverviewCard() {
            ensureSpace(125f)

            val cardTop = y
            val cardBottom = y + 112f

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                softPaint
            )

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                borderPaint
            )

            val latest = logs.maxByOrNull { it.createdAt }

            val totalLogs = logs.size
            val latestStatus = latest?.status ?: "No status"
            val symptomCount = logs.count {
                it.logType == "symptom" || it.logType == "full_assessment"
            }
            val sleepCount = logs.count { it.logType == "sleep" }
            val moodCount = logs.count { it.logType == "mental" }
            val activityCount = logs.count {
                it.logType == "physical_activity" || it.logType == "route_activity"
            }

            val leftX = MARGIN + 18f
            val rightX = PAGE_WIDTH / 2f + 8f
            var rowY = cardTop + 28f

            canvas.drawText("Total Logs: $totalLogs", leftX, rowY, subHeadingPaint)
            canvas.drawText("Latest Status: ${shortStatus(latestStatus)}", rightX, rowY, subHeadingPaint)

            rowY += 26f
            canvas.drawText("Symptoms / Assessments: $symptomCount", leftX, rowY, normalPaint)
            canvas.drawText("Sleep Logs: $sleepCount", rightX, rowY, normalPaint)

            rowY += 22f
            canvas.drawText("Mood & Stress Logs: $moodCount", leftX, rowY, normalPaint)
            canvas.drawText("Physical Activity Logs: $activityCount", rightX, rowY, normalPaint)

            y = cardBottom + 28f
        }

        fun drawStatusGuide() {
            ensureSpace(120f)

            val cardTop = y
            val cardBottom = y + 102f

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                cardPaint
            )

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                borderPaint
            )

            canvas.drawText("Status Guide", MARGIN + 18f, cardTop + 28f, subHeadingPaint)

            val guideY = cardTop + 55f

            drawStatusCircle(MARGIN + 20f, guideY, Color.rgb(5, 150, 105))
            canvas.drawText("Green - Low concern / improving", MARGIN + 38f, guideY + 4f, normalPaint)

            drawStatusCircle(MARGIN + 20f, guideY + 24f, Color.rgb(217, 119, 6))
            canvas.drawText("Yellow - Needs monitoring", MARGIN + 38f, guideY + 28f, normalPaint)

            drawStatusCircle(MARGIN + 20f, guideY + 48f, Color.rgb(220, 38, 38))
            canvas.drawText("Red - Seek medical advice for severe or worsening symptoms", MARGIN + 38f, guideY + 52f, normalPaint)

            y = cardBottom + 28f
        }

        fun drawDisclaimer() {
            ensureSpace(100f)

            val cardTop = y
            val cardBottom = y + 82f

            val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 251, 235)
                style = Paint.Style.FILL
            }

            val warningBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(217, 119, 6)
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                warningPaint
            )

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                warningBorder
            )

            canvas.drawText("Important Disclaimer", MARGIN + 18f, cardTop + 28f, subHeadingPaint)

            drawWrappedText(
                "This report is for wellness tracking only. It is not a medical diagnosis. Consult a healthcare professional for severe, persistent, or worsening symptoms.",
                MARGIN + 18f,
                cardTop + 50f,
                PAGE_WIDTH - (MARGIN * 2) - 36f,
                normalPaint,
                14f,
                3
            )

            y = cardBottom + 28f
        }

        fun drawLogCard(log: PdfReportLogItem) {
            val summary = shorten(log.summary, 420)
            val recommendations = shorten(log.recommendations, 320)

            val estimatedHeight = 178f

            ensureSpace(estimatedHeight)

            val cardTop = y
            val cardBottom = y + estimatedHeight

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                cardPaint
            )

            canvas.drawRoundRect(
                RectF(MARGIN, cardTop, PAGE_WIDTH - MARGIN, cardBottom),
                18f,
                18f,
                borderPaint
            )

            val x = MARGIN + 18f
            var textY = cardTop + 28f

            canvas.drawText(log.title.ifBlank { "Wellness Log" }, x, textY, subHeadingPaint)

            val statusText = shortStatus(log.status)

            drawBadge(
                text = statusText,
                x = PAGE_WIDTH - MARGIN - 90f,
                badgeY = cardTop + 13f,
                color = statusColor(log.status)
            )

            textY += 20f

            canvas.drawText(
                "${displayLogType(log.logType)} - ${formatFullDate(log.createdAt)}",
                x,
                textY,
                smallPaint
            )

            textY += 25f

            canvas.drawText("Summary", x, textY, subHeadingPaint)
            textY += 18f

            textY = drawWrappedText(
                summary,
                x,
                textY,
                PAGE_WIDTH - (MARGIN * 2) - 36f,
                normalPaint,
                14f,
                4
            )

            textY += 8f

            canvas.drawText("Recommendations", x, textY, subHeadingPaint)
            textY += 18f

            drawWrappedText(
                recommendations,
                x,
                textY,
                PAGE_WIDTH - (MARGIN * 2) - 36f,
                normalPaint,
                14f,
                3
            )

            y = cardBottom + 18f
        }

        fun drawLatestResult() {
            val latest = logs.maxByOrNull { it.createdAt } ?: return

            drawSectionTitle("Latest Result")
            drawLogCard(latest)
        }

        try {
            drawHeader()
            drawOverviewCard()
            drawStatusGuide()
            drawDisclaimer()
            drawLatestResult()

            drawSectionTitle("Wellness Logs")

            if (logs.isEmpty()) {
                canvas.drawText("No logs available for this date filter.", MARGIN, y, normalPaint)
            } else {
                logs.sortedByDescending { it.createdAt }.forEach { log ->
                    drawLogCard(log)
                }
            }

            finishCurrentPage()
            pdf.writeTo(output)
        } finally {
            output.close()
            pdf.close()
        }
    }

    private fun shortStatus(status: String): String {
        return when {
            status.contains("Red", true) -> "Red"
            status.contains("Yellow", true) -> "Yellow"
            status.contains("Green", true) -> "Green"
            else -> status.ifBlank { "Status" }
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
            else -> softGreen
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

    private fun formatFullDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown date"

        return SimpleDateFormat(
            "MMM dd, yyyy - hh:mm a",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    private fun shorten(text: String, max: Int): String {
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
}