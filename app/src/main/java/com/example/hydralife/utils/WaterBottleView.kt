package com.example.hydralife.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.hydralife.R
import kotlin.math.sin

class WaterBottleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetProgress: Float = 0f
    private var currentProgress: Float = 0f
    private var wavePhase: Float = 0f

    private val bottlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = ContextCompat.getColor(context, R.color.primary_container)
    }

    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.argb(120, 255, 255, 255)
    }

    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.primary)
    }

    private val waterPath = Path()
    private val bottlePath = Path()
    private val capPath = Path()

    private var waveAnimator: ValueAnimator? = null
    private var fillAnimator: ValueAnimator? = null

    init {
        currentProgress = 0f
        targetProgress = 0f
        startWaveAnimation()
    }

    fun setProgress(progress: Float, animate: Boolean = true) {
        targetProgress = progress.coerceIn(0f, 1f)
        if (!animate) {
            currentProgress = targetProgress
            invalidate()
            return
        }

        fillAnimator?.cancel()
        fillAnimator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = 900
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun startWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { animator ->
                wavePhase = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        waveAnimator?.cancel()
        fillAnimator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bodyWidth = w * 0.68f
        val bodyHeight = h * 0.82f
        val capHeight = h * 0.1f
        val capTop = h * 0.04f
        val bodyTop = capTop + capHeight
        val bodyLeft = (w - bodyWidth) / 2f

        waterPaint.shader = LinearGradient(
            bodyLeft, bodyTop, bodyLeft, bodyTop + bodyHeight,
            intArrayOf(Color.parseColor("#70D6E8"), Color.parseColor("#4A90E2")),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val capWidth = width * 0.35f
        val capHeight = height * 0.1f
        val capLeft = (width - capWidth) / 2f
        val capTop = height * 0.04f

        val bodyWidth = width * 0.68f
        val bodyHeight = height * 0.82f
        val bodyLeft = (width - bodyWidth) / 2f
        val bodyTop = capTop + capHeight
        val cornerRadius = 36f

        // Draw Cap
        capPath.reset()
        capPath.addRoundRect(capLeft, capTop, capLeft + capWidth, capTop + capHeight, 14f, 14f, Path.Direction.CW)
        canvas.drawPath(capPath, capPaint)

        // Draw Bottle Body Outline
        bottlePath.reset()
        bottlePath.addRoundRect(bodyLeft, bodyTop, bodyLeft + bodyWidth, bodyTop + bodyHeight, cornerRadius, cornerRadius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(bottlePath)

        // Draw Water Fill with Wave
        val waterHeight = bodyHeight * currentProgress
        val waterTop = bodyTop + bodyHeight - waterHeight

        waterPath.reset()
        waterPath.moveTo(bodyLeft, bodyTop + bodyHeight)
        waterPath.lineTo(bodyLeft, waterTop)

        val waveLength = bodyWidth
        val waveAmplitude = 9f
        val frequency = 2f * Math.PI.toFloat() / waveLength

        var x = bodyLeft
        while (x <= bodyLeft + bodyWidth) {
            val y = waterTop + sin(x * frequency + wavePhase) * waveAmplitude
            waterPath.lineTo(x, y)
            x += 2f
        }

        waterPath.lineTo(bodyLeft + bodyWidth, bodyTop + bodyHeight)
        waterPath.close()

        canvas.drawPath(waterPath, waterPaint)

        // Draw Bottle Glass Highlight
        canvas.drawLine(bodyLeft + 14f, bodyTop + 24f, bodyLeft + 14f, bodyTop + bodyHeight - 24f, highlightPaint)

        canvas.restore()

        // Draw Bottle Outline Border
        canvas.drawPath(bottlePath, bottlePaint)
    }
}
