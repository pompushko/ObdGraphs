package org.openobd2.core.logger.ui.preferences.header

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.openobd2.core.logger.navigateToPreferencesScreen
import org.openobd2.core.logger.ui.preferences.Prefs

internal const val LOG_KEY = "Header"

internal const val CAN_HEADER_COUNTER_PREF = "pref.adapter.init.header.counter"

class HeaderNamePreference(
    context: Context?,
    attrs: AttributeSet?
) :
    EditTextPreference(context, attrs) {
    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            Log.i(LOG_KEY, "Adding new CAN header=$newValue")
            var numberOfHeaders = Prefs.getInt(CAN_HEADER_COUNTER_PREF, 0)
            numberOfHeaders++

            Prefs.edit().run {
                putInt(CAN_HEADER_COUNTER_PREF, numberOfHeaders)
                putString("pref.adapter.init.header.$numberOfHeaders", newValue.toString())
                apply()
            }

            navigateToPreferencesScreen("pref.init")
            true
        }
    }
}