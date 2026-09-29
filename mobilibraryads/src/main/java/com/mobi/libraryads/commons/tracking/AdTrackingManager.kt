package com.mobi.libraryads.commons.tracking

import android.app.Activity
import android.app.Application
import android.util.Log
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.commons.tracking.model.AdRevenueData
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Bộ quản lý điều phối tracking trung tâm (Composite Pattern).
 * Nhận sự kiện doanh thu từ AdMob và chuyển tiếp tới tất cả các tracker đã đăng ký
 * (Firebase, Adjust, AppsFlyer, Custom Callback, v.v.).
 */
object AdTrackingManager {
    private const val TAG = "AdTrackingManager"
    private val trackers = CopyOnWriteArrayList<IAdTracker>()

    /** Đăng ký thêm một tracker */
    fun registerTracker(tracker: IAdTracker) {
        if (!trackers.any { it.trackerName == tracker.trackerName }) {
            trackers.add(tracker)
            Log.d(TAG, "Registered tracker: ${tracker.trackerName}")
        }
    }

    /** Xóa và thiết lập toàn bộ danh sách tracker */
    fun setTrackers(newTrackers: List<IAdTracker>) {
        trackers.clear()
        trackers.addAll(newTrackers)
        Log.d(TAG, "Set trackers: ${newTrackers.map { it.trackerName }}")
    }

    /** Lấy danh sách các tracker hiện tại */
    fun getTrackers(): List<IAdTracker> = trackers.toList()

    /** Khởi tạo tất cả các tracker */
    fun init(application: Application, isDebug: Boolean) {
        trackers.forEach { tracker ->
            try {
                tracker.init(application, isDebug)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing tracker: ${tracker.trackerName}", e)
            }
        }
    }

    /** Điều phối tracking doanh thu quảng cáo */
    fun trackAdRevenue(data: AdRevenueData) {
        trackers.forEach { tracker ->
            try {
                tracker.trackAdRevenue(data)
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking ad revenue with ${tracker.trackerName}", e)
            }
        }
    }

    /** Điều phối tracking sự kiện */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        trackers.forEach { tracker ->
            try {
                tracker.trackEvent(eventName, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking event with ${tracker.trackerName}", e)
            }
        }
    }

    /** Lifecycle Resume */
    fun onActivityResume(activity: Activity) {
        trackers.forEach { tracker ->
            try {
                tracker.onActivityResume(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error onActivityResume with ${tracker.trackerName}", e)
            }
        }
    }

    /** Lifecycle Pause */
    fun onActivityPause(activity: Activity) {
        trackers.forEach { tracker ->
            try {
                tracker.onActivityPause(activity)
            } catch (e: Exception) {
                Log.e(TAG, "Error onActivityPause with ${tracker.trackerName}", e)
            }
        }
    }
}
