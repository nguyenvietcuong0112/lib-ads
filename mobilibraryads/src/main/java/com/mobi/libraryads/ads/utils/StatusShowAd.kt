package com.mobi.libraryads.ads.utils

object StatusShowAd {

    var isInterstitialShown = false
    var isRewardAdsShown = false
    var isOpenAdShown = false
    var ignoreAOA = false

    /**
     * Kiểm tra xem có thể hiển thị quảng cáo liên màn hình (Interstitial) hay không
     */
    fun canShowInterstitialAd(): Boolean {
        return !this.isOpenAdShown && !this.isRewardAdsShown && !this.isInterstitialShown
    }

    /**
     * Kiểm tra xem có thể hiển thị quảng cáo thưởng (Reward Ads) hay không
     */
    fun canShowRewardAd(): Boolean {
        return !this.isOpenAdShown && !this.isRewardAdsShown && !this.isInterstitialShown
    }

    /**
     * Kiểm tra xem có thể hiển thị quảng cáo mở ứng dụng (Open Ads) hay không
     */
    fun canShowOpenAd(): Boolean {
        return !this.isInterstitialShown && !this.isRewardAdsShown && !this.isOpenAdShown && !ignoreAOA
    }

    /**
     * Đặt lại trạng thái quảng cáo về mặc định
     */
    fun resetAdStatuses() {
        this.isInterstitialShown = false
        this.isRewardAdsShown = false
        this.isOpenAdShown = false
        this.ignoreAOA = false
    }
}