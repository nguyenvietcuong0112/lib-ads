package com.mobi.libraryads.ads.utils

import android.view.View
import android.view.ViewGroup
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.mobi.libraryads.R
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible

/**
 * Tiện ích quản lý Shimmer layout cho Banner và Native Ads.
 * Hỗ trợ nhận diện shimmer do client include trực tiếp trong adFrame hoặc fallback shimmer.
 */
object ShimmerHelper {

    /**
     * Tìm kiếm đệ quy tất cả các ShimmerFrameLayout bên trong một View/ViewGroup
     */
    fun findShimmerFrameLayouts(view: View): List<ShimmerFrameLayout> {
        val list = mutableListOf<ShimmerFrameLayout>()
        if (view is ShimmerFrameLayout) {
            list.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                list.addAll(findShimmerFrameLayouts(view.getChildAt(i)))
            }
        }
        return list
    }

    /**
     * Bắt đầu animation shimmer cho tất cả ShimmerFrameLayout trong view
     */
    fun startShimmer(view: View) {
        findShimmerFrameLayouts(view).forEach {
            it.showShimmer(true)
            it.startShimmer()
        }
    }

    /**
     * Dừng animation shimmer cho tất cả ShimmerFrameLayout trong view
     */
    fun stopShimmer(view: View) {
        findShimmerFrameLayouts(view).forEach {
            it.stopShimmer()
        }
    }

    /**
     * Kiểm tra xem một View có phải là Ad view (AdMob Banner hoặc NativeAdView) hay không
     */
    fun isAdView(view: View): Boolean {
        if (view is AdView || view is NativeAdView) return true
        if (view is ViewGroup) {
            return view.findViewById<View>(R.id.native_ad_view) != null
        }
        return false
    }

    /**
     * Lấy danh sách các child view đóng vai trò Shimmer trong container (các view không phải là AdView)
     */
    fun getShimmerViews(container: ViewGroup): List<View> {
        val shimmers = mutableListOf<View>()
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (!isAdView(child)) {
                shimmers.add(child)
            }
        }
        return shimmers
    }

    /**
     * Hiển thị shimmer và kích hoạt hiệu ứng, đồng thời ẩn các AdView hiện có (nếu có từ lần load trước)
     */
    fun showShimmer(container: ViewGroup) {
        container.setVisible()
        val shimmerViews = getShimmerViews(container)
        for (shimmer in shimmerViews) {
            shimmer.setVisible()
            startShimmer(shimmer)
        }
        // Ẩn các ad view hiện có (nếu có)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (isAdView(child)) {
                child.setGone()
            }
        }
    }

    /**
     * Ẩn shimmer và dừng hiệu ứng shimmer
     */
    fun hideShimmer(container: ViewGroup) {
        val shimmerViews = getShimmerViews(container)
        for (shimmer in shimmerViews) {
            stopShimmer(shimmer)
            shimmer.setGone()
        }
        // Xử lý legacy shimmer_view nhúng bên trong Native XML nếu có
        container.findViewById<View>(R.id.shimmer_view)?.let {
            stopShimmer(it)
            it.setGone()
        }
    }

    /**
     * Dừng shimmer, ẩn tất cả view con và ẩn hoàn toàn container
     */
    fun hideAllAndGone(container: ViewGroup) {
        hideShimmer(container)
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            child.setGone()
        }
        container.setGone()
    }
}
