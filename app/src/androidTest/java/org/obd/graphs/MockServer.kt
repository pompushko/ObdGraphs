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
package org.obd.graphs

import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket

private const val LOG_TAG = "MockServer"

private const val DELAY_BETWEEN_COMMANDS = 5L

class MockServer(
    private val port: Int,
    requestResponse: Map<String, String> = mutableMapOf()
) : Runnable {

    private val thread: Thread = Thread(this)
    private lateinit var serverSocket: ServerSocket
    private lateinit var socket: Socket
    private lateinit var dataInputStream: DataInputStream
    private lateinit var dataOutputStream: DataOutputStream
    private var run: Boolean = true

    private val defaultRequestResponse = mutableMapOf(
        "ATD" to "ATD OK",
        "ATZ" to "ATZ OK",
        "ATL0" to "ATL0 OK",
        "ATH0" to "ATH0 OK",
        "ATE0" to "ATE0 OK",
        "ATPP 2CSV 01" to "ATPP 2CSV 01 OK",
        "ATPP 2C ON" to "ATPP 2C ON OK",
        "ATPP 2DSV 01" to "ATPP 2DSV 01 OK",
        "ATPP 2D ON" to "ATPP 2D ON OK",
        "ATAT2" to "ATAT2 OK",
        "ATSP0" to "ATSP0 OK",
        "0902" to "SEARCHING...0140:4902015756571:5A5A5A314B5A412:4D363930333932",
        "0100"  to "no data",
        "0120" to "no data",
        "0140" to "no data",
        "0160" to "no data",
        "0180" to "no data",
        "01A0" to "no data",
        "01C0" to "no data",
    )

    init {
        defaultRequestResponse.putAll(requestResponse)
    }

    fun launch() {
        Log.e(LOG_TAG, "Launching MockServer on port=$port")
        thread.priority = Thread.NORM_PRIORITY
        thread.start()
    }

    fun stop() {
        run = false
    }

    override fun run() {
        try {
            serverSocket = ServerSocket(port)
            run = true
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
        }

        if (waitForClient()) return


        while (run) {

            try {
               Thread.sleep(DELAY_BETWEEN_COMMANDS)

               readCommand()?.let { command ->
                    val commandResponse = defaultRequestResponse[command]
                    Log.i(LOG_TAG, "Received command: $command = $commandResponse")

                    if (commandResponse == null) {
                        runQuietly {
                            dataOutputStream.write("? >".toByteArray())
                            dataOutputStream.flush()
                        }
                    } else {
                        runQuietly {
                            dataOutputStream.write("$commandResponse >".toByteArray())
                            dataOutputStream.flush()
                        }
                    }
               }

            } catch (e: Exception) {
                Log.e(LOG_TAG, "Caught IO Exception", e)
                break
            }
        }
        runQuietly {
            dataOutputStream.close()
        }

        runQuietly {
            dataInputStream.close()
        }

        runQuietly {
            serverSocket.close()
        }


        Log.e(LOG_TAG, "Finishing Mock Server.")
    }

    private fun waitForClient(): Boolean {
        Log.i(LOG_TAG, "Waiting for upcoming connections")

        try {
            socket = serverSocket.accept()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
            return true
        }

        Log.i(LOG_TAG, "Client connected")

        try {
            dataInputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
            dataOutputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
            return true
        }
        return false
    }

    private fun readCommand(): String? {
        var cnt = 0
        var nextByte: Int
        val array = CharArray(255)
        while ((dataInputStream.read().also { nextByte = it } > -1)) {
            array[cnt++] = nextByte.toChar()
            if (nextByte.toChar() == '\r') {
                break
            }
        }

        if (cnt == 0) {
            Log.d(LOG_TAG, "Read no characters")
            return null
        }
        return array.copyOfRange(0, cnt - 1).concatToString()
    }
    private fun runQuietly(m: () -> Unit = {}){
        try {
            m.invoke()
        }catch (_: Exception){ }
    }
}