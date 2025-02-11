 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.renderer.dynamic

import android.content.Context
import android.graphics.*
import org.obd.graphs.renderer.AbstractDrawer
import org.obd.graphs.renderer.GaugeProgressBarType
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.gauge.DrawerSettings
import org.obd.graphs.renderer.gauge.GaugeDrawer
import org.obd.graphs.renderer.trip.TripInfoDrawer

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f

@Suppress("NOTHING_TO_INLINE")
internal class PerformanceDrawer(context: Context, settings: ScreenSettings) : AbstractDrawer(context, settings) {

    private val gaugeDrawer = GaugeDrawer(
        settings = settings, context = context,
        drawerSettings = DrawerSettings(
            gaugeProgressBarType = GaugeProgressBarType.LONG)
    )

    private val tripInfoDrawer = TripInfoDrawer(context, settings)

    private val background: Bitmap =
        BitmapFactory.decodeResource(context.resources, org.obd.graphs.renderer.R.drawable.drag_race_bg)

    override fun getBackground(): Bitmap = background

    override fun recycle() {
        super.getBackground().recycle()
        this.background.recycle()
    }

    inline fun drawScreen(
        canvas: Canvas,
        area: Rect,
        left: Float,
        top: Float,
        performanceInfoDetails: PerformanceInfoDetails
    ) {

        val (textSizeBase) = calculateFontSize(area)
        val x = maxItemWidth(area) + 4

        var rowTop = top + 12f
        var leftAlignment = 0
        performanceInfoDetails.airTemp?.let { tripInfoDrawer.drawMetric(it, top = rowTop, left = left + (leftAlignment++) * x, canvas, textSizeBase, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.coolantTemp?.let {  tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSizeBase, statsEnabled = true,area=area, castToInt = true) }
        performanceInfoDetails.oilTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSizeBase, statsEnabled = true,area=area, castToInt = true) }
        performanceInfoDetails.exhaustTemp?.let { tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSizeBase, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.gearboxOilTemp?.let { tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSizeBase, statsEnabled = true, area=area, castToInt = true) }
        performanceInfoDetails.ambientTemp?.let{ tripInfoDrawer.drawMetric(it, rowTop, left + (leftAlignment++) * x, canvas, textSizeBase, area=area) }

        drawDivider(canvas, left, area.width().toFloat(), rowTop + textSizeBase + 4, Color.DKGRAY)

        rowTop += textSizeBase + 16

        performanceInfoDetails.torque?.let {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = area.left.toFloat(),
                top = rowTop,
                width = area.width() / 2.6f,
                metric = it,
                labelCenterYPadding = 18f,
                fontSize = settings.getDynamicScreenSettings().fontSize,
                scaleEnabled = false
            )
        }

        performanceInfoDetails.gas?.let {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = (area.left + area.width() / 2.8f) - 6f,
                top =  rowTop - 4f,
                width = area.width() / 3.8f,
                metric = it,
                labelCenterYPadding = 26f,
                fontSize = settings.getDynamicScreenSettings().fontSize,
                scaleEnabled = false
            )
        }

        performanceInfoDetails.intakePressure?.let {
            gaugeDrawer.drawGauge(
                canvas = canvas,
                left = (area.left + area.width() / 1.65f) ,
                top =  rowTop,
                width = area.width() / 2.6f,
                metric = it,
                labelCenterYPadding = 18f,
                fontSize = settings.getDynamicScreenSettings().fontSize,
                scaleEnabled = false
            )
        }
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = getScaleRatio()

        val areaWidth = area.width()
        val valueTextSize = (areaWidth / 17f) * scaleRatio
        val textSizeBase = (areaWidth / 22f) * scaleRatio
        return Pair(valueTextSize, textSizeBase)
    }


    private inline fun getScaleRatio() = valueConverter.scaleToNewRange(
        settings.getDynamicScreenSettings().fontSize.toFloat(),
        CURRENT_MIN,
        CURRENT_MAX,
        NEW_MIN,
        NEW_MAX
    )

    private inline fun maxItemWidth(area: Rect) = (area.width() / 6)
}
