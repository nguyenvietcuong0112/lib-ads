package com.mobi.libraryads.ads.native_ads.renderer

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

interface INativeViewBinder {
    /** Kiểm tra binder này có hỗ trợ adapter/mediation đó không */
    fun canHandle(adapterClassName: String): Boolean

    /** Bind NativeAd vào NativeAdView */
    fun bind(adView: NativeAdView, ad: NativeAd)
}
