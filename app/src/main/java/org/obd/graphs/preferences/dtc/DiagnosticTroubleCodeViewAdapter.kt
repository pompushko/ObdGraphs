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
package org.obd.graphs.preferences.dtc

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.setText

class DiagnosticTroubleCodeViewAdapter internal constructor(
    context: Context?,
    private var data: MutableCollection<String>,
) : RecyclerView.Adapter<DiagnosticTroubleCodeViewAdapter.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder = ViewHolder(mInflater.inflate(R.layout.item_dtc, parent, false))

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        data.elementAt(position).run {
            holder.code.setText(this, COLOR_CARDINAL, Typeface.NORMAL, 1f)
        }
        holder.description.setText("", Color.GRAY, Typeface.NORMAL, 1f)
    }

    override fun getItemCount(): Int = data.size

    inner class ViewHolder internal constructor(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        var code: TextView = itemView.findViewById(R.id.metadata_value)
        var description: TextView = itemView.findViewById(R.id.dtc_description)
    }
}
