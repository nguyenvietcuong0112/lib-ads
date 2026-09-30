package com.mobi.libraryads.ads.inter_ads

import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd

data class InterAdModel(
    var name: String,
    var id: String = "",
    var interAd: InterstitialAd? = null,
    var retry: Int = 0,
    var isLoading: Boolean = false
)
