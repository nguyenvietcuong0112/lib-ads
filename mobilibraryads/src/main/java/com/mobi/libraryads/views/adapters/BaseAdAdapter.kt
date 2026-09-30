package com.mobi.libraryads.views.adapters

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.setGone
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import java.util.concurrent.ConcurrentHashMap

abstract class BaseAdAdapter<T, VH : RecyclerView.ViewHolder>(
    protected open var scrollingAdLoadDelayMs: Long = 500L,
    protected open var idleAdLoadDelayMs: Long = 50L
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    companion object {
        private const val TAG = "BaseAdAdapter"
        private const val TYPE_AD = -9999
    }

    // --- QUY TẮC CHÈN QUẢNG CÁO ---
    sealed class AdPlacementRule {
        // Chèn ở các vị trí cố định (VD: [3, 7])
        class Fixed(val positions: List<Int>) : AdPlacementRule()
        // Lặp lại đều đặn (VD: bắt đầu ở 2, cách 5 phần tử chèn 1)
        class Repeating(val startPosition: Int, val interval: Int) : AdPlacementRule()
    }

    // --- BỘ NHỚ ĐỆM (CACHE) AD ĐANG HIỂN THỊ TRÊN ADAPTER ---
    private val adMap = ConcurrentHashMap<Int, NativeAd>()
    
    // Theo dõi các vị trí đang tiến hành tải để tránh tải lặp 2 lần cùng lúc (chống spam request)
    private val activeLoads = ConcurrentHashMap<Int, Boolean>()
    
    // Theo dõi các vị trí tải bị lỗi (để sau này ẩn view đi, tránh bị khung trắng)
    private val failedPositions = ConcurrentHashMap<Int, Boolean>()

    // Hàng đợi các tác vụ tải quảng cáo (sử dụng độ trễ 50ms/500ms để chặn tải khi đang cuộn nhanh)
    private val delayedTasks = ConcurrentHashMap<Int, Runnable>()

    // Đếm số lần thử lại (retry) khi tải lỗi (tối đa 1 lần)
    private val retryCountMap = ConcurrentHashMap<Int, Int>()

    private var attachedRecyclerView: RecyclerView? = null
    private val handler = Handler(Looper.getMainLooper())

    // Caching resolved ad positions to avoid redundant computations on every getItemCount/isAdPosition
    private var cachedAdPositions = listOf<Int>()
    private var lastOriginalCount = -1

    // --- HOẠT ĐỘNG BẬT/TẮT QUẢNG CÁO (REMOTE CONFIG) ---
    protected open fun canShowAd(): Boolean = true

    // Abstract methods to configure and bind ads inside subclass adapter
    protected abstract fun getAdPlacementRule(): AdPlacementRule
    protected abstract fun getAdName(position: Int): String
    protected abstract fun getAdLayoutRes(position: Int): Int
    protected open fun getIdAd(context: Context, position: Int): String = ""

    // Abstract methods for data items (delegated to subclass)
    protected abstract fun onCreateDataViewHolder(parent: ViewGroup, viewType: Int): VH
    protected abstract fun onBindDataViewHolder(holder: VH, position: Int, item: T)
    protected abstract fun getDataItemCount(): Int
    protected abstract fun getDataItem(position: Int): T
    
    protected open fun getDataItemViewType(position: Int): Int = 0

    /**
     * Cập nhật danh sách dữ liệu an toàn và tự động cuộn về vị trí 0 nếu cần (Dành cho Lọc / Search / Reset).
     * @param scrollToTop Nếu true, cuộn danh sách về vị trí 0 sau khi cập nhật.
     */
    fun notifyDataChanged(scrollToTop: Boolean = false) {
        handleListSizeChanged()
        val rv = attachedRecyclerView
        if (rv?.isComputingLayout == true) {
            rv.post {
                notifyDataSetChanged()
                if (scrollToTop) rv.scrollToPosition(0)
            }
        } else {
            notifyDataSetChanged()
            if (scrollToTop) rv?.scrollToPosition(0)
        }
    }

    // --- BẮT SỰ KIỆN CUỘN DANH SÁCH & LAYOUT ---
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            // Khi RecyclerView DỪNG HẲN (SCROLL_STATE_IDLE) mới bắt đầu xử lý tải quảng cáo 
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                checkAndLoadVisibleAds()
            }
        }
    }

    private val layoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        checkAndLoadVisibleAds()
    }

    private val treeScrollListener = ViewTreeObserver.OnScrollChangedListener {
        checkAndLoadVisibleAds()
    }

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        checkAndLoadVisibleAds()
    }

    private class AdSpanSizeLookup(
        private val originalLookup: GridLayoutManager.SpanSizeLookup,
        private val layoutManager: GridLayoutManager,
        private val isAdPos: (Int) -> Boolean
    ) : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (isAdPos(position)) {
                layoutManager.spanCount
            } else {
                originalLookup.getSpanSize(position)
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.addOnLayoutChangeListener(layoutChangeListener)
        
        try {
            recyclerView.viewTreeObserver.addOnScrollChangedListener(treeScrollListener)
            recyclerView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        } catch (_: Exception) {}

        // Tự động cấu hình SpanSizeLookup cho GridLayoutManager nếu chưa được bọc
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            val currentLookup = layoutManager.spanSizeLookup
            if (currentLookup !is AdSpanSizeLookup) {
                layoutManager.spanSizeLookup = AdSpanSizeLookup(currentLookup, layoutManager) { pos ->
                    isAdPosition(pos)
                }
            }
        }

        // Đẩy task kiểm tra tải quảng cáo ngay khi RecyclerView hiển thị lần đầu
        recyclerView.post {
            checkAndLoadVisibleAds()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        attachedRecyclerView?.removeOnScrollListener(scrollListener)
        attachedRecyclerView?.removeOnLayoutChangeListener(layoutChangeListener)
        
        try {
            val observer = attachedRecyclerView?.viewTreeObserver
            if (observer != null && observer.isAlive) {
                observer.removeOnScrollChangedListener(treeScrollListener)
                observer.removeOnGlobalLayoutListener(globalLayoutListener)
            }
        } catch (_: Exception) {}
        
        attachedRecyclerView = null
        
        // Clean up delayed tasks
        delayedTasks.forEach { handler.removeCallbacks(it.value) }
        delayedTasks.clear()
        
        retryCountMap.clear()
        
        // Xóa tham chiếu cache adMap của adapter này (lifecycle ad do NativeManager quản lý)
        adMap.clear()
        
        activeLoads.clear()
        failedPositions.clear()
        cachedAdPositions = emptyList()
        lastOriginalCount = -1
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdViewHolder) {
            (holder.itemView as? FrameLayout)?.removeAllViews()
        }
        super.onViewRecycled(holder)
    }

    // --- TÍNH TOÁN VỊ TRÍ CHÈN QUẢNG CÁO ĐỘNG ---
    private fun getResolvedAdPositions(): List<Int> {
        if (!canShowAd()) {
            return emptyList()
        }
        val originalCount = getDataItemCount()
        if (originalCount == 0) {
            return emptyList()
        }
        
        // Tối ưu hóa: Nếu số lượng data không đổi so với lần tính trước, dùng luôn mảng cũ đã Cache để đỡ tốn CPU
        if (originalCount == lastOriginalCount) {
            return cachedAdPositions
        }

        val rule = getAdPlacementRule()
        val resolved = when (rule) {
            is AdPlacementRule.Fixed -> {
                // Loại bỏ vị trí trùng lặp và số âm để tránh nhảy index dữ liệu
                val sorted = rule.positions.distinct().filter { it >= 0 }.sorted()
                val list = mutableListOf<Int>()
                var currentTotal = originalCount
                for (pos in sorted) {
                    if (pos <= currentTotal) {
                        list.add(pos)
                        currentTotal++
                    } else {
                        break
                    }
                }
                list
            }
            is AdPlacementRule.Repeating -> {
                // Phòng chống vòng lặp vô hạn nếu interval <= 0 hoặc startPosition < 0
                if (rule.interval <= 0 || rule.startPosition < 0) {
                    emptyList()
                } else {
                    val list = mutableListOf<Int>()
                    var currentAdPos = rule.startPosition
                    var currentTotal = originalCount
                    while (currentAdPos <= currentTotal) {
                        list.add(currentAdPos)
                        currentTotal++
                        currentAdPos += rule.interval
                    }
                    list
                }
            }
        }
        cachedAdPositions = resolved
        lastOriginalCount = originalCount
        return resolved
    }

    fun isAdPosition(position: Int): Boolean {
        return getResolvedAdPositions().contains(position)
    }

    // Ánh xạ vị trí ảo: Chuyển đổi vị trí hiển thị trên UI (bao gồm cả data + ads) 
    // về lại vị trí thực tế của mảng Dữ Liệu gốc để không bị lỗi IndexOutOfBounds
    fun getOriginalPosition(adapterPosition: Int): Int {
        var adCountBefore = 0
        val sortedPositions = getResolvedAdPositions()
        for (pos in sortedPositions) {
            if (pos < adapterPosition) {
                adCountBefore++
            } else {
                break
            }
        }
        return adapterPosition - adCountBefore
    }

    // Tổng số item hiển thị trên RecyclerView = Tổng độ dài Data gốc + Tổng số Quảng cáo chèn vào
    override fun getItemCount(): Int {
        val originalCount = getDataItemCount()
        if (originalCount == 0) return 0
        
        var totalCount = originalCount
        val sortedPositions = getResolvedAdPositions()
        for (adPos in sortedPositions) {
            if (adPos <= totalCount) {
                totalCount++
            } else {
                break
            }
        }
        return totalCount
    }

    // Phân loại kiểu View: Là Quảng Cáo (TYPE_AD) hay Data bình thường
    override fun getItemViewType(position: Int): Int {
        return if (isAdPosition(position)) {
            TYPE_AD
        } else {
            getDataItemViewType(getOriginalPosition(position))
        }
    }

    // Khởi tạo ViewHolder tương ứng với kiểu View
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val frameLayout = FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            AdViewHolder(frameLayout)
        } else {
            onCreateDataViewHolder(parent, viewType)
        }
    }

    // Gắn dữ liệu (Bind) vào ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AdViewHolder) {
            val adFrame = holder.itemView as FrameLayout
            val adLayoutRes = getAdLayoutRes(position)
            
            if (failedPositions.containsKey(position)) {
                adFrame.setGone()
                val params = adFrame.layoutParams
                params.height = 0
                adFrame.layoutParams = params
            } else {
                val ad = adMap[position]
                if (ad != null) {
                    val params = adFrame.layoutParams
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    adFrame.layoutParams = params
                    NativeManager.renderAd(adFrame, ad, adLayoutRes)
                } else {
                    val params = adFrame.layoutParams
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    adFrame.layoutParams = params
                    NativeManager.showLoading(adFrame, adLayoutRes)
                }
            }
        } else {
            val originalPos = getOriginalPosition(position)
            onBindDataViewHolder(holder as VH, originalPos, getDataItem(originalPos))
        }
    }

    private fun safeNotifyItemChanged(pos: Int) {
        val rv = attachedRecyclerView ?: return
        if (rv.isComputingLayout) {
            rv.post { notifyItemChanged(pos) }
        } else {
            notifyItemChanged(pos)
        }
    }

    private fun checkAndLoadVisibleAds() {
        val recyclerView = attachedRecyclerView ?: return
        
        if (!recyclerView.isShown) return
        val rect = android.graphics.Rect()
        val isVisibleOnScreen = recyclerView.getGlobalVisibleRect(rect)
        if (!isVisibleOnScreen) {
            return
        }

        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return
        
        val isScrolling = recyclerView.scrollState != RecyclerView.SCROLL_STATE_IDLE
        val delay = if (isScrolling) scrollingAdLoadDelayMs else idleAdLoadDelayMs

        val iterator = delayedTasks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pos = entry.key
            if (pos < firstVisible || pos > lastVisible) {
                handler.removeCallbacks(entry.value)
                iterator.remove()
                AdsLog.d(TAG, "Cancelled pending ad load delay for position $pos (scrolled off-screen)")
            }
        }
        
        for (pos in firstVisible..lastVisible) {
            if (isAdPosition(pos)) {
                if (!adMap.containsKey(pos) && !delayedTasks.containsKey(pos) && !activeLoads.containsKey(pos) && !failedPositions.containsKey(pos)) {
                    val runnable = Runnable {
                        delayedTasks.remove(pos)
                        bindOrLoadAdForPosition(pos)
                    }
                    delayedTasks[pos] = runnable
                    handler.postDelayed(runnable, delay)
                    AdsLog.d(TAG, "Scheduled ${delay}ms delayed ad load for position $pos")
                }
            }
        }
    }

    private fun bindOrLoadAdForPosition(pos: Int) {
        val context = attachedRecyclerView?.context ?: return
        val adName = getAdName(pos)
        val adId = getIdAd(context, pos)
        val resolvedAdId = if (adId.isNotBlank()) adId else NativeManager.getAdId(adName)

        // 1. Kiểm tra ad sẵn có trong NativeManager (từ preload trước đó hoặc màn hình khác)
        val readyAd = NativeManager.consumeAdWithoutActive(adName)
        if (readyAd != null) {
            adMap[pos] = readyAd
            safeNotifyItemChanged(pos)
            AdsLog.d(TAG, "Reused preloaded ad from NativeManager for position $pos")
            replenishBackgroundAd(context, adName, resolvedAdId)
            return
        }
        
        if (activeLoads[pos] == true) return
        activeLoads[pos] = true
        AdsLog.d(TAG, "Requesting ad preload for position $pos")
        
        fun executePreloadRequest() {
            NativeManager.preloadNative(
                context = context,
                adName = adName,
                adId = resolvedAdId,
                callback = object : INativeAdCallback {
                    override fun onAdLoaded(adName: String) {
                        handler.post {
                            activeLoads.remove(pos)
                            // Kiểm tra nếu vị trí pos vẫn đang hiển thị trên màn hình
                            if (isPositionVisible(pos)) {
                                val nativeAd = NativeManager.consumeAdWithoutActive(adName)
                                if (nativeAd != null) {
                                    adMap[pos] = nativeAd
                                    safeNotifyItemChanged(pos)
                                    AdsLog.d(TAG, "Ad bound successfully at position $pos")
                                    replenishBackgroundAd(context, adName, resolvedAdId)
                                }
                            } else {
                                // Người dùng đã cuộn qua mất -> Giữ ad preloaded trong NativeManager
                                // để vị trí ad tiếp theo hoặc màn hình khác tái sử dụng (Tối ưu Show Rate ~100%)
                                AdsLog.d(TAG, "Ad loaded for position $pos but user scrolled away. Preserved in NativeManager.")
                                replenishBackgroundAd(context, adName, resolvedAdId)
                            }
                        }
                    }

                    override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                        handler.post {
                            val retries = retryCountMap[pos] ?: 0
                            if (retries < 1) {
                                retryCountMap[pos] = retries + 1
                                AdsLog.d(TAG, "Ad load failed for position $pos. Retrying once (attempt ${retries + 1})...")
                                executePreloadRequest()
                            } else {
                                activeLoads.remove(pos)
                                failedPositions[pos] = true
                                safeNotifyItemChanged(pos)
                                AdsLog.d(TAG, "Ad load failed for position $pos after 1 retry. Setting view to GONE.")
                            }
                        }
                    }
                }
            )
        }

        executePreloadRequest()
    }

    private fun replenishBackgroundAd(context: Context, adName: String, adId: String) {
        AdsLog.d(TAG, "Replenishing background ad for tag: $adName")
        NativeManager.preloadNative(
            context = context,
            adName = adName,
            adId = adId
        )
    }

    private fun isPositionVisible(pos: Int): Boolean {
        val recyclerView = attachedRecyclerView ?: return false
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) return false
        return pos in firstVisible..lastVisible
    }

    private fun handleListSizeChanged() {
        delayedTasks.forEach { handler.removeCallbacks(it.value) }
        delayedTasks.clear()
        
        activeLoads.clear()
        failedPositions.clear()
        retryCountMap.clear()
        lastOriginalCount = -1
    }

    fun clearAdCache() {
        adMap.clear()
        activeLoads.clear()
        failedPositions.clear()
        cachedAdPositions = emptyList()
        lastOriginalCount = -1
        delayedTasks.forEach { handler.removeCallbacks(it.value) }
        delayedTasks.clear()
        retryCountMap.clear()
    }

    class AdViewHolder(container: FrameLayout) : RecyclerView.ViewHolder(container)
}
