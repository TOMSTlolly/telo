package com.tomst.lolly.ui.graph

import android.graphics.Canvas
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer

class CustomBarChartRenderer(viewPortHandler: ViewPortHandler, xAxis: XAxis, trans: Transformer) : XAxisRenderer(viewPortHandler, xAxis, trans) {

    override fun renderAxisLabels(c: Canvas) {

        if (!mXAxis.isEnabled || !mXAxis.isDrawLabelsEnabled)
            return

        val yoffset = mXAxis.yOffset

        mAxisLabelPaint.typeface = mXAxis.typeface
        mAxisLabelPaint.textSize = mXAxis.textSize
        mAxisLabelPaint.color = mXAxis.textColor

        val pointF = MPPointF.getInstance(0f, 0f)
        if (mXAxis.position == XAxis.XAxisPosition.TOP) {
            pointF.x = 0.5f
            pointF.y = 1.0f
            drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)

        } else if (mXAxis.position == XAxis.XAxisPosition.TOP_INSIDE) {
            pointF.x = 0.5f
            pointF.y = 1.0f
            drawLabels(c, mViewPortHandler.contentBottom() - yoffset, pointF)

        } else if (mXAxis.position == XAxis.XAxisPosition.BOTTOM) {
            pointF.x = 0.5f
            pointF.y = 0.0f
            drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)

        } else if (mXAxis.position == XAxis.XAxisPosition.BOTTOM_INSIDE) {
            pointF.x = 0.5f
            pointF.y = 0.0f
            drawLabels(c, mViewPortHandler.contentBottom() - yoffset - mXAxis.mLabelRotatedHeight.toFloat(), pointF)

        } else { // BOTH SIDED
            pointF.x = 0.5f
            pointF.y = 1.0f
            drawLabels(c, mViewPortHandler.contentTop() - yoffset, pointF)
            pointF.x = 0.5f
            pointF.y = 0.0f
            drawLabels(c, mViewPortHandler.contentBottom() + yoffset, pointF)
        }
        MPPointF.recycleInstance(pointF)
    }
}