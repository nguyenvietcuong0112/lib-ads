package com.mobi.libraryads.commons.adjust

import android.util.Log
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdapterResponseInfo
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.mobi.libraryads.commons.tracking.AdTrackingManager
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

fun trackingRevenueAd(adFull: InterstitialAd) {
    adFull.onPaidEventListener = OnPaidEventListener { adValue ->
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val adSourceName = try {
            adFull.responseInfo.adapterResponses.firstOrNull()?.adSourceName ?: ""
        } catch (_: Exception) { "" }

        Log.d(
            "tracking_rewarded",
            "tracking InterstitialAd: valueMicros: $valueMicros, currencyCode: $currencyCode, source: $adSourceName"
        )

        AdTrackingManager.trackAdRevenue(
            AdRevenueData(
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                adSourceName = adSourceName,
                adFormat = "Interstitial",
                precisionType = adValue.precisionType
            )
        )
    }
}

fun trackingRevenueAd(ad: RewardedAd) {
    ad.onPaidEventListener = OnPaidEventListener { adValue ->
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val adSourceName = try {
            ad.responseInfo.adapterResponses.firstOrNull()?.adSourceName ?: ""
        } catch (_: Exception) { "" }

        Log.d(
            "tracking_rewarded",
            "tracking RewardAds: valueMicros: $valueMicros, currencyCode: $currencyCode, source: $adSourceName"
        )

        AdTrackingManager.trackAdRevenue(
            AdRevenueData(
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                adSourceName = adSourceName,
                adFormat = "Rewarded",
                precisionType = adValue.precisionType
            )
        )
    }
}

fun trackingRevenueAd(ad: NativeAd) {
    ad.setOnPaidEventListener { values ->
        val valueMicros = values.valueMicros
        val currencyCode = values.currencyCode
        val adSourceName = try {
            ad.responseInfo?.adapterResponses?.firstOrNull()?.adSourceName ?: ""
        } catch (_: Exception) { "" }

        Log.d(
            "tracking_rewarded",
            "tracking Native: valueMicros: $valueMicros, currencyCode: $currencyCode, source: $adSourceName"
        )

        AdTrackingManager.trackAdRevenue(
            AdRevenueData(
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                adSourceName = adSourceName,
                adFormat = "Native",
                precisionType = values.precisionType
            )
        )
    }
}

fun trackingRevenueAd(ad: AdView) {
    ad.setOnPaidEventListener { values ->
        val valueMicros = values.valueMicros
        val currencyCode = values.currencyCode
        val adSourceName = try {
            ad.responseInfo?.adapterResponses?.firstOrNull()?.adSourceName ?: ""
        } catch (_: Exception) { "" }

        Log.d(
            "tracking_rewarded",
            "tracking banner: valueMicros: $valueMicros, currencyCode: $currencyCode, source: $adSourceName"
        )

        AdTrackingManager.trackAdRevenue(
            AdRevenueData(
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                adSourceName = adSourceName,
                adFormat = "Banner",
                precisionType = values.precisionType
            )
        )
    }
}

fun trackingRevenueAd(ad: AppOpenAd) {
    ad.onPaidEventListener = OnPaidEventListener { adValue ->
        val valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val adSourceName = try {
            ad.responseInfo.adapterResponses.firstOrNull()?.adSourceName ?: ""
        } catch (_: Exception) { "" }

        Log.d(
            "tracking_rewarded",
            "tracking OpenApp: valueMicros: $valueMicros, currencyCode: $currencyCode, source: $adSourceName"
        )

        AdTrackingManager.trackAdRevenue(
            AdRevenueData(
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                adSourceName = adSourceName,
                adFormat = "AppOpen",
                precisionType = adValue.precisionType
            )
        )
    }
}

fun trackingEvent(tokenEvent: String?) {
    if (tokenEvent == null) return
    AdTrackingManager.trackEvent(tokenEvent)
}

/** Tương thích ngược với các dự án cũ trực tiếp gọi hàm này */
fun onTrackingAdjustOfAdmob(
    valueMicros: Long,
    currencyCode: String,
    loadedAdapterResponseInfo: AdapterResponseInfo
) {
    AdTrackingManager.trackAdRevenue(
        AdRevenueData(
            valueMicros = valueMicros,
            currencyCode = currencyCode,
            adSourceName = loadedAdapterResponseInfo.adSourceName,
            adFormat = "AdMob"
        )
    )
}

