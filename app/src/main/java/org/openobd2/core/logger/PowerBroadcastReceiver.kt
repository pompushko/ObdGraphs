package org.openobd2.core.logger

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import org.openobd2.core.logger.bl.datalogger.DataLoggerService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


const val SCREEN_OFF_EVENT = "power.screen.off"
const val SCREEN_ON_EVENT = "power.screen.on"

private const val CONNECT_TASK_DELAY_S = 5L

class PowerBroadcastReceiver : BroadcastReceiver() {

    private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val dataLoggerTask = Runnable {
        Log.i(ACTIVITY_LOGGER_TAG, "Start data logging")
        DataLoggerService.startAction()
    }

    override fun onReceive(context: Context?, intent: Intent) {
        val powerPreferences: PowerPreferences = getPowerPreferences()
        Log.i(
            ACTIVITY_LOGGER_TAG,
            "Received Power Event: ${intent.action}, powerPreferences.connectOnPower=${powerPreferences.connectOnPower}"
        )

        if (intent.action === Intent.ACTION_POWER_CONNECTED) {
            if (powerPreferences.switchNetworkOffOn) {
                true.run {
                    bluetooth(this)
                    wifi(this)
                    if (powerPreferences.connectOnPower) {
                        Log.i(
                            ACTIVITY_LOGGER_TAG,
                            "Schedule connect task WITH delay: $CONNECT_TASK_DELAY_S"
                        )
                        scheduleService.schedule(dataLoggerTask, CONNECT_TASK_DELAY_S, TimeUnit.SECONDS)
                    }
                }
            } else {
                if (powerPreferences.connectOnPower) {
                    DataLoggerService.startAction()
                }
            }

            if (powerPreferences.screenOnOff) {
                startMainActivity(context!!)
                context.sendBroadcast(Intent().apply {
                    action = SCREEN_ON_EVENT
                })
            }
        } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {
            if (powerPreferences.switchNetworkOffOn) {
                bluetooth(false)
                wifi(false)
            }

            if (powerPreferences.connectOnPower) {
                Log.i(
                    ACTIVITY_LOGGER_TAG,
                    "Stop data logging"
                )
                DataLoggerService.stopAction()
            }

            if (powerPreferences.screenOnOff) {
                context!!.sendBroadcast(Intent().apply {
                    action = SCREEN_OFF_EVENT
                })
            }
        }
    }

    private fun startMainActivity(context: Context) {
        if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun isActivityVisibleOnTheScreen(context: Context, activityClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = activityManager.getRunningTasks(1)
        Log.d(ACTIVITY_LOGGER_TAG, "Current top activity ${taskInfo[0].topActivity!!.className}")
        val componentInfo = taskInfo[0].topActivity
        return activityClass.canonicalName.equals(componentInfo!!.className, ignoreCase = true)
    }
}