package com.mobi.libraryads.ads.native_ads.repository

import android.widget.FrameLayout
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.native_ads.model.AdLoadState
import com.mobi.libraryads.ads.native_ads.model.NativeAdEntry
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import com.mobi.libraryads.commons.utils.AdsLog

class NativeAdRepository {
    private val entries = ConcurrentHashMap<String, NativeAdEntry>()
    private val pendingShows = ConcurrentHashMap<String, PendingShowInfo>()

    /** Lấy entry theo name, null nếu chưa có */
    fun get(name: String): NativeAdEntry? = entries[name]

    /** Lấy hoặc tạo mới entry */
    fun getOrCreate(name: String, factory: () -> NativeAdEntry): NativeAdEntry {
        return entries.computeIfAbsent(name) { factory() }
    }

    /** Kiểm tra ad đã ready (loaded & có nativeAd hoặc activeAd) */
    fun isAdReady(name: String): Boolean {
        val entry = entries[name] ?: return false
        return (entry.state == AdLoadState.LOADED && entry.nativeAd != null) || entry.activeAd != null
    }

    /** Kiểm tra ad đã load xong (không phải LOADING) */
    fun isLoadDone(name: String): Boolean {
        val entry = entries[name] ?: return true
        return entry.state != AdLoadState.LOADING
    }

    /** Kiểm tra ad đang load */
    fun isLoading(name: String): Boolean {
        return entries[name]?.state == AdLoadState.LOADING
    }

    /** Kiểm tra ad load thất bại */
    fun isAdFailed(name: String): Boolean {
        return entries[name]?.state == AdLoadState.ERROR
    }

    /** Consume ad (lấy nativeAd ra và reset state, giữ lại trong activeAd) */
    fun consumeAd(name: String): NativeAd? {
        val entry = entries[name] ?: return null
        if (entry.nativeAd != null) {
            entry.activeAd?.destroy()
            entry.activeAd = entry.nativeAd
            entry.isHighFloorActive = entry.isHighFloorLoaded
            entry.nativeAd = null
            entry.isHighFloorLoaded = false
            entry.state = AdLoadState.NOT_LOADED
        }
        return entry.activeAd
    }

    /** Consume ad mà không lưu và hủy activeAd */
    fun consumeAdWithoutActive(name: String): NativeAd? {
        val entry = entries[name] ?: return null
        val ad = entry.nativeAd
        if (ad != null) {
            entry.nativeAd = null
            entry.isHighFloorLoaded = false
            entry.state = AdLoadState.NOT_LOADED
        }
        return ad
    }

    /** Cập nhật state */
    fun updateState(name: String, state: AdLoadState, nativeAd: NativeAd? = null, adapterClass: String = "", isHighFloor: Boolean = false) {
        val entry = entries[name] ?: return
        entry.state = state
        if (nativeAd != null) {
            entry.nativeAd?.destroy()
            entry.nativeAd = nativeAd
            entry.adapterClassName = adapterClass
            entry.isHighFloorLoaded = isHighFloor
        } else if (state == AdLoadState.ERROR || state == AdLoadState.NOT_LOADED) {
            entry.nativeAd?.destroy()
            entry.nativeAd = null
            entry.adapterClassName = ""
            entry.isHighFloorLoaded = false
        }
    }

    /** Lưu pending show info */
    fun setPendingShow(
        name: String,
        frame: FrameLayout,
        layoutRes: Int,
        isPreload: Boolean,
        callback: INativeAdCallback?
    ) {
        pendingShows[name] = PendingShowInfo(
            frame = WeakReference(frame),
            layoutRes = layoutRes,
            isPreload = isPreload,
            callback = callback
        )
    }

    /** Lấy và clear pending show info */
    fun consumePendingShow(name: String): PendingShowInfo? {
        return pendingShows.remove(name)
    }

    /** Clear active ad if it matches the current active instance */
    fun clearActiveAdIfMatch(name: String, ad: NativeAd) {
        val entry = entries[name] ?: return
        if (entry.activeAd === ad) {
            entry.activeAd?.destroy()
            entry.activeAd = null
            entry.isHighFloorActive = false
            AdsLog.d("NativeAdRepository", "clearActiveAdIfMatch: Cleared active ad for $name")
        }
    }

    /** Xóa tất cả ad và destroy NativeAd instances */
    fun clear() {
        for (entry in entries.values) {
            entry.nativeAd?.destroy()
            entry.nativeAd = null
            entry.activeAd?.destroy()
            entry.activeAd = null
            entry.state = AdLoadState.NOT_LOADED
            entry.isHighFloorLoaded = false
            entry.isHighFloorActive = false
        }
        entries.clear()
        pendingShows.clear()
    }
}

data class PendingShowInfo(
    val frame: WeakReference<FrameLayout>,
    val layoutRes: Int,
    val isPreload: Boolean,
    val callback: INativeAdCallback?
)
