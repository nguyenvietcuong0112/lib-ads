package com.mobi.libraryads.commons.adjust

import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.mobi.libraryads.commons.tracking.AdTrackingManager
import com.mobi.libraryads.commons.tracking.model.AdRevenueData

fun trackingRevenueAd(adFull: InterstitialAd, adValue: AdValue) {
    val valueMicros = adValue.valueMicros
    val currencyCode = adValue.currencyCode
    val adSourceName = try {
        adFull.getResponseInfo().loadedAdSourceResponseInfo?.name ?: ""
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
            precisionType = adValue.precisionType.ordinal
        )
    )
}

fun trackingRevenueAd(ad: RewardedAd, adValue: AdValue) {
    val valueMicros = adValue.valueMicros
    val currencyCode = adValue.currencyCode
    val adSourceName = try {
        ad.getResponseInfo().loadedAdSourceResponseInfo?.name ?: ""
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
            precisionType = adValue.precisionType.ordinal
        )
    )
}

fun trackingRevenueAd(ad: NativeAd) {
    ad.adEventCallback = object : NativeAdEventCallback {
        override fun onAdPaid(value: AdValue) {
            trackingRevenueAd(ad, value)
        }
    }
}

fun trackingRevenueAd(ad: NativeAd, values: AdValue) {
    val valueMicros = values.valueMicros
    val currencyCode = values.currencyCode
    val adSourceName = try {
        ad.getResponseInfo().loadedAdSourceResponseInfo?.name ?: ""
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
            precisionType = values.precisionType.ordinal
        )
    )
}

fun trackingRevenueAd(ad: BannerAd, values: AdValue) {
    val valueMicros = values.valueMicros
    val currencyCode = values.currencyCode
    val adSourceName = try {
        ad.getResponseInfo().loadedAdSourceResponseInfo?.name ?: ""
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
            precisionType = values.precisionType.ordinal
        )
    )
}

fun trackingRevenueAd(ad: AppOpenAd, adValue: AdValue) {
    val valueMicros = adValue.valueMicros
    val currencyCode = adValue.currencyCode
    val adSourceName = try {
        ad.getResponseInfo().loadedAdSourceResponseInfo?.name ?: ""
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
            precisionType = adValue.precisionType.ordinal
        )
    )
}

fun trackingEvent(tokenEvent: String?) {
    if (tokenEvent == null) return
    AdTrackingManager.trackEvent(tokenEvent)
}

/** Tương thích ngược với các dự án cũ trực tiếp gọi hàm này */
fun onTrackingAdjustOfAdmob(
    valueMicros: Long,
    currencyCode: String,
    loadedAdapterResponseInfo: AdSourceResponseInfo?
) {
    AdTrackingManager.trackAdRevenue(
        AdRevenueData(
            valueMicros = valueMicros,
            currencyCode = currencyCode,
            adSourceName = loadedAdapterResponseInfo?.name ?: "",
            adFormat = "AdMob"
        )
    )
}
