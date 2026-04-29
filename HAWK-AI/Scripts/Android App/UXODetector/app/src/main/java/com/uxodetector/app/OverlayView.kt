package com.uxodetector.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: List<DetectionResult> = emptyList()

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FF4444")
        isAntiAlias = true
    }

    private val safeBoxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#00FF88")
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 255, 68, 68)
        isAntiAlias = true
    }

    private val safeFillPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 0, 255, 136)
        isAntiAlias = true
    }

    private val labelBgPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 0, 0, 0)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isAntiAlias = true
        isFakeBoldText = true
    }

    private val confidenceTextPaint = Paint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 30f
        isAntiAlias = true
    }

    fun setResults(detectionResults: List<DetectionResult>) {
        results = detectionResults
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        for (result in results) {
            val box = result.boundingBox
            val isSafe = result.label.contains("Safe", ignoreCase = true)

            val scaledBox = RectF(
                box.left * viewWidth,
                box.top * viewHeight,
                box.right * viewWidth,
                box.bottom * viewHeight
            )

            // Draw fill
            canvas.drawRect(scaledBox, if (isSafe) safeFillPaint else fillPaint)
            // Draw border
            canvas.drawRect(scaledBox, if (isSafe) safeBoxPaint else boxPaint)

            // Draw corner accents
            drawCornerAccents(canvas, scaledBox, if (isSafe) safeBoxPaint else boxPaint)

            // Draw label background
            val label = result.label
            val confidenceText = "%.1f%%".format(result.confidence * 100)
            val labelWidth = textPaint.measureText(label)
            val labelHeight = textPaint.textSize + confidenceTextPaint.textSize + 20f
            val labelTop = (scaledBox.top - labelHeight).coerceAtLeast(0f)

            val bgRect = RectF(
                scaledBox.left,
                labelTop,
                scaledBox.left + maxOf(labelWidth, textPaint.measureText(confidenceText)) + 16f,
                labelTop + labelHeight
            )
            canvas.drawRect(bgRect, labelBgPaint)

            // Draw label text
            canvas.drawText(label, scaledBox.left + 8f, labelTop + textPaint.textSize, textPaint)
            canvas.drawText(
                confidenceText,
                scaledBox.left + 8f,
                labelTop + textPaint.textSize + confidenceTextPaint.textSize + 4f,
                confidenceTextPaint
            )
        }
    }

    private fun drawCornerAccents(canvas: Canvas, box: RectF, paint: Paint) {
        val length = 24f
        val stroke = paint.strokeWidth

        val accentPaint = Paint(paint).apply { strokeWidth = stroke * 2.5f }

        // Top-left
        canvas.drawLine(box.left, box.top, box.left + length, box.top, accentPaint)
        canvas.drawLine(box.left, box.top, box.left, box.top + length, accentPaint)
        // Top-right
        canvas.drawLine(box.right - length, box.top, box.right, box.top, accentPaint)
        canvas.drawLine(box.right, box.top, box.right, box.top + length, accentPaint)
        // Bottom-left
        canvas.drawLine(box.left, box.bottom - length, box.left, box.bottom, accentPaint)
        canvas.drawLine(box.left, box.bottom, box.left + length, box.bottom, accentPaint)
        // Bottom-right
        canvas.drawLine(box.right - length, box.bottom, box.right, box.bottom, accentPaint)
        canvas.drawLine(box.right, box.bottom - length, box.right, box.bottom, accentPaint)
    }
}
