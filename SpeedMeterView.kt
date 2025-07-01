package com.general.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

class MeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var currentSpeed = 0f
        set(value) {
            field = value.coerceIn(0f, maxSpeed)
        }
    var maxSpeed = 50f
        set(value) {
            field = value
            currentSpeed = currentSpeed.coerceIn(0f, value)
        }
    var padding = 0f
        set(value) {
            field = value
            if (width > 0 && height > 0) {
                setupDialPath(width, height)
            }
        }
    var cornerRadius = 60f                      // 圆角半径
        set(value) {
            field = value
            if (width > 0 && height > 0) {
                setupDialPath(width, height)
            }
        }
    var totalScales = 50                        // 总刻度数
    var scaleLength = 24f                       // 刻度长度
        set(value) {
            field = value
            arrowLength = scaleLength * 1.2f
            arrowWidth = scaleLength * 0.4f
            if (width > 0 && height > 0) {
                setupDialPath(width, height)
            }
        }
    var scaleWidth = 5f                         // 刻度长度
    var scaleTextSize = 20f                     // 刻度值字体大小
        set(value) {
            field = value
            if (width > 0 && height > 0) {
                setupDialPath(width, height)
            }
            textPaint.textSize = scaleTextSize
        }
    var scaleEndAndBottomMargin: Float? = null
        set(value) {
            field = value
            if (width > 0 && height > 0) {
                setupDialPath(width, height)
            }
        }
    private var arrowLength = scaleLength * 1.2f        // 箭头长度
    private var arrowWidth = scaleLength * 0.4f         // 箭头宽度

    private val color = ContextCompat.getColor(context, com.general.R.color.color_black)
    var filledScaleColor: Int? = null
        set(value) {
            field = value
            filledScalePaint.color = value ?: color
        }
    private val scalePaint: Paint = Paint().apply {      // 表盘刻度画笔
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.STROKE
        color = this@MeterView.color
    }
    private val filledScalePaint: Paint = Paint().apply { // 实心刻度画笔
        isAntiAlias = true
        style = Paint.Style.FILL
        color = this@MeterView.filledScaleColor ?: color
    }
    private val textPaint: Paint = Paint().apply {      // 文字画笔
        isAntiAlias = true
        textSize = scaleTextSize
        textAlign = Paint.Align.CENTER
        color = this@MeterView.color
    }
    private val pointerPaint: Paint = Paint().apply {   // 指针画笔
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.FILL
        color = this@MeterView.color
    }

    private val dialPath = Path()                       // 表盘路径
    private val scalePath = Path()                      // 刻度路径
    private val pointerPath = Path()                    // 指针路径
    private val arcRect = RectF()                       // 圆弧区域
    private var pathMeasure: PathMeasure? = null        // 路径测量器
    private var pathLength: Float = 0f                  // 路径总长度
    private val pos = FloatArray(2)               // 位置坐标
    private val tan = FloatArray(2)               // 切线方向

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupDialPath(w, h)
    }

    private fun setupDialPath(w: Int, h: Int) {
        val padding = scaleLength + padding

        // 计算三个关键点
        val bottomLeftX = padding
        val bottomLeftY = h - (scaleEndAndBottomMargin ?: (scaleTextSize / 2))
        val topRightX = w - (scaleEndAndBottomMargin ?: (scaleTextSize / 2))
        val topRightY = padding

        // 计算圆弧的中心点
        val arcCenterX = padding + cornerRadius
        val arcCenterY = padding + cornerRadius

        // 设置圆弧区域
        arcRect.set(
            arcCenterX - cornerRadius,
            arcCenterY - cornerRadius,
            arcCenterX + cornerRadius,
            arcCenterY + cornerRadius
        )

        // 清除并重新创建路径
        dialPath.reset()

        // 从左下角开始
        dialPath.moveTo(bottomLeftX, bottomLeftY)
        // 画直线到圆弧起点
        dialPath.lineTo(bottomLeftX, arcCenterY + cornerRadius)
        // 添加圆弧
        dialPath.arcTo(arcRect, 180f, 90f)
        // 画直线到右上角
        dialPath.lineTo(topRightX, topRightY)

        // 初始化 PathMeasure
        pathMeasure = PathMeasure(dialPath, false).also { pathLength = it.length }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawScales(canvas)
        drawPointer(canvas)
    }

    private fun drawScales(canvas: Canvas) {
        val scaleSpacing = pathLength / totalScales

        for (i in 0..totalScales) {
            val distance = i * scaleSpacing
            val drawText = i % 10 == 0
            // 文字刻度在内部多延伸0.5倍长度
            val inwardExtension = if (drawText) scaleLength * 0.1f else 0f

            // 获取路径上的点和切线方向
            pathMeasure?.getPosTan(distance, pos, tan)

            // 计算垂直于切线的方向（顺时针旋转90度）
            val angle = Math.toDegrees(Math.atan2(tan[1].toDouble(), tan[0].toDouble()))

            // 计算刻度矩形的四个角点
            val halfWidth = scaleWidth / 2

            // 计算矩形起点（靠近路径的一端，带文字的刻度会向内延伸）
            val startLeftX = pos[0] + halfWidth * cos(Math.toRadians(angle)).toFloat() +
                    inwardExtension * cos(Math.toRadians(angle + 90)).toFloat()
            val startLeftY = pos[1] + halfWidth * sin(Math.toRadians(angle)).toFloat() +
                    inwardExtension * sin(Math.toRadians(angle + 90)).toFloat()
            val startRightX = pos[0] - halfWidth * cos(Math.toRadians(angle)).toFloat() +
                    inwardExtension * cos(Math.toRadians(angle + 90)).toFloat()
            val startRightY = pos[1] - halfWidth * sin(Math.toRadians(angle)).toFloat() +
                    inwardExtension * sin(Math.toRadians(angle + 90)).toFloat()

            // 计算矩形终点（远离路径的一端）
            val endLeftX = startLeftX - (scaleLength + inwardExtension) * cos(Math.toRadians(angle + 90)).toFloat()
            val endLeftY = startLeftY - (scaleLength + inwardExtension) * sin(Math.toRadians(angle + 90)).toFloat()
            val endRightX = startRightX - (scaleLength + inwardExtension) * cos(Math.toRadians(angle + 90)).toFloat()
            val endRightY = startRightY - (scaleLength + inwardExtension) * sin(Math.toRadians(angle + 90)).toFloat()

            // 绘制刻度矩形
            scalePath.reset()
            scalePath.moveTo(startLeftX, startLeftY)
            scalePath.lineTo(endLeftX, endLeftY)
            scalePath.lineTo(endRightX, endRightY)
            scalePath.lineTo(startRightX, startRightY)
            scalePath.close()

            // 根据当前速度决定是否填充刻度
            val scaleValue = (i.toFloat() / totalScales) * maxSpeed
            if (scaleValue <= currentSpeed) {
                canvas.drawPath(scalePath, filledScalePaint)
            } else {
                canvas.drawPath(scalePath, scalePaint)
            }

            // 绘制刻度值（每10个刻度画一次）
            if (drawText) {
                val textX = pos[0] + (scaleLength * 0.7f) * cos(Math.toRadians(angle + 90.0)).toFloat()
                val textY = pos[1] + (scaleLength * 0.7f) * sin(Math.toRadians(angle + 90.0)).toFloat() + 8
                canvas.drawText(i.toString(), textX, textY, textPaint)
            }
        }
    }

    private fun drawPointer(canvas: Canvas) {
        val distance = (currentSpeed / maxSpeed) * pathLength

        // 获取路径上的点和切线方向
        pathMeasure?.getPosTan(distance, pos, tan)

        // 计算垂直于切线的方向
        val angle = Math.toDegrees(Math.atan2(tan[1].toDouble(), tan[0].toDouble()))

        // 重置并创建箭头路径
        pointerPath.reset()

        // 箭头的顶点
        val tipX = pos[0] + arrowLength * .2f * cos(Math.toRadians(angle + 90)).toFloat()
        val tipY = pos[1] + arrowLength * .2f * sin(Math.toRadians(angle + 90)).toFloat()

        // 箭头的后部两个点
        val backLeftX = tipX + arrowLength * cos(Math.toRadians(angle + 90)).toFloat() +
                arrowWidth * 0.5f * cos(Math.toRadians(angle)).toFloat()
        val backLeftY = tipY + arrowLength * sin(Math.toRadians(angle + 90)).toFloat() +
                arrowWidth * 0.5f * sin(Math.toRadians(angle)).toFloat()

        val backRightX = tipX + arrowLength * cos(Math.toRadians(angle + 90)).toFloat() -
                arrowWidth * 0.5f * cos(Math.toRadians(angle)).toFloat()
        val backRightY = tipY + arrowLength * sin(Math.toRadians(angle + 90)).toFloat() -
                arrowWidth * 0.5f * sin(Math.toRadians(angle)).toFloat()

        // 绘制箭头路径
        pointerPath.moveTo(tipX, tipY)  // 移动到箭头顶点
        pointerPath.lineTo(backLeftX, backLeftY)  // 连接到左后点
        pointerPath.lineTo(backRightX, backRightY)  // 连接到右后点
        pointerPath.close()  // 闭合路径

        // 绘制箭头
        canvas.drawPath(pointerPath, pointerPaint)
    }
}