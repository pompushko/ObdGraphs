/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.bl.collector.CarMetricsCollector
import kotlin.math.min


private const val LOG_KEY = "GiuliaScreenRenderer"

private const val CURRENT_MIN = 22f
private const val CURRENT_MAX = 72f
private const val NEW_MAX = 1.6f
private const val NEW_MIN = 0.6f
private const val AREA_MAX_WIDTH = 500
private const val FOOTER_SIZE_RATIO = 1.3f

@Suppress("NOTHING_TO_INLINE")
internal class GiuliaScreenRenderer(
    context: Context,
    settings: ScreenSettings,
    private val metricsCollector: CarMetricsCollector,
    fps: Fps
) : AbstractRenderer(settings, context, fps) {

    private val valueScaler = ValueScaler()
    private val drawer = GiuliaDrawer(context, settings)

    override fun onDraw(canvas: Canvas, drawArea: Rect?) {

        drawArea?.let { area ->

            if (area.isEmpty) {
                area[0, 0, canvas.width - 1] = canvas.height - 1
            }

            val metrics = metricsCollector.metrics()
            val (valueTextSize, textSizeBase) = calculateFontSize(area)

            drawer.canvas = canvas
            drawer.drawBackground(area)

            var top = area.top + textSizeBase / 2
            var left = drawer.getMarginLeft(area)

            if (settings.isStatusPanelEnabled()) {
                top = drawer.drawStatusBar(area, fps.get()) + 18
                drawer.drawDivider(left, area.width().toFloat(), area.top + 10f, Color.DKGRAY)
            }

            val topCpy = top
            var valueTop = initialValueTop(area)

            splitIntoChunks(metrics).forEach { chunk ->
                chunk.forEach lit@{ metric ->
                    top = drawMetric(
                        area = area,
                        metric = metric,
                        textSizeBase = textSizeBase,
                        valueTextSize = valueTextSize,
                        left = left,
                        top = top,
                        valueTop = valueTop
                    )
                }

                if (settings.getMaxColumns() > 1) {
                    valueTop += area.width() / 2 - 18
                }

                left += calculateLeftMargin(area)
                top = calculateTop(textSizeBase, top, topCpy)
            }
        }
    }

    override fun release() {
        drawer.recycle()
    }


    private inline fun drawMetric(
        area: Rect,
        metric: CarMetric,
        textSizeBase: Float,
        valueTextSize: Float,
        left: Float,
        top: Float,
        valueTop: Float,
    ): Float {

        var top1 = top
        val footerValueTextSize = textSizeBase / FOOTER_SIZE_RATIO
        val footerTitleTextSize = textSizeBase / FOOTER_SIZE_RATIO / FOOTER_SIZE_RATIO
        var left1 = left

        drawer.drawTitle(
            metric, left1, top1,
            textSizeBase
        )

        drawer.drawValue(
            metric,
            valueTop,
            top1 + 10,
            valueTextSize
        )

        if (settings.isHistoryEnabled()) {
            top1 += textSizeBase / FOOTER_SIZE_RATIO
            left1 = drawer.drawText(
                "min",
                left,
                top1,
                Color.DKGRAY,
                footerTitleTextSize
            )
            left1 = drawer.drawText(
                metric.toNumber(metric.min),
                left1,
                top1,
                Color.LTGRAY,
                footerValueTextSize
            )

            left1 = drawer.drawText(
                "max",
                left1,
                top1,
                Color.DKGRAY,
                footerTitleTextSize
            )
            left1 = drawer.drawText(
                metric.toNumber(metric.max),
                left1,
                top1,
                Color.LTGRAY,
                footerValueTextSize
            )

            if (metric.source.command.pid.historgam.isAvgEnabled) {
                left1 = drawer.drawText(
                    "avg",
                    left1,
                    top1,
                    Color.DKGRAY,
                    footerTitleTextSize
                )

                left1 = drawer.drawText(
                    metric.toNumber(metric.mean),
                    left1,
                    top1,
                    Color.LTGRAY,
                    footerValueTextSize
                )
            }

            drawer.drawAlertingLegend(metric, left1, top1)

        } else {
            top1 += 12
        }

        top1 += 6f

        drawer.drawProgressBar(
            left,
            itemWidth(area).toFloat(), top1, metric,
            color = settings.colorTheme().progressColor
        )

        top1 += calculateDividerSpacing()

        drawer.drawDivider(
            left, itemWidth(area).toFloat(), top1,
            color = settings.colorTheme().dividerColor
        )

        top1 += (textSizeBase * 1.7).toInt()

        if (top1 > area.height()) {
            if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                Log.v(LOG_KEY, "Skipping entry to display verticalPos=$top1},area.height=${area.height()}")
            }
            return top1
        }

        return top1
    }

    private inline fun calculateFontSize(
        area: Rect
    ): Pair<Float, Float> {

        val scaleRatio = valueScaler.scaleToNewRange(settings.getFontSize().toFloat(), CURRENT_MIN, CURRENT_MAX, NEW_MIN, NEW_MAX)

        val areaWidth = min(
            when (settings.getMaxColumns()) {
                1 -> area.width()
                else -> area.width() / 2
            }, AREA_MAX_WIDTH
        )

        val valueTextSize = (areaWidth / 10f) * scaleRatio
        val textSizeBase = (areaWidth / 16f) * scaleRatio

        if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
            Log.v(
                LOG_KEY,
                "areaWidth=$areaWidth valueTextSize=$valueTextSize textSizeBase=$textSizeBase scaleRatio=$scaleRatio"
            )
        }
        return Pair(valueTextSize, textSizeBase)
    }

    private inline fun calculateDividerSpacing() = when (settings.getMaxColumns()) {
        1 -> 14
        else -> 8
    }

    private inline fun calculateTop(
        textHeight: Float,
        top: Float,
        topCpy: Float
    ): Float = when (settings.getMaxColumns()) {
        1 -> top + (textHeight / 3) - 10
        else -> topCpy
    }

    private inline fun calculateLeftMargin(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> 0
            else -> (area.width() / 2)
        }

    private inline fun itemWidth(area: Rect): Int =
        when (settings.getMaxColumns()) {
            1 -> area.width()
            else -> area.width() / 2
        }
}