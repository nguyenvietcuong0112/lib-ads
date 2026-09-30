package com.mobi.libraryads.ads.native_ads.renderer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.utils.ShimmerHelper
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class NativeAdRenderer(
    private val viewBinders: List<INativeViewBinder>
) {
    private val defaultBinder = DefaultNativeViewBinder()

    /**
     * Render ad vào FrameLayout
     * @param adFrame container
     * @param nativeAd ad đã load
     * @param layoutRes layout XML (0 = dùng layout có sẵn trong adFrame)
     * @param adapterClassName tên adapter để chọn binder phù hợp
     */
    fun render(
        adFrame: FrameLayout,
        nativeAd: NativeAd,
        layoutRes: Int = 0,
        adapterClassName: String = ""
    ) {
        AdsLog.d("NativeAdLoader", "render: Rendering native ad (layoutRes = $layoutRes, adapter = $adapterClassName, instanceId = ${System.identityHashCode(nativeAd)})")
        adFrame.setVisible()

        // Ẩn shimmer (cả shimmer được include ở adFrame lẫn legacy shimmer_view)
        ShimmerHelper.hideShimmer(adFrame)

        // Nếu có layoutRes mới, dọn dẹp các ad view cũ trong container và inflate layoutRes mới
        if (layoutRes != 0) {
            for (i in adFrame.childCount - 1 downTo 0) {
                val child = adFrame.getChildAt(i)
                if (ShimmerHelper.isAdView(child)) {
                    adFrame.removeViewAt(i)
                }
            }
            val adView = LayoutInflater.from(adFrame.context).inflate(layoutRes, adFrame, false)
            adFrame.addView(adView)
        }

        val nativeAdView = adFrame.findViewById<NativeAdView>(R.id.native_ad_view)
        if (nativeAdView == null) {
            AdsLog.e("NativeAdLoader", "render: Không tìm thấy native_ad_view trong adFrame hoặc layoutRes ($layoutRes)")
            return
        }

        val binder = viewBinders.firstOrNull { it.canHandle(adapterClassName) } ?: defaultBinder
        binder.bind(nativeAdView, nativeAd)

        adFrame.findViewById<View>(R.id.btn_close_ad)?.apply {
            setVisible()
            setOnClickListener {
                hideLoading(adFrame)
            }
        }

        nativeAdView.setVisible()
        adFrame.findViewById<ViewGroup>(R.id.ad_content_view)?.setVisible()
    }

    /**
     * Hiển thị shimmer loading cho Native Ad.
     * Bảo toàn view shimmer nếu client đã include sẵn trong adFrame.
     */
    fun showLoading(adFrame: FrameLayout, layoutRes: Int = 0) {
        adFrame.setVisible()
        val shimmerViews = ShimmerHelper.getShimmerViews(adFrame)
        if (shimmerViews.isEmpty()) {
            // Client chưa include shimmer trong adFrame
            if (layoutRes != 0) {
                val adView = LayoutInflater.from(adFrame.context).inflate(layoutRes, adFrame, false)
                adFrame.addView(adView)
                val legacyShimmer = adView.findViewById<View>(R.id.shimmer_view)
                if (legacyShimmer == null) {
                    // Layout ad không có sẵn shimmer_view -> inflate fallback shimmer
                    val fallback = LayoutInflater.from(adFrame.context)
                        .inflate(R.layout.layout_shimmer_load_ads_native_banner, adFrame, false)
                    adFrame.addView(fallback)
                }
            } else {
                val fallback = LayoutInflater.from(adFrame.context)
                    .inflate(R.layout.layout_shimmer_load_ads_native_banner, adFrame, false)
                adFrame.addView(fallback)
            }
        }
        ShimmerHelper.showShimmer(adFrame)
        adFrame.findViewById<View>(R.id.btn_close_ad)?.setGone()
    }

    /**
     * Dọn dẹp shimmer và ẩn hoàn toàn adFrame khi quảng cáo lỗi hoặc điều kiện hiển thị không đạt
     */
    fun hideLoading(adFrame: FrameLayout) {
        ShimmerHelper.hideAllAndGone(adFrame)
    }
}
