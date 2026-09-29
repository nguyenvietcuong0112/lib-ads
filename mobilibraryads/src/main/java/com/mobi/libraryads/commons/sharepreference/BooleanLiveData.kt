package com.mobi.libraryads.commons.sharepreference

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class BooleanLiveData(
    private val sharedPreferences: SharedPreferences,
    private val key: String,
    private val defValue: Boolean
) : LiveData<Boolean>() {

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (key == changedKey) {
            value = sharedPreferences.getBoolean(key, defValue)
        }
    }

    override fun onActive() {
        super.onActive()
        value = sharedPreferences.getBoolean(key, defValue)
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}