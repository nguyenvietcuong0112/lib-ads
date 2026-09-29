package com.mobi.libraryads.commons.tracking.impl

import android.app.Application
import android.os.Bundle
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.tracking.IAdTracker
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

/**
 * Implementation tracking doanh thu và sự kiện qua Firebase Analytics.
 * Tích hợp tự động kiểm tra Organic User thông qua Google Play Install Referrer API
 * khi ứng dụng không sử dụng các MMP bên thứ 3 (Adjust, AppsFlyer).
 */
class FirebaseAdTracker : IAdTracker {
    override val trackerName: String = "Firebase"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun init(application: Application, isDebug: Boolean) {
        try {
            firebaseAnalytics = Firebase.analytics
            Log.d("FirebaseAdTracker", "FirebaseAdTracker initialized")
        } catch (e: Exception) {
            Log.e("FirebaseAdTracker", "Failed to initialize FirebaseAnalytics", e)
        }

        // Tự động kiểm tra nguồn cài đặt Organic/Paid từ Google Play Install Referrer
        checkOrganicFromInstallReferrer(application)
    }

    private fun checkOrganicFromInstallReferrer(application: Application) {
        val spf = SPF(application)
        // Nếu đã từng xác định trạng thái nguồn user trước đó, giữ nguyên trạng thái
        if (spf.is_tracked_organic) {
            FOConfigs.isOrganic = spf.is_organic
            Log.d("FirebaseAdTracker", "Organic state already cached: isOrganic = ${spf.is_organic}")
            return
        }

        try {
            val referrerClient = InstallReferrerClient.newBuilder(application).build()
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                val response: ReferrerDetails = referrerClient.installReferrer
                                val referrerUrl = response.installReferrer ?: ""
                                Log.d("FirebaseAdTracker", "Google Play Install Referrer URL: $referrerUrl")

                                val isOrganic = isOrganicReferrer(referrerUrl)
                                FOConfigs.isOrganic = isOrganic
                                spf.is_organic = isOrganic
                                spf.is_tracked_organic = true

                                if (isOrganic) {
                                    val bundle = Bundle().apply {
                                        putLong("session_count", spf.count_session_app)
                                    }
                                    "user_organic".postFirebaseEvent(bundle)
                                }
                                firebaseAnalytics?.setUserProperty(
                                    "traffic_channel",
                                    if (isOrganic) "organic" else "paid"
                                )
                                Log.d(
                                    "FirebaseAdTracker",
                                    "Resolved organic status: isOrganic = $isOrganic (referrer: '$referrerUrl')"
                                )
                            } catch (e: Exception) {
                                Log.e("FirebaseAdTracker", "Error reading referrer details", e)
                            } finally {
                                try {
                                    referrerClient.endConnection()
                                } catch (_: Exception) {}
                            }
                        }
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE,
                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR -> {
                            Log.w("FirebaseAdTracker", "InstallReferrer responseCode: $responseCode")
                            // Khi không kết nối được Play Store (ví dụ: sideload APK, emulator), mặc định coi là organic
                            if (!spf.is_tracked_organic) {
                                val isOrganic = true
                                FOConfigs.isOrganic = isOrganic
                                spf.is_organic = isOrganic
                                spf.is_tracked_organic = true
                                if (isOrganic) {
                                    val bundle = Bundle().apply {
                                        putLong("session_count", spf.count_session_app)
                                    }
                                    "user_organic".postFirebaseEvent(bundle)
                                }
                                firebaseAnalytics?.setUserProperty("traffic_channel", "organic")
                            }
                            try {
                                referrerClient.endConnection()
                            } catch (_: Exception) {}
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {
                    Log.d("FirebaseAdTracker", "InstallReferrer service disconnected")
                }
            })
        } catch (e: Throwable) {
            Log.e("FirebaseAdTracker", "Failed to start InstallReferrerClient", e)
        }
    }

    /**
     * Phân loại organic dựa trên URL referrer từ Google Play Store.
     * Google Ads (UAC, Search, Display) sẽ đính kèm 'gclid' hoặc các tham số trả phí.
     */
    private fun isOrganicReferrer(referrerUrl: String): Boolean {
        if (referrerUrl.isBlank()) {
            return true
        }
        val lower = referrerUrl.lowercase()

        // 1. Dấu hiệu chắc chắn của quảng cáo trả phí (Google Ads / Third-party Ads)
        if (lower.contains("gclid") ||
            lower.contains("gbraid") ||
            lower.contains("wbraid") ||
            lower.contains("utm_medium=cpc") ||
            lower.contains("utm_medium=cpm") ||
            lower.contains("utm_medium=paid") ||
            lower.contains("utm_medium=ad") ||
            lower.contains("fb_clickid") ||
            lower.contains("ttclid") ||
            lower.contains("adjust_reftag") ||
            lower.contains("af_tranid")
        ) {
            return false
        }

        // 2. Dấu hiệu xác định organic từ Google Play
        if (lower.contains("utm_medium=organic")) {
            return true
        }

        // 3. Nếu có utm_campaign cụ thể (khác rỗng và không phải organic)
        if (lower.contains("utm_campaign=") && !lower.contains("utm_campaign=organic")) {
            return false
        }

        // Mặc định cài từ Google Play hoặc direct mà không có tracking ads -> Organic
        return true
    }

    override fun trackAdRevenue(data: AdRevenueData) {
        try {
            val analytics = firebaseAnalytics ?: Firebase.analytics
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob")
                putString(FirebaseAnalytics.Param.AD_SOURCE, data.adSourceName)
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, data.adUnitId)
                putString(FirebaseAnalytics.Param.AD_FORMAT, data.adFormat)
                putDouble(FirebaseAnalytics.Param.VALUE, data.revenue)
                putString(FirebaseAnalytics.Param.CURRENCY, data.currencyCode)
            }
            analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle)
            Log.d(
                "FirebaseAdTracker",
                "Logged ad_impression: ${data.revenue} ${data.currencyCode} (${data.adFormat} / ${data.adSourceName})"
            )
        } catch (e: Exception) {
            Log.e("FirebaseAdTracker", "Failed to log ad_impression to Firebase", e)
        }
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>?) {
        try {
            val analytics = firebaseAnalytics ?: Firebase.analytics
            val bundle = Bundle()
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    else -> bundle.putString(key, value.toString())
                }
            }
            val safeEventName = eventName
                .replace(".", "_")
                .replace(Regex("[^a-zA-Z0-9_]"), "")
                .take(40)
            analytics.logEvent(safeEventName, bundle)
        } catch (e: Exception) {
            Log.e("FirebaseAdTracker", "Failed to log event $eventName to Firebase", e)
        }
    }
}
