package com.example.wellnessguide.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class HeadacheView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onRegionSelected: ((String) -> Unit)? = null
    private var selectedRegion: String? = null

    private lateinit var forehead: RectF
    private lateinit var top: RectF
    private lateinit var left: RectF
    private lateinit var right: RectF
    private lateinit var lower: RectF

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val headTop = 70f
        val head = RectF(cx - 120f, headTop + 60f, cx + 120f, headTop + 360f)

        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(232, 248, 255)
            style = Paint.Style.FILL
        }

        val blueStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 170, 255)
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        canvas.drawOval(head, facePaint)
        canvas.drawOval(head, blueStroke)

        val neck = RectF(cx - 50f, headTop + 345f, cx + 50f, headTop + 440f)
        canvas.drawRect(neck, facePaint)
        canvas.drawRect(neck, blueStroke)

        forehead = RectF(cx - 28f, headTop, cx + 28f, headTop + 56f)
        top = RectF(cx - 28f, headTop + 55f, cx + 28f, headTop + 111f)
        left = RectF(cx - 130f, headTop + 140f, cx - 74f, headTop + 196f)
        right = RectF(cx + 74f, headTop + 140f, cx + 130f, headTop + 196f)
        lower = RectF(cx - 28f, headTop + 270f, cx + 28f, headTop + 326f)

        drawPainCircle(canvas, forehead, "forehead")
        drawPainCircle(canvas, top, "top")
        drawPainCircle(canvas, left, "left")
        drawPainCircle(canvas, right, "right")
        drawPainCircle(canvas, lower, "lower")

        val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(140, 215, 235)
            style = Paint.Style.FILL
        }

        canvas.drawCircle(cx - 45f, headTop + 200f, 8f, eyePaint)
        canvas.drawCircle(cx + 45f, headTop + 200f, 8f, eyePaint)

        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(140, 215, 235)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val smile = Path().apply {
            moveTo(cx - 28f, headTop + 245f)
            quadTo(cx, headTop + 260f, cx + 28f, headTop + 245f)
        }

        canvas.drawPath(smile, smilePaint)
    }

    private fun drawPainCircle(canvas: Canvas, rect: RectF, id: String) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selectedRegion == id) Color.rgb(255, 45, 55) else Color.rgb(255, 190, 190)
            style = Paint.Style.FILL
        }

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selectedRegion == id) Color.rgb(30, 41, 59) else Color.rgb(255, 120, 120)
            style = Paint.Style.STROKE
            strokeWidth = if (selectedRegion == id) 5f else 2f
        }

        canvas.drawOval(rect, fill)
        canvas.drawOval(rect, stroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val region = when {
            forehead.contains(event.x, event.y) -> "forehead"
            top.contains(event.x, event.y) -> "top"
            left.contains(event.x, event.y) -> "left"
            right.contains(event.x, event.y) -> "right"
            lower.contains(event.x, event.y) -> "lower"
            else -> null
        }

        if (region != null) {
            selectedRegion = region
            invalidate()
            onRegionSelected?.invoke(region)
        }

        return true
    }
}