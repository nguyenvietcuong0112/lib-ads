package com.mobi.libraryads.commons.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

object AdsLog {

    private const val DEFAULT_TAG = "AdsLog"
    
    // Cờ trạng thái Debug. Mặc định là false cho an toàn, sẽ được cập nhật khi init.
    var isDebug: Boolean = false
        private set

    private var isInitialized = false

    fun turnOnLogcat(context: Context) {
        init(context, true)
    }

    /**
     * Khởi tạo AdsLog với Context của ứng dụng và cờ debug tùy chọn.
     */
    fun init(context: Context, isDebugEnabled: Boolean = false) {
        // Ưu tiên cờ cấu hình truyền vào hoặc tự động quét trạng thái debuggable của Host App
        val hostAppDebuggable = try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
        
        this.isDebug = isDebugEnabled || hostAppDebuggable
        
        if (this.isDebug) {
            Log.d(DEFAULT_TAG, "AdsLog initialized. Debug logging is ENABLED.")
        }
    }

    /**
     * Log Verbose
     */
    fun v(msg: String) {
        Log.v(DEFAULT_TAG, msg)
    }

    fun v(tag: String, msg: String) {
        if (!isDebug) return
        Log.v(tag, getCallerInfo() + msg)
    }

    /**
     * Log Debug
     */
    fun d(msg: String) {
        Log.d(DEFAULT_TAG, msg)
    }

    fun d(tag: String, msg: String) {
        if (!isDebug) return
        Log.d(tag, getCallerInfo() + msg)
    }

    /**
     * Log Info
     */
    fun i(msg: String) {
        Log.i(DEFAULT_TAG, msg)
    }

    fun i(tag: String, msg: String) {
        if (!isDebug) return
        Log.i(tag, getCallerInfo() + msg)
    }

    /**
     * Log Warning
     */
    fun w(msg: String, throwable: Throwable? = null) {
        w(DEFAULT_TAG, msg, throwable)
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        if (!isDebug) return
        if (throwable != null) {
            Log.w(tag, getCallerInfo() + msg, throwable)
        } else {
            Log.w(tag, getCallerInfo() + msg)
        }
    }

    /**
     * Log Error
     */
    fun e(msg: String, throwable: Throwable? = null) {
        Log.e(DEFAULT_TAG, msg, throwable)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (!isDebug) return
        if (throwable != null) {
            Log.e(tag, getCallerInfo() + msg, throwable)
        } else {
            Log.e(tag, getCallerInfo() + msg)
        }
    }

    /**
     * Tự động lấy thông tin Class, Method và dòng gọi log trong StackTrace
     */
    private fun getCallerInfo(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val index = stackTrace.indexOfFirst { it.className == AdsLog::class.java.name }
        if (index != -1) {
            for (i in index + 1 until stackTrace.size) {
                val element = stackTrace[i]
                if (element.className != AdsLog::class.java.name && 
                    !element.className.contains("java.lang.Thread")
                ) {
                    val className = element.className.substringAfterLast('.')
                    return "[$className.${element.methodName}:${element.lineNumber}] "
                }
            }
        }
        return ""
    }
}