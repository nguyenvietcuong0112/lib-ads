package com.mobi.libraryads.commons.tracking.impl

import android.app.Activity
import android.app.Application
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.tracking.IAdTracker
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

/**
 * Implementation tracking doanh thu và sự kiện qua Adjust SDK.
 * Bọc an toàn, chỉ chạy khi dự án có cung cấp appToken hợp lệ và có chứa SDK Adjust.
 */
class AdjustAdTracker(
    private val appToken: String
) : IAdTracker {
    override val trackerName: String = "Adjust"

    private var isInitialized = false

    companion object {
        /** Kiểm tra SDK Adjust có mặt trong classpath của ứng dụng hay không */
        val isSdkAvailable: Boolean by lazy {
            try {
                Class.forName("com.adjust.sdk.Adjust")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    override fun init(application: Application, isDebug: Boolean) {
        if (!isSdkAvailable) {
            Log.w("AdjustAdTracker", "Adjust SDK is not found in classpath. Skipping Adjust initialization.")
            return
        }
        if (appToken.isBlank()) {
            Log.w("AdjustAdTracker", "Adjust appToken is blank. Skipping Adjust initialization.")
            return
        }

        try {
            val environment = if (isDebug) {
                AdjustConfig.ENVIRONMENT_SANDBOX
            } else {
                AdjustConfig.ENVIRONMENT_PRODUCTION
            }
            val adjustConfig = AdjustConfig(application, appToken, environment)
            adjustConfig.setLogLevel(if (isDebug) LogLevel.VERBOSE else LogLevel.INFO)

            adjustConfig.setOnAttributionChangedListener { attribution ->
                val network = attribution.network
                Log.d("AdjustAdTracker", "Adjust attribution network: $network")
                val isOrganic = network?.lowercase()?.contains("organic") ?: false
                FOConfigs.isOrganic = isOrganic
                val spf = SPF(application)
                spf.is_organic = isOrganic
                spf.is_tracked_organic = true
                if (isOrganic) {
                    val bundle = android.os.Bundle().apply {
                        putLong("session_count", spf.count_session_app)
                    }
                    "user_organic".postFirebaseEvent(bundle)
                }
            }

            Adjust.initSdk(adjustConfig)
            isInitialized = true
            Log.d("AdjustAdTracker", "Adjust SDK initialized successfully with environment: $environment")
        } catch (e: Throwable) {
            Log.e("AdjustAdTracker", "Failed to initialize Adjust SDK", e)
        }
    }

    override fun trackAdRevenue(data: AdRevenueData) {
        if (!isSdkAvailable || !isInitialized) return
        try {
            val adRevenue = AdjustAdRevenue("admob_sdk")
            adRevenue.setRevenue(data.revenue, data.currencyCode)
            adRevenue.adRevenueNetwork = data.adSourceName
            Adjust.trackAdRevenue(adRevenue)
            Log.d(
                "AdjustAdTracker",
                "Tracked Adjust ad revenue: ${data.revenue} ${data.currencyCode} (${data.adSourceName})"
            )
        } catch (e: Throwable) {
            Log.e("AdjustAdTracker", "Failed to track Adjust ad revenue", e)
        }
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>?) {
        if (!isSdkAvailable || !isInitialized) return
        try {
            val token = params?.get("token") as? String ?: eventName
            val adjustEvent = AdjustEvent(token)
            params?.forEach { (key, value) ->
                if (key != "token") {
                    adjustEvent.addCallbackParameter(key, value.toString())
                }
            }
            Adjust.trackEvent(adjustEvent)
        } catch (e: Throwable) {
            Log.e("AdjustAdTracker", "Failed to track Adjust event: $eventName", e)
        }
    }

    override fun onActivityResume(activity: Activity) {
        if (!isSdkAvailable || !isInitialized) return
        try {
            Adjust.onResume()
        } catch (e: Throwable) {
            Log.e("AdjustAdTracker", "Error in Adjust.onResume()", e)
        }
    }

    override fun onActivityPause(activity: Activity) {
        if (!isSdkAvailable || !isInitialized) return
        try {
            Adjust.onPause()
        } catch (e: Throwable) {
            Log.e("AdjustAdTracker", "Error in Adjust.onPause()", e)
        }
    }
}
