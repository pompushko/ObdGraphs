package org.obd.graphs.preferences.pid

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.obd.graphs.bl.datalogger.pidResources
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.metrics.pid.PidDefinition

private const val EXPERIMENTAL_LABEL  = "(Experimental)"

internal fun PidDefinition.displayString(): Spanned {
    val text = "[${pidResources.getDefaultPidFiles()[resourceFile]?: resourceFile}] ${longDescription?:description} " +  (if (stable) "" else EXPERIMENTAL_LABEL)
    return SpannableString(text).apply {
        var endIndexOf = text.indexOf("]") + 1
        setSpan(
            RelativeSizeSpan(0.5f), 0, endIndexOf,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        setSpan(
            ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, endIndexOf,
            0
        )

        if (!stable){
            val startIndexOf = text.indexOf(EXPERIMENTAL_LABEL)
            endIndexOf = startIndexOf + EXPERIMENTAL_LABEL.length

            setSpan(
                RelativeSizeSpan(0.5f), startIndexOf, endIndexOf,
               0
            )

            setSpan(
                ForegroundColorSpan(COLOR_CARDINAL),
                startIndexOf,
                endIndexOf,
                0
            )
        }
    }
}



