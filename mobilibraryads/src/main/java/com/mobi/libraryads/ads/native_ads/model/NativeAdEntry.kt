package com.mobi.libraryads.ads.native_ads.model

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

data class NativeAdEntry(
    val name: String,                           // Tên định danh duy nhất (vd: "home_native", "language_native")
    var id: String,                             // ID ad chính (all price / normal)
    var idHigh: String = "",                    // ID ad high price floor (optional, chỉ language cần)
    var strategy: LoadStrategy = LoadStrategy.SEQUENTIAL, // Chiến lược load high floor (Sequential/Parallel)
    
    // --- Trạng thái (internal) ---
    var state: AdLoadState = AdLoadState.NOT_LOADED,
    var nativeAd: NativeAd? = null,             // Ad đã load
    var activeAd: NativeAd? = null,             // Ad đang hoạt động/hiển thị
    var adapterClassName: String = "",          // Tên adapter mediation (để xử lý Meta)
    var isHighFloorLoaded: Boolean = false,     // Ad hiện tại đã load có phải high floor ko
    var isHighFloorActive: Boolean = false      // Ad hiện tại đang active/show có phải high floor ko
)
