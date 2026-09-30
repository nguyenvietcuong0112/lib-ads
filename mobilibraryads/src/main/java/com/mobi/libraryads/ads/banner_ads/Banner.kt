package com.mobi.libraryads.ads.banner_ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mobi.libraryads.R
import com.mobi.libraryads.commons.adjust.trackingRevenueAd
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.dpToPx
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible
import com.mobi.libraryads.ads.utils.ShimmerHelper
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlin.math.pow

object Banner {

    private const val TAG = "Banner"
    private var mAdView: AdView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var retryAttempt = 0

    enum class TypeAds {
        BANNER_NORMAL,
        BANNER_ADAPTIVE,
        BANNER_250,
        BANNER_COLLAPSIBLE_BOTTOM,
        BANNER_COLLAPSIBLE_TOP,
    }

    fun requestBanner(
        activity: Activity,
        id: String = "",
        typeAds: TypeAds = TypeAds.BANNER_ADAPTIVE,
        adFrame: FrameLayout,
        canShowAd: Boolean = true,
        onResult: ((ad: AdView?) -> Unit)? = null,
        onShown: (() -> Unit)? = null
    ) {
        AdsLog.d(TAG, "requestBanner: Nhận yêu cầu tải Banner. ID = \"$id\", Loại = $typeAds")

        if (activity.isFinishing || activity.isDestroyed) {
            AdsLog.w(
                TAG,
                "requestBanner: Activity đang kết thúc hoặc đã bị hủy. Bỏ qua yêu cầu tải quảng cáo."
            )
            onResult?.invoke(null)
            return
        }

        if (id.isBlank()) {
            AdsLog.e(TAG, "requestBanner: Ad Unit ID bị trống!")
            ShimmerHelper.hideAllAndGone(adFrame)
            onResult?.invoke(null)
            return
        }

        if (!canShowAd) {
            AdsLog.e(TAG, "requestBanner: Disable remote config")
            ShimmerHelper.hideAllAndGone(adFrame)
            onResult?.invoke(null)
            return
        }

        // Tự động đăng ký Lifecycle Observer để dọn dẹp khi Activity chứa banner bị hủy (ON_DESTROY)
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        AdsLog.d(
                            TAG,
                            "LifecycleObserver: Activity $activity phát tín hiệu ON_DESTROY. Tự động dọn dẹp banner."
                        )
                        destroyBannerAds(activity)
                        activity.lifecycle.removeObserver(this)
                    }
                }
            })
        }

        // Hủy tác vụ retry đang chờ nếu có
        retryRunnable?.let {
            AdsLog.d(TAG, "requestBanner: Hủy tác vụ retry đang chờ từ lượt tải trước đó.")
            handler.removeCallbacks(it)
        }
        retryRunnable = null
        retryAttempt = 0

        // Hủy banner cũ nếu đang có
        if (mAdView != null) {
            AdsLog.d(
                TAG,
                "requestBanner: Phát hiện banner cũ đang tồn tại. Tiến hành hủy bỏ trước."
            )
            destroyBannerAds()
        }

        // Kiểm tra điều kiện (Mạng, Trạng thái mua PRO)
        val isPro = SPF(activity).is_app_pro
        val isConnected = activity.isInternetConnected()
        AdsLog.d(TAG, "requestBanner: is_app_pro = $isPro, isInternetConnected = $isConnected")

        if (!isConnected || isPro) {
            AdsLog.d(
                TAG,
                "requestBanner: Không có kết nối mạng hoặc tài khoản PRO. Ẩn adFrame và kết thúc."
            )
            ShimmerHelper.hideAllAndGone(adFrame)
            onResult?.invoke(null)
            return
        }

        // Lấy kích thước quảng cáo đã tính toán và chiều cao shimmer tương ứng
        val adSize = getAdSize(activity, typeAds)
        val shimmerHeightDp = if (adSize.height > 0) adSize.height.toFloat() else 60f
        AdsLog.d(
            TAG,
            "requestBanner: Kích thước AdSize tính toán = $adSize, Chiều cao Shimmer = ${shimmerHeightDp}dp"
        )

        // Chuẩn bị adFrame và hiển thị Shimmer
        adFrame.setVisible()

        val shimmerViews = ShimmerHelper.getShimmerViews(adFrame)
        if (shimmerViews.isEmpty()) {
            val fallbackShimmer = LayoutInflater.from(activity)
                .inflate(R.layout.layout_shimmer_load_ads_native_banner, adFrame, false).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        activity.dpToPx(shimmerHeightDp)
                    )
                }
            adFrame.addView(fallbackShimmer)
            AdsLog.d(TAG, "requestBanner: Đã inflate và thêm fallback Shimmer layout vào adFrame.")
        } else {
            AdsLog.d(TAG, "requestBanner: Đã phát hiện client-included Shimmer layout trong adFrame.")
        }
        ShimmerHelper.showShimmer(adFrame)

        // Khởi tạo AdView mới
        val adView = AdView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER
            )
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setGone() // Ẩn đi lúc đầu, chỉ hiện khi đã tải xong
        }
        mAdView = adView
        adFrame.addView(adView)
        AdsLog.d(
            TAG,
            "requestBanner: Khởi tạo AdView mới và thêm vào adFrame (trạng thái ẩn ban đầu)."
        )

        // Cấu hình BannerAdRequest
        val requestBuilder = BannerAdRequest.Builder(id, adSize)
        when (typeAds) {
            TypeAds.BANNER_COLLAPSIBLE_BOTTOM, TypeAds.BANNER_COLLAPSIBLE_TOP -> {
                val collapsibleValue =
                    if (typeAds == TypeAds.BANNER_COLLAPSIBLE_BOTTOM) "bottom" else "top"
                AdsLog.d(
                    TAG,
                    "requestBanner: Cấu hình quảng cáo Collapsible Banner hướng = $collapsibleValue"
                )
                val extras = Bundle().apply {
                    putString("collapsible", collapsibleValue)
                }
                requestBuilder.setGoogleExtrasBundle(extras)
            }

            else -> {
                AdsLog.d(TAG, "requestBanner: Cấu hình quảng cáo Banner thường.")
            }
        }
        val bannerAdRequest = requestBuilder.build()

        // Tải quảng cáo bằng AdLoadCallback<BannerAd> theo GMA Next-Gen SDK
        val adLoadCallback = object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                AdsLog.i(TAG, "Banner: onAdLoaded - Quảng cáo đã được tải thành công.")
                retryAttempt = 0

                if (activity.isFinishing || activity.isDestroyed || mAdView !== adView) {
                    AdsLog.w(
                        TAG,
                        "Banner: onAdLoaded - Activity đã kết thúc hoặc banner đã bị hủy. Bỏ qua cập nhật UI."
                    )
                    return
                }

                // Đăng ký BannerAdEventCallback
                ad.adEventCallback = object : BannerAdEventCallback {
                    override fun onAdImpression() {
                        AdsLog.i(TAG, "Banner: onAdImpression - Ghi nhận lượt hiển thị.")
                        activity.runOnUiThread {
                            onShown?.invoke()
                        }
                    }

                    override fun onAdClicked() {
                        AdsLog.i(TAG, "Banner: onAdClicked - Người dùng nhấp vào quảng cáo.")
                    }

                    override fun onAdShowedFullScreenContent() {
                        AdsLog.i(TAG, "Banner: onAdShowedFullScreenContent.")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        AdsLog.i(TAG, "Banner: onAdDismissedFullScreenContent.")
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        AdsLog.w(
                            TAG,
                            "Banner: onAdFailedToShowFullScreenContent: $fullScreenContentError"
                        )
                    }

                    override fun onAdPaid(value: AdValue) {
                        trackingRevenueAd(ad, value)
                    }
                }

                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed || mAdView !== adView) return@runOnUiThread
                    // Ẩn shimmer và hiển thị AdView
                    ShimmerHelper.hideShimmer(adFrame)
                    adView.setVisible()
                    adFrame.setVisible()
                    onResult?.invoke(adView)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                AdsLog.e(
                    TAG,
                    "Banner: onAdFailedToLoad - Lỗi tải quảng cáo: Mã lỗi = ${loadAdError.code}, Nội dung = ${loadAdError.message}"
                )

                if (activity.isFinishing || activity.isDestroyed || mAdView !== adView) {
                    AdsLog.w(
                        TAG,
                        "Banner: onAdFailedToLoad - Activity đã kết thúc hoặc banner đã bị hủy. Hủy tác vụ thử lại."
                    )
                    return
                }

                retryAttempt++
                AdsLog.d(TAG, "Banner: onAdFailedToLoad - Lượt thử lại hiện tại: $retryAttempt")

                if (retryAttempt > 2) {
                    AdsLog.e(
                        TAG,
                        "Banner: onAdFailedToLoad - Vượt quá số lần thử lại tối đa (2). Dọn dẹp giao diện và gọi callback thất bại."
                    )
                    activity.runOnUiThread {
                        ShimmerHelper.hideAllAndGone(adFrame)
                        onResult?.invoke(null)
                    }
                    return
                }

                activity.runOnUiThread {
                    ShimmerHelper.showShimmer(adFrame)
                }

                val delay = minOf(2.0.pow(retryAttempt.toDouble()).toLong(), 60) * 1000
                AdsLog.d(
                    TAG,
                    "Banner: onAdFailedToLoad - Lập lịch thử lại (retry) sau ${delay}ms"
                )

                val runnable = Runnable {
                    if (activity.isFinishing || activity.isDestroyed || mAdView !== adView) {
                        AdsLog.w(
                            TAG,
                            "Banner: Tác vụ thử lại - Activity đã bị hủy. Bỏ qua tải."
                        )
                        return@Runnable
                    }
                    AdsLog.d(
                        TAG,
                        "Banner: Tác vụ thử lại - Bắt đầu tải lại quảng cáo (Lần thử $retryAttempt)..."
                    )
                    mAdView?.loadAd(bannerAdRequest, this)
                }
                retryRunnable = runnable
                handler.postDelayed(runnable, delay)
            }
        }

        AdsLog.d(TAG, "requestBanner: Gọi loadAd() để bắt đầu tải quảng cáo từ AdMob GMA Next-Gen.")
        adView.loadAd(bannerAdRequest, adLoadCallback)
    }

    private fun getAdSize(activity: Activity, type: TypeAds): AdSize {
        val density = activity.resources.displayMetrics.density
        val adWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            (bounds.width() / density).toInt()
        } else {
            val display = activity.windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            (outMetrics.widthPixels / density).toInt()
        }

        return when (type) {
            TypeAds.BANNER_NORMAL -> AdSize.BANNER
            TypeAds.BANNER_ADAPTIVE -> AdSize.getInlineAdaptiveBannerAdSize(adWidth, 60)
            TypeAds.BANNER_250 -> AdSize.MEDIUM_RECTANGLE
            TypeAds.BANNER_COLLAPSIBLE_BOTTOM, TypeAds.BANNER_COLLAPSIBLE_TOP ->
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        }
    }

    /**
     * Tiêu hủy banner. Nếu truyền Activity vào, chỉ tiêu hủy khi banner hiện tại thuộc về Activity đó.
     */
    fun destroyBannerAds(activity: Activity? = null) {
        if (activity != null) {
            val currentActivity = mAdView?.context?.let { getActivityFromContext(it) }
            if (currentActivity != activity) {
                AdsLog.d(
                    TAG,
                    "destroyBannerAds: Banner hiện tại đang thuộc về Activity khác ($currentActivity). Không hủy banner cho $activity."
                )
                return
            }
        }

        AdsLog.d(TAG, "destroyBannerAds: Tiến hành hủy banner và giải phóng tài nguyên.")

        // Hủy bỏ tác vụ handler retry đang chờ chạy
        retryRunnable?.let {
            AdsLog.d(TAG, "destroyBannerAds: Hủy bỏ tác vụ thử lại đang xếp hàng.")
            handler.removeCallbacks(it)
        }
        retryRunnable = null
        retryAttempt = 0

        if (mAdView?.parent is ViewGroup) {
            (mAdView?.parent as ViewGroup).removeView(mAdView)
        }
        mAdView?.destroy()
        mAdView = null
    }

    /**
     * Lấy Activity thực tế từ Context (bóc tách ContextWrapper nếu có)
     */
    private fun getActivityFromContext(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }

}
