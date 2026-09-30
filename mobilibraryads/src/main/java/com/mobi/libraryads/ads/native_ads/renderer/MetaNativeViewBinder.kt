package com.mobi.libraryads.ads.native_ads.renderer

import android.os.Bundle
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.utils.AdsLog
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class MetaNativeViewBinder : INativeViewBinder {
    private val TAG = "MetaNativeViewBinder"

    override fun canHandle(adapterClassName: String): Boolean {
        return adapterClassName.contains("facebook", ignoreCase = true) ||
               adapterClassName.contains("meta", ignoreCase = true)
    }

    override fun bind(adView: NativeAdView, ad: NativeAd) {
        // Bind using the default binding logic first
        DefaultNativeViewBinder().bind(adView, ad)

        // Additional Meta-specific log tracking
        try {
            val adapterClass = ad.getResponseInfo().adapterClassName ?: ""
            val bundle = Bundle().apply {
                putString("ad_network", "meta")
                putString("adapter_class", adapterClass)
            }
            "native_meta_shown".postFirebaseEvent(bundle)
            AdsLog.d(TAG, "🟦 Meta native ad displayed! : adapter : $adapterClass")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
