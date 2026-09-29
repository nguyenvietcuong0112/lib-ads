package com.mobi.libraryads.commons.tracking

import android.app.Activity
import android.app.Application
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

/**
 * Interface chuẩn cho mọi nền tảng tracking doanh thu và sự kiện (Firebase, Adjust, AppsFlyer, Custom...)
 */
interface IAdTracker {
    /** Tên định danh của tracker */
    val trackerName: String

    /** Khởi tạo tracker khi Application được tạo */
    fun init(application: Application, isDebug: Boolean) {}

    /** Bắn tracking doanh thu khi có impression paid event từ AdMob */
    fun trackAdRevenue(data: AdRevenueData) {}

    /** Bắn tracking sự kiện tùy biến */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {}

    /** Xử lý lifecycle khi Activity Resume */
    fun onActivityResume(activity: Activity) {}

    /** Xử lý lifecycle khi Activity Pause */
    fun onActivityPause(activity: Activity) {}
}
