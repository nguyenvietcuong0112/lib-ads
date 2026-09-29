package com.mobi.libraryads.ads.native_ads.callback

interface INativeAdCallback {
    /** Ad đã load thành công */
    fun onAdLoaded(adName: String) {}

    /** Ad load thất bại */
    fun onAdFailedToLoad(adName: String, errorMessage: String) {}

    /** Ad đã show thành công lên UI */
    fun onAdShown(adName: String) {}

    /** Ad đã show thành công lên UI kèm thông tin high floor */
    fun onAdShown(adName: String, isHighFloor: Boolean) {
        onAdShown(adName)
    }

    /** Ad show thất bại (không có ad, activity destroyed, v.v.) */
    fun onAdShowFailed(adName: String, reason: String) {}

    /** User click vào ad */
    fun onAdClicked(adName: String) {}

    /** Ad impression được ghi nhận */
    fun onAdImpression(adName: String) {}

    /** Ad bị dismiss (cho collapsible) */
    fun onAdDismissed(adName: String) {}
}
