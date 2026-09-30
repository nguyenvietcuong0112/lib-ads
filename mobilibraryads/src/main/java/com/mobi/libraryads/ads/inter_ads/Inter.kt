package com.mobi.libraryads.ads.inter_ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.mobi.libraryads.AdsApplication
import com.mobi.libraryads.AdsCoroutineScope
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy
import com.mobi.libraryads.ads.utils.StatusShowAd
import com.mobi.libraryads.commons.adjust.trackingRevenueAd
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.inter_high_splash_view
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.inter_splash_view
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.inters_ad_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule.ad_full_capping_time
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.views.base.BaseActivity.Companion.inForceUpdate
import com.mobi.libraryads.views.dialogs.DialogLoadingAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

object Inter {

    private const val TAG = "Inter "
    private var lastShowAdFull = 0L
    private var mInterAds = ConcurrentHashMap<String, InterAdModel>()
    private var mInterAOA: InterstitialAd? = null

    private fun postOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }

    fun showInter(
        activity: Activity,
        adName: String,
        nextAction: (onDismiss: Boolean) -> Unit,
        onShown: (() -> Unit)? = null,
        preload: Boolean = true,
        canShowAd: Boolean = true,
        checkCappingTime: Boolean = true
    ) {
        if (activity is LifecycleOwner) {
            val lifecycle = activity.lifecycle
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                lifecycle.addObserver(object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event == Lifecycle.Event.ON_RESUME) {
                            lifecycle.removeObserver(this)
                            showInter(activity, adName, nextAction, onShown, preload, canShowAd, checkCappingTime)
                        } else if (event == Lifecycle.Event.ON_DESTROY) {
                            lifecycle.removeObserver(this)
                        }
                    }
                })
                return
            }
        }

        var handled = false

        fun runNext(onDismiss: Boolean = false) {
            if (handled) return
            handled = true
            postOnMain {
                nextAction.invoke(onDismiss)
            }
        }

        if (SPF(activity).is_app_pro || !canShowAd) {
            runNext()
            return
        }
        val adModel = mInterAds[adName]
        val interAd = adModel?.interAd ?: run {
            if (preload) preLoadInter(activity, adModel ?: InterAdModel(adName, ""), canShowAd)
            runNext()
            return
        }
        AdsLog.d(
            TAG,
            "showInter: adName = $adName ${interAd.adUnitId} "
        )
        if (checkCappingTime(checkCappingTime)) {

            interAd.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    lastShowAdFull = System.currentTimeMillis()
                    interAd.adEventCallback = null

                    adModel.interAd = null
                    if (preload) preLoadInter(activity, adModel, canShowAd)
                    runNext(true)
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdShowedFullScreenContent() {
                    postOnMain {
                        onShown?.invoke()
                    }
                    inters_ad_view.postFirebaseEvent()
                    AdsLog.d(
                        TAG,
                        "showInter: onAdShowedFullScreenContent = $adName"
                    )
                    StatusShowAd.isInterstitialShown = true
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    interAd.adEventCallback = null
                    AdsLog.d(
                        TAG,
                        "showInter: onAdFailedToShowFullScreenContent = $adName (${fullScreenContentError.message})"
                    )
                    adModel.interAd = null
                    runNext()
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdPaid(value: AdValue) {
                    trackingRevenueAd(interAd, value)
                }
            }

            if (!activity.isFinishing
                && !activity.isDestroyed
                && !StatusShowAd.isInterstitialShown
            ) {
                try {
                    StatusShowAd.isInterstitialShown = true
                    interAd.show(activity)
                } catch (_: Exception) {
                    StatusShowAd.isInterstitialShown = false
                    interAd.adEventCallback = null
                    runNext()
                }
            } else {
                interAd.adEventCallback = null
                runNext()
            }

        } else {
            if (preload) preLoadInter(activity, adModel, canShowAd)
            runNext()
        }
    }

    fun showInterAndNativeFull(
        activity: AppCompatActivity,
        interAdName: String,
        nativeAdName: String,
        nativeAdId: String = "",
        nativeIdHigh: String = "",
        adLayoutRes: Int,
        nextAction: (onDismiss: Boolean) -> Unit,
        countdownSeconds: Int = 3,
        canShowInter: Boolean = true,
        canShowNative: Boolean = true,
        canShowNativeHigh: Boolean = true,
        preloadInter: Boolean = true,
        isPreloadNative: Boolean = true,
        checkCappingTime: Boolean = true
    ) {
        if (!canShowInter) {
            nextAction.invoke(false)
            return
        }

        var interShown = false

        showInter(
            activity = activity,
            adName = interAdName,
            onShown = {
                interShown = true
                NativeManager.showFullscreenNativeWithCountdownWithHigh(
                    activity = activity,
                    adName = nativeAdName,
                    adId = nativeAdId,
                    idHigh = nativeIdHigh,
                    adLayoutRes = adLayoutRes,
                    countdownSeconds = countdownSeconds,
                    isPreload = isPreloadNative,
                    canShowIdAll = canShowNative,
                    canShowIdHigh = canShowNativeHigh,
                    startCountdownImmediately = false,
                    onDismissed = {
                        nextAction.invoke(true)
                    }
                )
            },
            nextAction = { dismissed ->
                if (dismissed && interShown) {
                    NativeManager.startFullscreenCountdown(nativeAdName)
                } else {
                    nextAction.invoke(false)
                }
            },
            preload = preloadInter,
            canShowAd = canShowInter,
            checkCappingTime = checkCappingTime
        )
    }

    private var pendingShowInterAOA: (() -> Unit)? = null
    private var isWaitingForResume = false
    fun showInterAOA(
        activity: Activity,
        nextAction: (onDismiss: Boolean) -> Unit,
        onShown: (() -> Unit)? = null,
        onShowFail: (() -> Unit)? = null
    ) {

        if (activity.isFinishing || activity.isDestroyed) {
            onShowFail?.invoke()
            return
        }
        if (activity is LifecycleOwner) {
            val lifecycle = activity.lifecycle
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                pendingShowInterAOA = { showInterAOA(activity, nextAction, onShown, onShowFail) }

                if (!isWaitingForResume) {
                    isWaitingForResume = true
                    lifecycle.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> {
                                    lifecycle.removeObserver(this)
                                    isWaitingForResume = false
                                    val pending = pendingShowInterAOA
                                    pendingShowInterAOA = null
                                    pending?.invoke()
                                }

                                Lifecycle.Event.ON_DESTROY -> {
                                    lifecycle.removeObserver(this)
                                    isWaitingForResume = false
                                    pendingShowInterAOA = null
                                }

                                else -> Unit
                            }
                        }
                    })
                }
                return
            }
        }

        var loadingDialog: DialogLoadingAds? = null
        fun dismissLoading() {
            postOnMain {
                DialogLoadingAds.dismissLoading(loadingDialog, activity)
                loadingDialog = null
            }
        }

        var handled = false

        fun runNext(onDismiss: Boolean = false) {
            if (handled) return
            handled = true
            dismissLoading()
            postOnMain {
                nextAction.invoke(onDismiss)
            }
        }

        if (SPF(activity).is_app_pro
            || inForceUpdate
            || mInterAOA == null
            || (!ValueRemoteConfigModule.inter_splash_high
                    && !ValueRemoteConfigModule.inter_splash)
        ) {
            postOnMain { onShowFail?.invoke() }
            runNext()
            return
        }

        val currentInterAOA = mInterAOA ?: run {
            postOnMain { onShowFail?.invoke() }
            runNext()
            return
        }

        val isType =
            if (currentInterAOA.adUnitId == FOConfigs.splashConfig.adsSplashConfig.interHighId
                || currentInterAOA.adUnitId == FOConfigs.splashConfig.adsSplashConfig.interHighIdS2
            ) {
                "interHigh"
            } else {
                "interAll"
            }
        AdsLog.d(TAG, "showInterAOA: call show function $isType id = ${currentInterAOA.adUnitId}")

        currentInterAOA.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                lastShowAdFull = System.currentTimeMillis()
                currentInterAOA.adEventCallback = null
                mInterAOA = null
                runNext(true)
                StatusShowAd.isInterstitialShown = false
            }

            override fun onAdShowedFullScreenContent() {
                postOnMain {
                    onShown?.invoke()
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    dismissLoading()
                }, 500)
                inter_splash_view.postFirebaseEvent()
                if (isType == "interHigh") {
                    inter_high_splash_view.postFirebaseEvent()
                }

                AdsLog.d(TAG, "showInterAOA: onAdShowedFullScreenContent $isType ")

                StatusShowAd.isInterstitialShown = true
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                currentInterAOA.adEventCallback = null
                AdsLog.d(TAG, "showInterAOA: onAdFailedToShowFullScreenContent $isType (${fullScreenContentError.message})")
                mInterAOA = null
                postOnMain { onShowFail?.invoke() }
                runNext()
                StatusShowAd.isInterstitialShown = false
            }

            override fun onAdPaid(value: AdValue) {
                trackingRevenueAd(currentInterAOA, value)
            }
        }

        try {
            val delayShowLoading =
                if (FOConfigs.splashConfig.adsSplashConfig.showLoadingAdFull) {
                    loadingDialog = DialogLoadingAds.showLoading(activity)
                    FOConfigs.splashConfig.adsSplashConfig.loadingAdFullTime
                } else 0L
            Handler(Looper.getMainLooper()).postDelayed({
                if (!activity.isFinishing
                    && !activity.isDestroyed
                    && !StatusShowAd.isInterstitialShown
                ) {
                    currentInterAOA.show(activity)
                    StatusShowAd.isInterstitialShown = true
                } else {
                    StatusShowAd.isInterstitialShown = false
                    currentInterAOA.adEventCallback = null
                    onShowFail?.invoke()
                    runNext()
                }
            }, delayShowLoading)
            AdsLog.d(TAG, "showInterAOA: show $isType id = ${currentInterAOA.adUnitId}")
        } catch (_: Exception) {
            StatusShowAd.isInterstitialShown = false
            currentInterAOA.adEventCallback = null
            onShowFail?.invoke()
            runNext()
        }
    }

    fun preLoadInter(
        activity: Context,
        adModel: InterAdModel,
        canShowAd: Boolean
    ) {
        if (!activity.isInternetConnected() || SPF(activity).is_app_pro || !canShowAd) return
        var currentModel = mInterAds[adModel.name]

        if (currentModel == null) {
            mInterAds[adModel.name] = adModel
            currentModel = adModel
        }

        if (currentModel.interAd != null || currentModel.isLoading) return

        AdsLog.d(
            TAG,
            "preLoadInter: activity $activity -- loading adModel = ${adModel.name} ${adModel.id}"
        )
        currentModel.isLoading = true
        val adRequest = AdRequest.Builder(currentModel.id).build()
        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.setImmersiveMode(true)
                    currentModel.interAd = interstitialAd
                    currentModel.isLoading = false
                    AdsLog.d(
                        TAG,
                        "preLoadInter: onAdLoaded $activity -- adModel = ${adModel.name}"
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    currentModel.isLoading = false
                    AdsLog.d(
                        TAG,
                        "preLoadInter: onAdFailedToLoad $activity -- adModel = ${adModel.name} (${loadAdError.message})"
                    )
                }
            })
    }

    fun loadAndShowInterSplash(
        activity: Activity,
        idHigh: String = "",
        idAllPrice: String = "",
        nextAction: (onDismiss: Boolean) -> Unit,
        onShown: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
        onShowFail: (() -> Unit)? = null,
        strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    ) {
        val canShowHigh = ValueRemoteConfigModule.inter_splash_high && idHigh.isNotBlank()
        val canShowAll = ValueRemoteConfigModule.inter_splash && idAllPrice.isNotBlank()
        var overTime = false
        var finished = false

        val mainHandler = Handler(Looper.getMainLooper())

        fun complete(onDismiss: Boolean = false) {
            if (finished) return
            finished = true
            mainHandler.removeCallbacksAndMessages(null)
            postOnMain {
                nextAction.invoke(onDismiss)
            }
        }

        val onAdShownCallback: () -> Unit = {
            mainHandler.removeCallbacksAndMessages(null)
            postOnMain {
                onShown?.invoke()
            }
        }

        if (SPF(activity).is_app_pro) {
            postOnMain { onLoadFailed?.invoke() }
            mainHandler.postDelayed({
                complete()
            }, 2000)
            return
        }

        if (!canShowHigh && !canShowAll) {
            postOnMain { onLoadFailed?.invoke() }
            mainHandler.postDelayed({
                complete()
            }, 3000L)
            return
        }

        val startTimeLoad = System.currentTimeMillis()
        AdsCoroutineScope.scope.launch(Dispatchers.Main) {
            withTimeoutOrNull(6000.milliseconds) {
                AdsApplication.fetchConfigDone
                    .filterNotNull()
                    .first()
            }
            val timeout = ValueRemoteConfigModule.timeout_splash * 1000L
            val elapsed = System.currentTimeMillis() - startTimeLoad
            val remaining = timeout - elapsed

            if (remaining > 0L) {
                delay(remaining.milliseconds)
            }

            overTime = true
            if (mInterAOA != null) {
                showInterAOA(
                    activity,
                    nextAction = { onDismiss -> complete(onDismiss) },
                    onShown = onAdShownCallback,
                    onShowFail = {
                        onShowFail?.invoke()
                    }
                )
            } else {
                postOnMain { onLoadFailed?.invoke() }
                delay(3000.milliseconds)
                complete()
            }
        }

        if (strategy == LoadStrategy.SEQUENTIAL) {
            AdsLog.d(TAG, "loadAndShowInterSplashSequential")
            fun loadSingleAd(adType: String, result: ((success: Boolean) -> Unit)? = null) {
                val adId = if (adType == "high") idHigh else idAllPrice
                val adRequest = AdRequest.Builder(adId).build()
                InterstitialAd.load(
                    adRequest,
                    object : AdLoadCallback<InterstitialAd> {
                        override fun onAdLoaded(ad: InterstitialAd) {

                            AdsLog.d(TAG, "loadAndShowInterSplashSequential onAdLoaded $adType")
                            postOnMain { result?.invoke(true) }
                            ad.setImmersiveMode(true)
                            mInterAOA = ad

                            if (!overTime)
                                showInterAOA(
                                    activity,
                                    nextAction = { onDismiss -> complete(onDismiss) },
                                    onShown = onAdShownCallback,
                                    onShowFail = {
                                        onShowFail?.invoke()
                                    }
                                )
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            AdsLog.d(
                                TAG,
                                "loadAndShowInterSplashSequential onAdFailedToLoad $adType (${error.message})"
                            )
                            postOnMain { result?.invoke(false) }
                            if (adType != "high") {
                                if (!overTime) {
                                    mInterAOA = null
                                    postOnMain { onLoadFailed?.invoke() }
                                    mainHandler.postDelayed({
                                        complete()
                                    }, 3000)
                                }
                            }
                        }
                    }
                )

                AdsLog.d(TAG, "loadAndShowInterSplashSequential $adType: $adId")
            }
            if (canShowHigh) {
                loadSingleAd("high") { success ->
                    if (!success && !overTime && canShowAll)
                        loadSingleAd("all")
                }
            } else {
                loadSingleAd("all")
            }

        } else {
            AdsLog.d(TAG, "preLoadInterSplashParallel")
            var loadHighDone = false
            if (canShowHigh) {
                preLoadInterAOAHigh(activity, idHigh) {
                    loadHighDone = true
                    if (mInterAOA != null && !overTime)
                        showInterAOA(
                            activity,
                            nextAction = { onDismiss -> complete(onDismiss) },
                            onShown = onAdShownCallback,
                            onShowFail = {
                                onShowFail?.invoke()
                            }
                        )
                }
            } else loadHighDone = true

            if (canShowAll) {
                preLoadInterAOAAllPrice(activity, idAllPrice) {
                    if (mInterAOA != null && !overTime && loadHighDone)
                        showInterAOA(
                            activity,
                            nextAction = { onDismiss -> complete(onDismiss) },
                            onShown = onAdShownCallback,
                            onShowFail = {
                                onShowFail?.invoke()
                            }
                        )
                }
            }
        }
    }

    private fun preLoadInterAOAHigh(
        activity: Activity,
        id: String,
        result: (success: Boolean) -> Unit
    ) {

        AdsLog.d(TAG, "preLoadInterAOAHigh")
        val adRequest = AdRequest.Builder(id).build()
        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    AdsLog.d(TAG, "preLoadInterAOAHigh onAdLoaded")
                    interstitialAd.setImmersiveMode(true)
                    mInterAOA = interstitialAd
                    postOnMain { result.invoke(true) }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "preImpAOAHigh onAdFailedToLoad (${loadAdError.message})")
                    postOnMain { result.invoke(false) }
                }
            })
    }

    private fun preLoadInterAOAAllPrice(activity: Activity, id: String, result: () -> Unit) {

        AdsLog.d(TAG, "preLoadInterAOAAllPrice")

        val adRequest = AdRequest.Builder(id).build()
        InterstitialAd.load(
            adRequest,
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    AdsLog.d(TAG, "preLoadInterAOAAllPrice onAdLoaded")
                    interstitialAd.setImmersiveMode(true)
                    if (mInterAOA == null) mInterAOA = interstitialAd
                    postOnMain { result.invoke() }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AdsLog.d(TAG, "preLoadInterAOAAllPrice onAdFailedToLoad (${loadAdError.message})")
                    postOnMain { result.invoke() }
                }
            })
    }

    private fun checkCappingTime(checkCappingTime: Boolean = true): Boolean {
        if (!checkCappingTime) return true
        return System.currentTimeMillis() - lastShowAdFull > ad_full_capping_time
    }

    fun loadAndShowInter(
        activity: Activity,
        adId: String,
        timeDelay: Long = 0L,
        timeOut: Long = 0L,
        nextAction: (onDismiss: Boolean) -> Unit,
        onShown: (() -> Unit)? = null,
        canShowId: Boolean = true,
        onLoadFailed: (() -> Unit)? = null,
        showLoading: Boolean = true,
        checkCappingTime: Boolean = true
    ) {
        var isTimeout = false
        var isTimeDelayDone = false
        var finished = false
        var loadedAd: InterstitialAd? = null
        var loadingDialog: DialogLoadingAds? = null

        val mainHandler = Handler(Looper.getMainLooper())

        fun dismissLoading() {
            postOnMain {
                DialogLoadingAds.dismissLoading(loadingDialog, activity)
                loadingDialog = null
            }
        }

        fun complete(onDismiss: Boolean = false) {
            if (finished) return
            finished = true
            dismissLoading()
            mainHandler.removeCallbacksAndMessages(null)
            postOnMain {
                nextAction.invoke(onDismiss)
            }
        }

        if (SPF(activity).is_app_pro || !activity.isInternetConnected() || !canShowId) {
            complete()
            return
        }

        fun showAd(ad: InterstitialAd) {
            dismissLoading()
            if (activity is LifecycleOwner) {
                val lifecycle = activity.lifecycle
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    lifecycle.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(
                            source: LifecycleOwner,
                            event: Lifecycle.Event
                        ) {
                            if (event == Lifecycle.Event.ON_RESUME) {
                                lifecycle.removeObserver(this)
                                showAd(ad)
                            } else if (event == Lifecycle.Event.ON_DESTROY) {
                                lifecycle.removeObserver(this)
                            }
                        }
                    })
                    return
                }
            }

            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    lastShowAdFull = System.currentTimeMillis()
                    ad.adEventCallback = null
                    complete(true)
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdShowedFullScreenContent() {
                    mainHandler.removeCallbacksAndMessages(null)
                    postOnMain {
                        onShown?.invoke()
                    }
                    inters_ad_view.postFirebaseEvent()
                    StatusShowAd.isInterstitialShown = true
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    ad.adEventCallback = null
                    postOnMain { onLoadFailed?.invoke() }
                    complete()
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdPaid(value: AdValue) {
                    trackingRevenueAd(ad, value)
                }
            }

            if (!activity.isFinishing && !activity.isDestroyed && !StatusShowAd.isInterstitialShown) {
                try {
                    StatusShowAd.isInterstitialShown = true
                    ad.show(activity)
                } catch (_: Exception) {
                    StatusShowAd.isInterstitialShown = false
                    ad.adEventCallback = null
                    postOnMain { onLoadFailed?.invoke() }
                    complete()
                }
            } else {
                ad.adEventCallback = null
                postOnMain { onLoadFailed?.invoke() }
                complete()
            }
        }

        if (checkCappingTime(checkCappingTime)) {
            if (showLoading) {
                loadingDialog = DialogLoadingAds.showLoading(activity)
            }

            if (timeOut > 0) {
                mainHandler.postDelayed({
                    isTimeout = true
                    val ad = loadedAd
                    if (ad != null) {
                        showAd(ad)
                    } else {
                        onLoadFailed?.invoke()
                        complete()
                    }
                }, timeOut)
            }

            mainHandler.postDelayed({
                isTimeDelayDone = true
                val ad = loadedAd
                if (ad != null && !isTimeout) {
                    showAd(ad)
                }
            }, timeDelay)

            val adRequest = AdRequest.Builder(adId).build()
            InterstitialAd.load(
                adRequest,
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        ad.setImmersiveMode(true)
                        loadedAd = ad
                        if (isTimeout) return
                        if (isTimeDelayDone) {
                            showAd(ad)
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        if (isTimeout) return
                        postOnMain { onLoadFailed?.invoke() }
                        complete()
                    }
                }
            )
        } else {
            complete()
        }
    }

}