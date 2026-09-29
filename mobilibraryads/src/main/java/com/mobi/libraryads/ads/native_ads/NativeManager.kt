package com.mobi.libraryads.ads.native_ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.native_ads.collapsible.CollapsibleController
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.ads.native_ads.loader.NativeAdLoader
import com.mobi.libraryads.ads.native_ads.model.AdLoadState
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy
import com.mobi.libraryads.ads.native_ads.model.NativeAdEntry
import com.mobi.libraryads.ads.native_ads.renderer.DefaultNativeViewBinder
import com.mobi.libraryads.ads.native_ads.renderer.MetaNativeViewBinder
import com.mobi.libraryads.ads.native_ads.renderer.NativeAdRenderer
import com.mobi.libraryads.ads.native_ads.repository.NativeAdRepository
import com.mobi.libraryads.commons.adjust.trackingRevenueAd
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible
import com.google.android.gms.ads.nativead.NativeAd
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.os.CountDownTimer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mobi.libraryads.AdsApplication.Companion.isFirstOpen
import java.util.concurrent.ConcurrentHashMap

object NativeManager {
    private const val TAG = "NativeManager"

    private val repository = NativeAdRepository()
    private val loader = NativeAdLoader(repository)
    private val renderer = NativeAdRenderer(
        listOf(MetaNativeViewBinder(), DefaultNativeViewBinder())
    )
    private val collapsibleController = CollapsibleController()
    private val activeCountdownTimers = ConcurrentHashMap<String, () -> Unit>()

    fun startFullscreenCountdown(adName: String) {
        activeCountdownTimers.remove(adName)?.invoke()
    }

    private val showingFullscreenAds = ConcurrentHashMap<String, Boolean>()

    fun isFullscreenAdShowing(adName: String): Boolean {
        return showingFullscreenAds.containsKey(adName)
    }

    private fun canShowAd(context: Context): Boolean {
        return context.isInternetConnected() && !SPF(context).is_app_pro
    }

    // ══════════════════════════════════════════
    // KIỂM TRA TRẠNG THÁI
    // ══════════════════════════════════════════

    /** Ad đã load xong và sẵn sàng show */
    fun isAdReady(adName: String): Boolean = repository.isAdReady(adName)

    /** Ad đã load xong (không phải LOADING, có thể thành công hoặc thất bại) */
    fun isLoadDone(adName: String): Boolean = repository.isLoadDone(adName)

    /** Ad load thất bại (ERROR) */
    fun isAdFailed(adName: String): Boolean = repository.isAdFailed(adName)

    // ══════════════════════════════════════════
    // PRELOAD (Load trước, chưa show)
    // ══════════════════════════════════════════

    /**
     * Preload 1 native ad thường (chỉ có 1 id)
     */
    fun preloadNative(
        context: Context,
        adName: String,
        adId: String = "",
        canShowAd: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = adId.ifBlank { getAdIdFromConfig(adName) }
            NativeAdEntry(name = adName, id = resolvedId)
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }

        if (!canShowAd) {
            callback?.onAdFailedToLoad(adName, "Ad disabled by Remote Config")
            handlePendingShow(entry, false)
            return
        }

        if (!canShowAd(context)) {
            callback?.onAdFailedToLoad(adName, "Ad conditions not met (Pro user or no internet)")
            handlePendingShow(entry, false)
            return
        }

        loader.preloadNative(context, entry) { success ->
            if (success) {
                callback?.onAdLoaded(adName)
            } else {
                callback?.onAdFailedToLoad(adName, "Failed to load native ad")
            }
            handlePendingShow(entry, success)
        }
    }

    /**
     * Preload native ad có high floor (chỉ dùng cho các vị trí cần, vd: language)
     */
    fun preloadNativeWithHigh(
        context: Context,
        adName: String,
        adId: String,
        idHigh: String,
        strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
        canShowIdAll: Boolean = true,
        canShowIdHigh: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            NativeAdEntry(name = adName, id = adId, idHigh = idHigh, strategy = strategy)
        }
        entry.strategy = strategy
        entry.id = adId
        entry.idHigh = idHigh

        if (!canShowIdAll && !canShowIdHigh) {
            callback?.onAdFailedToLoad(adName, "Ad disabled by Remote Config (Both floors)")
            handlePendingShow(entry, false)
            return
        }

        if (!canShowAd(context)) {
            callback?.onAdFailedToLoad(adName, "Ad conditions not met (Pro user or no internet)")
            handlePendingShow(entry, false)
            return
        }

        loader.preloadNativeWithHigh(
            context,
            entry,
            strategy,
            canShowIdAll,
            canShowIdHigh
        ) { success ->
            if (success) {
                callback?.onAdLoaded(adName)
            } else {
                callback?.onAdFailedToLoad(adName, "Failed to load native ad with high floor")
            }
            handlePendingShow(entry, success)
        }
    }

    // ══════════════════════════════════════════
    // SHOW NATIVE THƯỜNG
    // ══════════════════════════════════════════

    /**
     * Show native ad lên UI
     */
    fun showNative(
        adFrame: FrameLayout,
        adName: String,
        adId: String = "",
        adLayout: Int = 0,
        isPreload: Boolean = false,
        canShowAd: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = if (adId.isNotBlank()) adId else getAdIdFromConfig(adName)
            val resolvedIdHigh = if (adId.isNotBlank()) "" else getAdIdHighFromConfig(adName)
            NativeAdEntry(name = adName, id = resolvedId, idHigh = resolvedIdHigh)
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }

        if (!canShowAd) {
            renderer.hideLoading(adFrame)
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config")
            return
        }

        performShow(
            adFrame = adFrame,
            entry = entry,
            adLayout = adLayout,
            isPreload = isPreload,
            canShowIdAll = canShowAd,
            canShowIdHigh = canShowAd,
            callback = callback
        )
    }

    // ══════════════════════════════════════════
    // SHOW NATIVE COLLAPSIBLE
    // ══════════════════════════════════════════

    /**
     * Show native collapsible (auto refresh theo lifecycle)
     */
    fun showCollapsibleNative(
        activity: AppCompatActivity,
        adFrame: FrameLayout,
        adName: String,
        adId: String = "",
        adLayout: Int,
        refreshTime: Long = 30_000L,
        isPreload: Boolean = true,
        canShowAd: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        if (!canShowAd) {
            renderer.hideLoading(adFrame)
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config")
            collapsibleController.stop(activity)
            return
        }

        collapsibleController.start(activity, refreshTime, adName) {
            val entry = repository.getOrCreate(adName) {
                val resolvedId = if (adId.isNotBlank()) adId else getAdIdFromConfig(adName)
                val resolvedIdHigh = if (adId.isNotBlank()) "" else getAdIdHighFromConfig(adName)
                NativeAdEntry(name = adName, id = resolvedId, idHigh = resolvedIdHigh)
            }
            if (adId.isNotBlank()) {
                entry.id = adId
            }
            performShow(
                adFrame = adFrame,
                entry = entry,
                adLayout = adLayout,
                isPreload = isPreload,
                canShowIdAll = canShowAd,
                canShowIdHigh = canShowAd,
                callback = callback
            )
        }
    }

    /** Dừng collapsible cho activity */
    fun stopCollapsible(activity: Activity) {
        collapsibleController.stop(activity)
    }

    // ══════════════════════════════════════════
    // LOAD & SHOW (Tiện ích - load rồi show luôn)
    // ══════════════════════════════════════════

    /**
     * Load rồi show ngay — gộp preload + show
     */
    fun loadAndShowNative(
        adFrame: FrameLayout,
        adName: String,
        adId: String = "",
        adLayout: Int = 0,
        isPreload: Boolean = false,
        canShowAd: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = if (adId.isNotBlank()) adId else getAdIdFromConfig(adName)
            val resolvedIdHigh = if (adId.isNotBlank()) "" else getAdIdHighFromConfig(adName)
            NativeAdEntry(name = adName, id = resolvedId, idHigh = resolvedIdHigh)
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }

        if (!canShowAd) {
            renderer.hideLoading(adFrame)
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config")
            return
        }

        repository.updateState(adName, AdLoadState.NOT_LOADED)
        performShow(
            adFrame = adFrame,
            entry = entry,
            adLayout = adLayout,
            isPreload = isPreload,
            canShowIdAll = canShowAd,
            canShowIdHigh = canShowAd,
            callback = callback
        )
    }

    /**
     * Load với high floor rồi show ngay
     */
    fun loadAndShowNativeWithHigh(
        adFrame: FrameLayout,
        adName: String,
        adId: String = "",
        idHigh: String = "",
        strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
        adLayout: Int = 0,
        isPreload: Boolean = false,
        canShowIdAll: Boolean = true,
        canShowIdHigh: Boolean = true,
        callback: INativeAdCallback? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = if (adId.isNotBlank()) adId else getAdIdFromConfig(adName)
            val resolvedIdHigh = if (idHigh.isNotBlank()) idHigh else getAdIdHighFromConfig(adName)
            NativeAdEntry(name = adName, id = resolvedId, idHigh = resolvedIdHigh)
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }
        if (idHigh.isNotBlank()) {
            entry.idHigh = idHigh
        }

        if (!canShowIdAll && !canShowIdHigh) {
            renderer.hideLoading(adFrame)
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config (Both floors)")
            return
        }

        repository.updateState(adName, AdLoadState.NOT_LOADED)
        performShow(
            adFrame = adFrame,
            entry = entry,
            adLayout = adLayout,
            isPreload = isPreload,
            canShowIdAll = canShowIdAll,
            canShowIdHigh = canShowIdHigh,
            callback = callback
        )
    }

    /**
     * Show fullscreen native ad with countdown timer (1 ID)
     */
    fun showFullscreenNativeWithCountdown(
        activity: AppCompatActivity,
        adName: String,
        adId: String = "",
        adLayoutRes: Int,
        countdownSeconds: Int = 3,
        isPreload: Boolean = false,
        canShowAd: Boolean = true,
        startCountdownImmediately: Boolean = true,
        layoutContainerId: Int = android.R.id.content,
        dismissOnAdClick: Boolean = true,
        callback: INativeAdCallback? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = if (adId.isNotBlank()) adId else getAdIdFromConfig(adName)
            NativeAdEntry(name = adName, id = resolvedId, idHigh = "")
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }
        entry.idHigh = ""

        if (!canShowAd) {
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config")
            onDismissed?.invoke()
            return
        }

        val adReady = repository.isAdReady(entry.name)

        if (!startCountdownImmediately && !adReady) {
            AdsLog.d(
                TAG,
                "showFullscreenNativeWithCountdown: Ad is not ready yet. Delaying show until startFullscreenCountdown."
            )
            val triggerAction: () -> Unit = {
                if (repository.isAdReady(entry.name)) {
                    performShowFullscreenNativeWithCountdown(
                        activity = activity,
                        entry = entry,
                        adLayoutRes = adLayoutRes,
                        countdownSeconds = countdownSeconds,
                        isPreload = isPreload,
                        canShowIdAll = canShowAd,
                        canShowIdHigh = canShowAd,
                        startCountdownImmediately = true,
                        layoutContainerId = layoutContainerId,
                        dismissOnAdClick = dismissOnAdClick,
                        callback = callback,
                        onDismissed = onDismissed
                    )
                } else {
                    AdsLog.d(
                        TAG,
                        "showFullscreenNativeWithCountdown: Ad is still not ready on startFullscreenCountdown. Dismissing directly."
                    )
                    if (isPreload && canShowAd(activity)) {
                        preloadNative(activity, entry.name, entry.id, canShowAd = canShowAd)
                    }
                    onDismissed?.invoke()
                }
            }
            activeCountdownTimers[entry.name] = triggerAction
            return
        }

        performShowFullscreenNativeWithCountdown(
            activity = activity,
            entry = entry,
            adLayoutRes = adLayoutRes,
            countdownSeconds = countdownSeconds,
            isPreload = isPreload,
            canShowIdAll = canShowAd,
            canShowIdHigh = canShowAd,
            startCountdownImmediately = startCountdownImmediately,
            layoutContainerId = layoutContainerId,
            dismissOnAdClick = dismissOnAdClick,
            callback = callback,
            onDismissed = onDismissed
        )
    }

    /**
     * Show fullscreen native ad with countdown timer (2 IDs: High Floor and Normal Floor)
     */
    fun showFullscreenNativeWithCountdownWithHigh(
        activity: AppCompatActivity,
        adName: String,
        adId: String = "",
        idHigh: String = "",
        strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
        adLayoutRes: Int,
        countdownSeconds: Int = 3,
        isPreload: Boolean = false,
        canShowIdAll: Boolean = true,
        canShowIdHigh: Boolean = true,
        startCountdownImmediately: Boolean = true,
        layoutContainerId: Int = android.R.id.content,
        dismissOnAdClick: Boolean = true,
        callback: INativeAdCallback? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        val entry = repository.getOrCreate(adName) {
            val resolvedId = adId.ifBlank { getAdIdFromConfig(adName) }
            val resolvedIdHigh =
                idHigh.ifBlank { (if (adId.isNotBlank()) "" else getAdIdHighFromConfig(adName)) }
            NativeAdEntry(
                name = adName,
                id = resolvedId,
                idHigh = resolvedIdHigh,
                strategy = strategy
            )
        }
        if (adId.isNotBlank()) {
            entry.id = adId
        }
        if (idHigh.isNotBlank()) {
            entry.idHigh = idHigh
        }
        entry.strategy = strategy

        if (!canShowIdAll && !canShowIdHigh) {
            callback?.onAdShowFailed(adName, "Ad disabled by Remote Config (Both floors)")
            onDismissed?.invoke()
            return
        }

        val adReady = repository.isAdReady(entry.name)

        if (!startCountdownImmediately && !adReady) {
            AdsLog.d(
                TAG,
                "showFullscreenNativeWithCountdownWithHigh: Ad is not ready yet. Delaying show until startFullscreenCountdown."
            )
            val triggerAction: () -> Unit = {
                if (repository.isAdReady(entry.name)) {
                    performShowFullscreenNativeWithCountdown(
                        activity = activity,
                        entry = entry,
                        adLayoutRes = adLayoutRes,
                        countdownSeconds = countdownSeconds,
                        isPreload = isPreload,
                        canShowIdAll = canShowIdAll,
                        canShowIdHigh = canShowIdHigh,
                        startCountdownImmediately = true,
                        layoutContainerId = layoutContainerId,
                        dismissOnAdClick = dismissOnAdClick,
                        callback = callback,
                        onDismissed = onDismissed
                    )
                } else {
                    AdsLog.d(
                        TAG,
                        "showFullscreenNativeWithCountdownWithHigh: Ad is still not ready on startFullscreenCountdown. Dismissing directly."
                    )
                    if (isPreload && canShowAd(activity)) {
                        if (entry.idHigh.isNotBlank()) {
                            preloadNativeWithHigh(
                                activity,
                                entry.name,
                                entry.id,
                                entry.idHigh,
                                strategy = entry.strategy,
                                canShowIdAll = canShowIdAll,
                                canShowIdHigh = canShowIdHigh
                            )
                        } else {
                            preloadNative(activity, entry.name, entry.id, canShowAd = canShowIdAll)
                        }
                    }
                    onDismissed?.invoke()
                }
            }
            activeCountdownTimers[entry.name] = triggerAction
            return
        }

        performShowFullscreenNativeWithCountdown(
            activity = activity,
            entry = entry,
            adLayoutRes = adLayoutRes,
            countdownSeconds = countdownSeconds,
            isPreload = isPreload,
            canShowIdAll = canShowIdAll,
            canShowIdHigh = canShowIdHigh,
            startCountdownImmediately = startCountdownImmediately,
            layoutContainerId = layoutContainerId,
            dismissOnAdClick = dismissOnAdClick,
            callback = callback,
            onDismissed = onDismissed
        )
    }

    private fun performShowFullscreenNativeWithCountdown(
        activity: AppCompatActivity,
        entry: NativeAdEntry,
        adLayoutRes: Int,
        countdownSeconds: Int,
        isPreload: Boolean,
        canShowIdAll: Boolean = true,
        canShowIdHigh: Boolean = true,
        startCountdownImmediately: Boolean = true,
        layoutContainerId: Int,
        dismissOnAdClick: Boolean = true,
        callback: INativeAdCallback?,
        onDismissed: (() -> Unit)?
    ) {
        if (!canShowAd(activity) || !repository.isAdReady(entry.name)) {
            AdsLog.d(
                TAG,
                "showFullscreenNativeWithCountdown: Ad is not ready or conditions not met. Dismissing directly."
            )
            if (isPreload && canShowAd(activity)) {
                if (entry.idHigh.isNotBlank()) {
                    preloadNativeWithHigh(
                        activity,
                        entry.name,
                        entry.id,
                        entry.idHigh,
                        strategy = entry.strategy,
                        canShowIdAll = canShowIdAll,
                        canShowIdHigh = canShowIdHigh
                    )
                } else {
                    preloadNative(activity, entry.name, entry.id, canShowAd = canShowIdAll)
                }
            }
            onDismissed?.invoke()
            return
        }

        val viewRoot = activity.findViewById<ViewGroup>(layoutContainerId)
        if (viewRoot == null) {
            AdsLog.e(
                TAG,
                "showFullscreenNativeWithCountdown: viewRoot is null, cannot display overlay."
            )
            onDismissed?.invoke()
            return
        }

        val inflater = LayoutInflater.from(activity)
        val overlayView = try {
            inflater.inflate(adLayoutRes, viewRoot, false)
        } catch (e: Exception) {
            e.printStackTrace()
            AdsLog.e(TAG, "showFullscreenNativeWithCountdown: Failed to inflate overlay layout.")
            onDismissed?.invoke()
            return
        }

        overlayView.visibility = View.GONE
        overlayView.isClickable = true
        overlayView.isFocusable = true
        overlayView.isFocusableInTouchMode = true

        val layoutAdsNative = overlayView.findViewById<FrameLayout>(R.id.layoutAds)
        val btnNextNative = overlayView.findViewById<View>(R.id.btnNext)
        val tvCountdownNative = overlayView.findViewById<TextView>(R.id.tvCountdown)

        if (layoutAdsNative == null) {
            AdsLog.e(
                TAG,
                "showFullscreenNativeWithCountdown: layoutAds FrameLayout not found in overlay layout."
            )
            onDismissed?.invoke()
            return
        }

        var countDownTimerNative: CountDownTimer? = null
        var isCountingDownNative = false
        var isDismissed = false
        var isAdClicked = false

        val finishAndNextAction = {
            if (!isDismissed) {
                isDismissed = true
                showingFullscreenAds.remove(entry.name)
                activeCountdownTimers.remove(entry.name)
                countDownTimerNative?.cancel()
                try {
                    viewRoot.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                callback?.onAdDismissed(entry.name)
                onDismissed?.invoke()
            }
        }

        viewRoot.addView(overlayView)

        overlayView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                if (isCountingDownNative) {
                    return@setOnKeyListener true
                }
            }
            false
        }

        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                if (isAdClicked && !isDismissed) {
                    activity.lifecycle.removeObserver(this)
                    finishAndNextAction()
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                showingFullscreenAds.remove(entry.name)
                activeCountdownTimers.remove(entry.name)
                countDownTimerNative?.cancel()
                try {
                    viewRoot.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                activity.lifecycle.removeObserver(this)
                super.onDestroy(owner)
            }
        }
        activity.lifecycle.addObserver(lifecycleObserver)

        val wrappedCallback = object : INativeAdCallback {
            override fun onAdLoaded(adName: String) {
                callback?.onAdLoaded(adName)
            }

            override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                callback?.onAdFailedToLoad(adName, errorMessage)
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onAdShown(adName: String, isHighFloor: Boolean) {
                callback?.onAdShown(adName, isHighFloor)
                showingFullscreenAds[entry.name] = true

                overlayView.visibility = View.VISIBLE
                tvCountdownNative?.setVisible()
                btnNextNative?.setGone()

                // Chuyển tiếp luồng chạm (MotionEvent) từ view countdown sang Quảng cáo Native để AdMob nhận diện cú nhấp CTR thực sự
                tvCountdownNative?.setOnTouchListener { _, event ->
                    AdsLog.d("tvCountdownNative?.setOnClickListener")
                    val targetView = layoutAdsNative.findViewById<View>(R.id.ad_call_to_action)
                        ?: layoutAdsNative.findViewById<View>(R.id.native_ad_view)
                        ?: layoutAdsNative
                    targetView.dispatchTouchEvent(event)
                    true
                }

                isCountingDownNative = true
                tvCountdownNative?.text = "$countdownSeconds"

                val startTimerAction = {
                    if (countDownTimerNative == null && !activity.isFinishing && !activity.isDestroyed) {
                        countDownTimerNative =
                            object : CountDownTimer((countdownSeconds * 1000L) - 100, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    if (activity.isFinishing || activity.isDestroyed) return
                                    val secondsLeft = (millisUntilFinished / 1000 + 1).toInt()
                                    if (secondsLeft > 0) {
                                        tvCountdownNative?.text = "$secondsLeft"
                                    }
                                }

                                override fun onFinish() {
                                    if (activity.isFinishing || activity.isDestroyed) return
                                    isCountingDownNative = false
                                    tvCountdownNative?.setGone()
                                    btnNextNative?.setVisible()
                                    btnNextNative?.setOnClickListener {
                                        activeCountdownTimers.remove(entry.name)
                                        activity.lifecycle.removeObserver(lifecycleObserver)
                                        finishAndNextAction()
                                    }
                                }
                            }.start()
                    }
                }

                if (startCountdownImmediately) {
                    startTimerAction()
                } else {
                    activeCountdownTimers[entry.name] = startTimerAction
                }
            }

            override fun onAdShowFailed(adName: String, reason: String) {
                AdsLog.e(TAG, "showFullscreenNativeWithCountdown: onAdShowFailed: $reason")
                showingFullscreenAds.remove(entry.name)
                activeCountdownTimers.remove(entry.name)
                activity.lifecycle.removeObserver(lifecycleObserver)
                try {
                    viewRoot.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (isPreload && canShowAd(activity)) {
                    if (entry.idHigh.isNotBlank()) {
                        preloadNativeWithHigh(
                            activity,
                            entry.name,
                            entry.id,
                            entry.idHigh,
                            strategy = entry.strategy,
                            canShowIdAll = canShowIdAll,
                            canShowIdHigh = canShowIdHigh
                        )
                    } else {
                        preloadNative(activity, entry.name, entry.id, canShowAd = canShowIdAll)
                    }
                }
                callback?.onAdShowFailed(adName, reason)
                onDismissed?.invoke()
            }

            override fun onAdClicked(adName: String) {
                isAdClicked = true
                callback?.onAdClicked(adName)
            }

            override fun onAdImpression(adName: String) {
                callback?.onAdImpression(adName)
            }

            override fun onAdDismissed(adName: String) {
                callback?.onAdDismissed(adName)
            }
        }

        performShow(
            adFrame = layoutAdsNative,
            entry = entry,
            adLayout = 0,
            isPreload = isPreload,
            canShowIdAll = canShowIdAll,
            canShowIdHigh = canShowIdHigh,
            callback = wrappedCallback
        )
    }

    // ══════════════════════════════════════════
    // LOGIC NỘI BỘ
    // ══════════════════════════════════════════

    private fun performShow(
        adFrame: FrameLayout,
        entry: NativeAdEntry,
        adLayout: Int,
        isPreload: Boolean,
        canShowIdAll: Boolean = true,
        canShowIdHigh: Boolean = true,
        callback: INativeAdCallback?
    ) {
        if (!canShowAd(adFrame.context)) {
            renderer.hideLoading(adFrame)
            callback?.onAdShowFailed(entry.name, "Conditions not met (Pro user or no internet)")
            return
        }

        val activeAd = entry.activeAd
        if (activeAd != null && entry.state != AdLoadState.LOADED) {
            adFrame.setVisible()
            AdsLog.d(TAG, "performShow: Reusing active ad for ${entry.name}")
            AdsLog.d(
                "NativeAdLoader",
                "performShow [Reusing Active Ad]: adName = ${entry.name}, adId = ${entry.id}, instanceId = ${
                    System.identityHashCode(activeAd)
                }"
            )
            renderer.render(adFrame, activeAd, adLayout, entry.adapterClassName)
            callback?.onAdShown(entry.name, entry.isHighFloorActive)
            registerActiveAdLifecycle(adFrame.context, entry.name, activeAd)

            if (entry.state == AdLoadState.NOT_LOADED || entry.state == AdLoadState.ERROR) {
                if (isPreload) {
                    AdsLog.d(
                        TAG,
                        "performShow: Active ad is reused but next ad is not loading. Preloading in background."
                    )
                    repository.setPendingShow(entry.name, adFrame, adLayout, isPreload, callback)
                    if (entry.idHigh.isNotBlank()) {
                        preloadNativeWithHigh(
                            adFrame.context,
                            entry.name,
                            entry.id,
                            entry.idHigh,
                            strategy = entry.strategy,
                            canShowIdAll = canShowIdAll,
                            canShowIdHigh = canShowIdHigh
                        )
                    } else {
                        preloadNative(
                            adFrame.context,
                            entry.name,
                            entry.id,
                            canShowAd = canShowIdAll
                        )
                    }
                }
            } else if (entry.state == AdLoadState.LOADING) {
                repository.setPendingShow(entry.name, adFrame, adLayout, isPreload, callback)
            }
            return
        }

        when (entry.state) {
            AdLoadState.LOADED -> {
                val ad = repository.consumeAd(entry.name)
                if (ad != null) {
                    adFrame.setVisible()
                    AdsLog.d(TAG, "performShow: Showing preloaded ad for ${entry.name}")
                    AdsLog.d(
                        "NativeAdLoader",
                        "performShow [Showing Preloaded Ad]: adName = ${entry.name}, adId = ${entry.id}, instanceId = ${
                            System.identityHashCode(ad)
                        }"
                    )
                    renderer.render(adFrame, ad, adLayout, entry.adapterClassName)
                    trackRevenue(ad)
                    callback?.onAdShown(entry.name, entry.isHighFloorActive)
                    registerActiveAdLifecycle(adFrame.context, entry.name, ad)

                    if (isPreload) {
                        AdsLog.d(TAG, "performShow: Preloading next ad for ${entry.name}")
                        if (entry.idHigh.isNotBlank()) {
                            preloadNativeWithHigh(
                                adFrame.context,
                                entry.name,
                                entry.id,
                                entry.idHigh,
                                strategy = entry.strategy,
                                canShowIdAll = canShowIdAll,
                                canShowIdHigh = canShowIdHigh
                            )
                        } else {
                            preloadNative(
                                adFrame.context,
                                entry.name,
                                entry.id,
                                canShowAd = canShowIdAll
                            )
                        }
                    }
                } else {
                    renderer.hideLoading(adFrame)
                    callback?.onAdShowFailed(entry.name, "Loaded ad was consumed or null")
                }
            }

            AdLoadState.LOADING -> {
                AdsLog.d(
                    TAG,
                    "performShow: Ad ${entry.name} is currently loading. Registering pending show."
                )
                renderer.showLoading(adFrame, adLayout)
                repository.setPendingShow(entry.name, adFrame, adLayout, isPreload, callback)
            }

            AdLoadState.ERROR -> {
                if (isPreload) {
                    AdsLog.d(
                        TAG,
                        "performShow: Ad ${entry.name} failed previously. But isPreload is true, loading again."
                    )
                    renderer.showLoading(adFrame, adLayout)
                    repository.setPendingShow(entry.name, adFrame, adLayout, isPreload, callback)
                    if (entry.idHigh.isNotBlank()) {
                        preloadNativeWithHigh(
                            adFrame.context,
                            entry.name,
                            entry.id,
                            entry.idHigh,
                            strategy = entry.strategy,
                            canShowIdAll = canShowIdAll,
                            canShowIdHigh = canShowIdHigh
                        )
                    } else {
                        preloadNative(
                            adFrame.context,
                            entry.name,
                            entry.id,
                            canShowAd = canShowIdAll
                        )
                    }
                } else {
                    AdsLog.d(
                        TAG,
                        "performShow: Ad ${entry.name} failed to load previously. Not reloading."
                    )
                    renderer.hideLoading(adFrame)
                    callback?.onAdShowFailed(entry.name, "Ad previously failed to load")
                }
            }

            AdLoadState.NOT_LOADED -> {
                AdsLog.d(
                    TAG,
                    "performShow: Ad ${entry.name} not loaded. Loading now and showing upon completion."
                )
                renderer.showLoading(adFrame, adLayout)
                repository.setPendingShow(entry.name, adFrame, adLayout, isPreload, callback)

                if (entry.idHigh.isNotBlank()) {
                    preloadNativeWithHigh(
                        adFrame.context,
                        entry.name,
                        entry.id,
                        entry.idHigh,
                        strategy = entry.strategy,
                        canShowIdAll = canShowIdAll,
                        canShowIdHigh = canShowIdHigh
                    )
                } else {
                    preloadNative(adFrame.context, entry.name, entry.id, canShowAd = canShowIdAll)
                }
            }
        }
    }

    private fun handlePendingShow(entry: NativeAdEntry, success: Boolean) {
        val pending = repository.consumePendingShow(entry.name) ?: return
        val frame = pending.frame.get() ?: return
        val callback = pending.callback

        if (success) {
            val ad = repository.consumeAd(entry.name)
            if (ad != null) {
                AdsLog.d(TAG, "handlePendingShow: Pending ad loaded. Showing ad for ${entry.name}")
                AdsLog.d(
                    "NativeAdLoader",
                    "handlePendingShow [Showing Pending Ad]: adName = ${entry.name}, adId = ${entry.id}, instanceId = ${
                        System.identityHashCode(ad)
                    }"
                )
                renderer.render(frame, ad, pending.layoutRes, entry.adapterClassName)
                trackRevenue(ad)
                callback?.onAdShown(entry.name, entry.isHighFloorActive)
                registerActiveAdLifecycle(frame.context, entry.name, ad)

                if (pending.isPreload) {
                    AdsLog.d(TAG, "handlePendingShow: Preloading next ad for ${entry.name}")
                    if (entry.idHigh.isNotBlank()) {
                        preloadNativeWithHigh(
                            frame.context,
                            entry.name,
                            entry.id,
                            entry.idHigh,
                            strategy = entry.strategy
                        )
                    } else {
                        preloadNative(frame.context, entry.name, entry.id)
                    }
                }
            } else {
                if (entry.activeAd == null) {
                    renderer.hideLoading(frame)
                }
                callback?.onAdShowFailed(entry.name, "Loaded ad was consumed or null")
            }
        } else {
            if (entry.activeAd == null) {
                renderer.hideLoading(frame)
            }
            callback?.onAdShowFailed(entry.name, "Ad failed to load")
        }
    }

    private fun trackRevenue(nativeAd: NativeAd) {
        try {
            trackingRevenueAd(nativeAd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Xóa tất cả cache */
    fun clearAll() {
        repository.clear()
        collapsibleController.stopAll()
    }

    private fun getAdIdFromConfig(adName: String): String {
        return try {
            if (isFirstOpen) {
                when (adName) {
                    EnumAdsNamePosition.NATIVE_LANGUAGE.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangId
                    EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangClickId
                    EnumAdsNamePosition.NATIVE_ONBOARDING_1.position -> FOConfigs.obConfig.adsOBConfig.nativeOB1Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_2.position -> FOConfigs.obConfig.adsOBConfig.nativeOB2Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_3.position -> FOConfigs.obConfig.adsOBConfig.nativeOB3Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_4.position -> FOConfigs.obConfig.adsOBConfig.nativeOB4Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull12Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull23Id
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull34Id
                    else -> ""
                }
            } else {
                when (adName) {
                    EnumAdsNamePosition.NATIVE_LANGUAGE.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangIdS2
                    EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangClickIdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_1.position -> FOConfigs.obConfig.adsOBConfig.nativeOB1IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_2.position -> FOConfigs.obConfig.adsOBConfig.nativeOB2IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_3.position -> FOConfigs.obConfig.adsOBConfig.nativeOB3IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_4.position -> FOConfigs.obConfig.adsOBConfig.nativeOB4IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull12IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull23IdS2
                    EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position -> FOConfigs.obConfig.adsOBConfig.nativeOBFull34IdS2
                    else -> ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun getAdIdHighFromConfig(adName: String): String {
        return try {
            if (isFirstOpen) {
                when (adName) {
                    EnumAdsNamePosition.NATIVE_LANGUAGE.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangHighId
                    EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangClickHighId
                    else -> ""
                }
            } else {
                when (adName) {
                    EnumAdsNamePosition.NATIVE_LANGUAGE.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangHighIdS2
                    EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position -> FOConfigs.languageConfig.adsLanguageConfig.nativeLangClickHighIdS2
                    else -> ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getAdId(adName: String): String {
        val entry = repository.get(adName)
        if (entry != null && entry.id.isNotBlank()) {
            return entry.id
        }
        return getAdIdFromConfig(adName)
    }

    fun getAdIdHigh(adName: String): String {
        val entry = repository.get(adName)
        if (entry != null && entry.idHigh.isNotBlank()) {
            return entry.idHigh
        }
        return getAdIdHighFromConfig(adName)
    }

    fun consumeAd(adName: String): NativeAd? {
        return repository.consumeAd(adName)
    }

    fun isPreloadedAdReady(adName: String): Boolean {
        val entry = repository.get(adName) ?: return false
        return entry.state == AdLoadState.LOADED && entry.nativeAd != null
    }

    fun consumeAdWithoutActive(adName: String): NativeAd? {
        return repository.consumeAdWithoutActive(adName)
    }

    fun renderAd(
        adFrame: FrameLayout,
        nativeAd: NativeAd,
        adLayoutRes: Int,
        adapterClassName: String = ""
    ) {
        renderer.render(adFrame, nativeAd, adLayoutRes, adapterClassName)
    }

    fun showLoading(adFrame: FrameLayout, adLayoutRes: Int = 0) {
        renderer.showLoading(adFrame, adLayoutRes)
    }

    fun hideLoading(adFrame: FrameLayout) {
        renderer.hideLoading(adFrame)
    }

    private fun getLifecycleOwner(context: Context): LifecycleOwner? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is LifecycleOwner) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return ctx as? LifecycleOwner
    }

    private fun registerActiveAdLifecycle(context: Context, adName: String, adInstance: NativeAd) {
        val lifecycleOwner = getLifecycleOwner(context) ?: return
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                repository.clearActiveAdIfMatch(adName, adInstance)
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }
}
