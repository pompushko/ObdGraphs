package org.obd.graphs.bl.trip

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mikephil.charting.data.Entry
import org.obd.graphs.ApplicationContext
import org.obd.graphs.Cache
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.ui.common.Colors
import org.obd.graphs.ui.graph.ValueScaler
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.isEnabled
import org.obd.graphs.ui.preferences.profile.getCurrentProfile
import org.obd.graphs.ui.preferences.profile.getProfileList
import org.obd.metrics.api.model.ObdMetric
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"
private const val LOGGER_KEY = "TripRecorder"
private const val MIN_TRIP_LENGTH = 5

private val labelColor = Color.parseColor("#C22636")

private val profileColors = mutableMapOf<String,Int>().apply {
    val colors = Colors().generate()
    getProfileList().forEach { (s, _) ->
        put(s,colors.nextInt())
    }
}

data class TripDesc (val fileName:String, val profileId:String, val profileLabel:String, val startTime: String, val tripTimeSec: String){

    fun displayString(): Spanned {
        val text = "[${profileLabel}] $startTime (${tripTimeSec}s)"

        return SpannableString(text).apply {
            setSpan(
                RelativeSizeSpan(0.5f), 0, text.indexOf("]") + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            setSpan(
                ForegroundColorSpan(profileColors[profileId]!!),
                0, text.indexOf("]") + 1,0
            )

            setSpan(
                ForegroundColorSpan(labelColor),
                text.indexOf("("),
                text.indexOf(")") + 1,
                0
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TripEntry(
    val id: Long,
    val entries: MutableList<Entry>,
    var min: Number = 0,
    var max: Number = 0,
    var mean: Number = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val startTs: Long, val entries: MutableMap<Long, TripEntry>)

class TripRecorder private constructor() {

    companion object {

        @JvmStatic
        val instance: TripRecorder = TripRecorder().apply {
            val trip = Trip(startTs = System.currentTimeMillis(), entries = mutableMapOf())
            Cache[CACHE_TRIP_PROPERTY_NAME] = trip
            Log.i(LOGGER_KEY, "Init Trip with stamp: $${trip.startTs}")
        }
    }

    private val valueScaler = ValueScaler()
    private val context: Context by lazy { ApplicationContext.get()!! }
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())

    fun addTripEntry(metric: ObdMetric) {
        try {
            getTripFromCache()?.let { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val key = metric.command.pid.id
                val newRecord =
                    Entry(ts, valueScaler.scaleToNewRange(metric), key)

                if (trip.entries.containsKey(key)) {
                    val tripEntry = trip.entries[key]!!
                    tripEntry.entries.add(newRecord)
                } else {
                    trip.entries[key] = TripEntry(id = key, entries = mutableListOf(newRecord))
                }
            }
        } catch (e: Throwable) {
            Log.e(LOGGER_KEY, "Failed to add cache entry", e)
        }
    }

    fun getCurrentTrip(): Trip {
        if (null == getTripFromCache()) {
            startNewTrip(System.currentTimeMillis())
        }

        val trip = getTripFromCache()!!
        Log.i(LOGGER_KEY, "Get current trip ts: '${dateFormat.format(Date(trip.startTs))}'")
        return trip
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_KEY, "Starting new trip, time stamp: '${dateFormat.format(Date(newTs))}'")
        updateCache(newTs)
    }


    fun saveCurrentTrip() {
        getTripFromCache()?.let { trip ->
            val histogram = DataLogger.instance.diagnostics().histogram()
            val pidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()

            trip.entries.forEach { (t, u) ->
                val histogramSupplier = histogram.findBy(pidDefinitionRegistry.findBy(t))
                u.max = histogramSupplier.max
                u.min = histogramSupplier.min
                u.mean = histogramSupplier.mean
            }

            val endDate = Date()
            val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")

            val tripLength = if (trip.startTs == 0L) 0 else {
                (endDate.time - trip.startTs) / 1000
            }

            Log.i(LOGGER_KEY, "Recorded trip, length: ${tripLength}s")

            if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                val startString = dateFormat.format(Date(trip.startTs))

                val content: String = jacksonObjectMapper().writeValueAsString(trip)
                val fileName = "trip-${getCurrentProfile()}-${startString}-${tripLength}.json"
                Log.i(LOGGER_KEY, "Saving the trip to the file: $fileName")
                writeFile(context, fileName, content)

                Log.i(LOGGER_KEY, "Trip was written to the file: $fileName")
            } else {
                Log.i(LOGGER_KEY, "Trip was no saved. Trip time is less than ${trip.startTs}s")
            }
        }
    }

    fun findAllTripsBy(query: String = ".json"): MutableList<TripDesc>? {
        Log.i(LOGGER_KEY, "Find all trips with query: $query")

        val profiles = getProfileList()

        val result = context.cacheDir.list()?.filter { it.startsWith("trip_") || it.contains("") }
            ?.sortedByDescending { it }
            ?.map { fileName ->

                val p = fileName.substring(0, fileName.length - 5).split("-")

                if (p.size < 3) {
                    return null
                }

                val profileId = p[1]
                val profileLabel = profiles[profileId]!!


                TripDesc(fileName = fileName,
                    profileId = profileId,
                    profileLabel =  profileLabel,
                    startTime = p[2],
                    tripTimeSec = p[3])
            }
            ?.toMutableList()

        Log.i(LOGGER_KEY, "Find all trips with query: ${query}. Result size: ${result?.size}")

        return result
    }

    fun loadTrip(tripName: String) {
        Log.i(LOGGER_KEY, "Loading '$tripName' from disk.")

        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        } else {
            val file = File(context.cacheDir, tripName)
            try {
                val trip: Trip = jacksonObjectMapper().readValue(file, Trip::class.java)
                Log.i(LOGGER_KEY, "Trip '$tripName' was loaded from the disk.")
                Cache[CACHE_TRIP_PROPERTY_NAME] = trip
            } catch (e: FileNotFoundException) {
                Log.e(LOGGER_KEY, "Did not find trip '$tripName'.", e)
                updateCache(System.currentTimeMillis())
            }
        }
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        content: String
    ) {
        var fd: FileOutputStream? = null
        try {
            val file = File(context.cacheDir, fileName)
            fd = FileOutputStream(file).apply {
                write(content.toByteArray())
            }

        } finally {
            fd?.run {
                flush()
                close()
            }
        }
    }

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs, entries = mutableMapOf())
        Cache[CACHE_TRIP_PROPERTY_NAME] = trip
        Log.i(LOGGER_KEY, "Init new Trip with stamp: $${trip.startTs}")
    }

    private fun getTripFromCache(): Trip? = Cache[CACHE_TRIP_PROPERTY_NAME] as Trip?
}