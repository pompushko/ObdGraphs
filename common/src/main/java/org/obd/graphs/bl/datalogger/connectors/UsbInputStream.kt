package org.obd.graphs.bl.datalogger.connectors

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.InputStream

private const val MAX_READ_ATTEMPTS = 7
private const val TERMINATOR_CHAR = '>'
private const val MAX_READ_SIZE = 16 * 1024
private const val LOGGER_TAG = "USB_CONNECTION"

class UsbInputStream(val port: UsbSerialPort) : InputStream() {

    private val buffer =
        ByteArray(MAX_READ_SIZE).apply { fill(0, 0, size) }

    private val tmp =
        ByteArray(MAX_READ_SIZE).apply { fill(0, 0, size) }

    private var buffeReadPos = 0
    private var bytesRead = 0

    override fun read(b: ByteArray): Int {
        return port.read(b, IO_TIMEOUT)
    }

    override fun read(): Int {
        return try {
            if (buffeReadPos == 0) {
                fillBuffer()
            } else {
                readFromBuffer()
            }
        } catch (e: java.lang.Exception) {
            Log.i(LOGGER_TAG, "Failed to read data ", e)
            -1
        }
    }

    private fun fillBuffer(): Int {
        buffer.run { fill(0, 0, bytesRead) }
        tmp.run { fill(0, 0, size) }

        var cnt = 0
        for (it in 1..MAX_READ_ATTEMPTS) {
            bytesRead = port.read(tmp, IO_TIMEOUT)
            if (bytesRead > 0) {
                System.arraycopy(tmp, 0, buffer, cnt, bytesRead)
                cnt += bytesRead
                if (buffer[cnt - 1].toInt().toChar() == TERMINATOR_CHAR) {
                    break
                }
            }
        }
        bytesRead = cnt

        if (bytesRead == 0) {
            return -1
        }
        return buffer[buffeReadPos++].toInt()
    }

    private fun readFromBuffer(): Int =
        if (buffeReadPos < bytesRead && buffer[buffeReadPos].toInt()
                .toChar() != TERMINATOR_CHAR
        ) {
            buffer[buffeReadPos++].toInt()
        } else {
            buffeReadPos = 0
            -1
        }
}