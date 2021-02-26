package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.model.GradientColor
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.preferences.Preferences
import java.util.*
import kotlin.collections.ArrayList


class DashViewAdapter internal constructor(
    context: Context,
    data: MutableList<ObdMetric>
) :
    RecyclerView.Adapter<DashViewAdapter.ViewHolder>() {
    var mData: MutableList<ObdMetric> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val ctx: Context = context

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(mData, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view: View = mInflater.inflate(R.layout.dash_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val commandReply = mData.elementAt(position)
        val obdCommand = commandReply.command as ObdCommand
        holder.buildChart(obdCommand.pid)

        var segmentNum: Int = holder.segments.indexOf(commandReply.valueToDouble())
        (segmentNum > 0).apply {
            //reset
            (0 until holder.chart.data.dataSetCount).reversed().forEach { e ->
                val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                dataSet.color = Color.parseColor("#0D000000")//transparent
            }

            (0..segmentNum).forEach { e ->
                val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                dataSet.color = Color.rgb(124, 252, 79)
                val gradientColors: MutableList<GradientColor> = ArrayList()
                gradientColors.add(GradientColor(Color.rgb(124, 252, 79), Color.rgb(243, 249, 167)))
                dataSet.gradientColors = gradientColors
            }

            if (Preferences.isEnabled(ctx, "pref.dash.top.values.hl")) {
                val percent75: Int = (holder.segments.numOfSegments * 75) / 100
                if (segmentNum > percent75) {
                    (percent75..segmentNum).forEach { e ->
                        val dataSet = holder.chart.data.getDataSetByIndex(e) as BarDataSet
                        dataSet.color = Color.rgb(124, 252, 79)
                        val gradientColors: MutableList<GradientColor> = ArrayList()
                        gradientColors.add(GradientColor(Color.rgb(237, 33, 58),Color.rgb(147, 41, 30)))
                        dataSet.gradientColors = gradientColors
                    }
                }
            }
            holder.chart.invalidate()
        }

        holder.units.text = (obdCommand.pid).units
        holder.value.text = commandReply.valueToString()
        holder.label.text = obdCommand.pid.description
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var chart: BarChart = itemView.findViewById(R.id.chart)
        var label: TextView = itemView.findViewById(R.id.dash_label)
        var value: TextView = itemView.findViewById(R.id.dash_value)
        var units: TextView = itemView.findViewById(R.id.dash_units)

        lateinit var segments: Segments
        var initialized: Boolean = false

        fun buildChart(pid: PidDefinition) {
            if (initialized) {
            } else {
                val numOfSegments = 30
                this.segments = Segments(numOfSegments, pid.min.toDouble(), pid.max.toDouble())
                this.label.text = pid.description
                chart.setDrawBarShadow(false)
                chart.setDrawValueAboveBar(false)
                chart.setTouchEnabled(false)

                chart.setDrawBorders(false)
                chart.setAddStatesFromChildren(false)

                chart.description.isEnabled = false
                chart.setPinchZoom(false)
                chart.setDrawGridBackground(false)

                val xAxis = chart.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.setDrawLabels(true)
                xAxis.setDrawGridLinesBehindData(false)
                xAxis.setDrawLimitLinesBehindData(false)
                xAxis.setDrawAxisLine(false)
                xAxis.setCenterAxisLabels(false)

                val leftAxis = chart.axisLeft
                leftAxis.axisMinimum = pid.min.toFloat()
                leftAxis.setDrawGridLines(false)
                leftAxis.setDrawTopYLabelEntry(false)
                leftAxis.setDrawAxisLine(false)
                leftAxis.setDrawGridLinesBehindData(false)
                leftAxis.setDrawLabels(false)
                leftAxis.setDrawZeroLine(false)
                leftAxis.setDrawLimitLinesBehindData(false)

                leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                leftAxis.spaceTop = 15f

                val rightAxis = chart.axisRight
                rightAxis.setDrawGridLines(false)

                val legend = chart.legend
                legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.setDrawInside(false)
                legend.form = Legend.LegendForm.SQUARE

                val dataSets: ArrayList<IBarDataSet> = ArrayList()

                this.segments.to().forEach { v: Double ->
                    val values: ArrayList<BarEntry> = ArrayList()
                    values.add(BarEntry(v.toFloat(), v.toFloat()))
                    val set1 = BarDataSet(values, "")
                    set1.setDrawIcons(false)
                    set1.setDrawValues(false)
                    set1.color = Color.rgb(187, 187, 187)
                    dataSets.add(set1)
               }

                val data = BarData(dataSets)
                data.setDrawValues(false)

                data.barWidth = pid.max.toFloat() / this.segments.numOfSegments / 1.2f
                chart.data = data
                initialized = true
            }
        }
    }
}