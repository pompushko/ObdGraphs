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
package org.obd.graphs.bl.query

interface Query: java.io.Serializable {
    fun getDefaults():  Set<Long>
    fun getIDs(): MutableSet<Long>
    fun getStrategy(): QueryStrategyType
    fun setStrategy(queryStrategyType: QueryStrategyType): Query
    fun update(newPIDs: Set<Long>): Query
    fun filterBy(filter: String): Set<Long>
    fun apply(filter: String): Query
    fun apply(filter: Set<Long>): Query
    companion object {
        fun instance (queryStrategyType: QueryStrategyType = QueryStrategyType.SHARED_QUERY): Query = QueryStrategyOrchestrator().apply {
            setStrategy(queryStrategyType)
        }
    }
}
