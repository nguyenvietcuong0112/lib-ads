package com.mobi.libraryads.commons.sharepreference

import android.content.Context
import androidx.core.content.edit


open class SPF(context: Context) {

    protected val preferences =
        context.applicationContext.getSharedPreferences(
            context.packageName,
            Context.MODE_PRIVATE
        )

    protected fun getBoolean(
        key: String,
        default: Boolean = false
    ): Boolean {
        return preferences.getBoolean(key, default)
    }

    protected fun putBoolean(
        key: String,
        value: Boolean
    ) {
        preferences.edit {
            putBoolean(key, value)
            apply()
        }
    }

    protected fun getString(
        key: String,
        default: String = ""
    ): String {
        return preferences.getString(key, default) ?: default
    }

    protected fun putString(
        key: String,
        value: String
    ) {
        preferences.edit {
            putString(key, value)
        }
    }

    protected fun getLong(
        key: String,
        default: Long = -1L
    ): Long {
        return preferences.getLong(key, default)
    }

    protected fun putLong(
        key: String,
        value: Long
    ) {
        preferences.edit {
            putLong(key, value)
        }
    }


    var is_app_pro: Boolean
        get() = getBoolean("is_app_pro", false)
        set(value) = putBoolean("is_app_pro", value)


    var language_code_selected: String
        get() = getString("language_code_selected", "en")
        set(value) = putString("language_code_selected", value)


    var is_second_time_open_app: Boolean
        get() = getBoolean("is_second_time_open_app", false)
        set(value) = putBoolean("is_second_time_open_app", value)


    var count_session_app: Long
        get() = getLong("count_session_app", 0L)
        set(value) = putLong("count_session_app", value)


    var is_organic: Boolean
        get() = getBoolean("is_organic", false)
        set(value) = putBoolean("is_organic", value)

    var is_tracked_organic: Boolean
        get() = getBoolean("is_tracked_organic", false)
        set(value) = putBoolean("is_tracked_organic", value)

}