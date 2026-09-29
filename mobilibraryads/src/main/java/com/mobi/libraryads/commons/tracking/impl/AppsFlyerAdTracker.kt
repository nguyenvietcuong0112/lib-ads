package com.mobi.libraryads.commons.tracking.impl

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.appsflyer.AFLogger
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.tracking.IAdTracker
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

/**
 * Implementation tracking doanh thu và sự kiện qua AppsFlyer SDK.
 * Bọc an toàn, chỉ chạy khi dự án có cung cấp devKey hợp lệ và có chứa SDK AppsFlyer.
 */
class AppsFlyerAdTracker(
    private val devKey: String
) : IAdTracker {
    override val trackerName: String = "AppsFlyer"

    private var isInitialized = false
    private var appContext: Context? = null

    companion object {
        /** Kiểm tra SDK AppsFlyer có mặt trong classpath của ứng dụng hay không */
        val isSdkAvailable: Boolean by lazy {
            try {
                Class.forName("com.appsflyer.AppsFlyerLib")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    override fun init(application: Application, isDebug: Boolean) {
        if (!isSdkAvailable) {
            Log.w("AppsFlyerAdTracker", "AppsFlyer SDK is not found in classpath. Skipping AppsFlyer initialization.")
            return
        }
        if (devKey.isBlank()) {
            Log.w("AppsFlyerAdTracker", "AppsFlyer devKey is blank. Skipping AppsFlyer initialization.")
            return
        }

        try {
            appContext = application.applicationContext
            val appsflyer = AppsFlyerLib.getInstance()

            if (isDebug) {
                appsflyer.setLogLevel(AFLogger.LogLevel.VERBOSE)
            }

            val conversionListener = object : AppsFlyerConversionListener {
                override fun onConversionDataSuccess(conversionData: MutableMap<String, Any>?) {
                    val status = conversionData?.get("af_status")?.toString() ?: ""
                    val mediaSource = conversionData?.get("media_source")?.toString() ?: ""
                    Log.d("AppsFlyerAdTracker", "Conversion data: status = $status, mediaSource = $mediaSource")
                    val isOrganic = status.equals("Organic", ignoreCase = true) ||
                            mediaSource.contains("organic", ignoreCase = true)
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

                override fun onConversionDataFail(errorMessage: String?) {
                    Log.e("AppsFlyerAdTracker", "onConversionDataFail: $errorMessage")
                }

                override fun onAppOpenAttribution(attributionData: MutableMap<String, String>?) {}

                override fun onAttributionFailure(errorMessage: String?) {}
            }

            appsflyer.init(devKey, conversionListener, application)
            appsflyer.start(application)
            isInitialized = true
            Log.d("AppsFlyerAdTracker", "AppsFlyer SDK initialized successfully")
        } catch (e: Throwable) {
            Log.e("AppsFlyerAdTracker", "Failed to initialize AppsFlyer SDK", e)
        }
    }

    override fun trackAdRevenue(data: AdRevenueData) {
        if (!isSdkAvailable || !isInitialized) return
        val context = appContext ?: return
        try {
            val eventValues = HashMap<String, Any>()
            eventValues["af_revenue"] = data.revenue
            eventValues["af_currency"] = data.currencyCode
            eventValues["af_ad_network"] = data.adSourceName
            eventValues["af_ad_unit_id"] = data.adUnitId
            eventValues["af_ad_format"] = data.adFormat

            AppsFlyerLib.getInstance().logEvent(context, "af_ad_revenue", eventValues)
            Log.d(
                "AppsFlyerAdTracker",
                "Tracked AppsFlyer ad revenue: ${data.revenue} ${data.currencyCode} (${data.adSourceName})"
            )
        } catch (e: Throwable) {
            Log.e("AppsFlyerAdTracker", "Failed to track AppsFlyer ad revenue", e)
        }
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>?) {
        if (!isSdkAvailable || !isInitialized) return
        val context = appContext ?: return
        try {
            val eventValues = params?.let { HashMap(it) } ?: HashMap()
            AppsFlyerLib.getInstance().logEvent(context, eventName, eventValues)
        } catch (e: Throwable) {
            Log.e("AppsFlyerAdTracker", "Failed to track AppsFlyer event: $eventName", e)
        }
    }
}
