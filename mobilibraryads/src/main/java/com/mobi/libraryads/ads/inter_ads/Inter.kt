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
import com.adjust.sdk.sig.c
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

object Inter {

    private var mInterAds = ConcurrentHashMap<String, InterAdModel>()
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
                            showInter(activity, adName, nextAction, onShown, preload, canShowAd)
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
            nextAction.invoke(onDismiss)
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

            trackingRevenueAd(interAd)

            interAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    lastShowAdFull = System.currentTimeMillis()
                    interAd.fullScreenContentCallback = null

                    adModel.interAd = null
                    if (preload) preLoadInter(activity, adModel, canShowAd)
                    runNext(true)
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    onShown?.invoke()
                    inters_ad_view.postFirebaseEvent()
                    AdsLog.d(
                        TAG,
                        "showInter: onAdShowedFullScreenContent = $adName"
                    )
                    StatusShowAd.isInterstitialShown = true
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                    interAd.fullScreenContentCallback = null
                    AdsLog.d(
                        TAG,
                        "showInter: onAdFailedToShowFullScreenContent = $adName "
                    )
                    adModel.interAd = null
                    runNext()
                    StatusShowAd.isInterstitialShown = false
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
                    interAd.fullScreenContentCallback = null
                    runNext()
                }
            } else {
                interAd.fullScreenContentCallback = null
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
            DialogLoadingAds.dismissLoading(loadingDialog, activity)
            loadingDialog = null
        }

        var handled = false

        fun runNext(onDismiss: Boolean = false) {
            if (handled) return
            handled = true
            dismissLoading()
            nextAction.invoke(onDismiss)
        }

        if (SPF(activity).is_app_pro
            || inForceUpdate
            || mInterAOA == null
            || (!ValueRemoteConfigModule.inter_splash_high
                    && !ValueRemoteConfigModule.inter_splash)
        ) {
            onShowFail?.invoke()
            runNext()
            return
        }

        mInterAOA?.let {
            trackingRevenueAd(it)
        }
        val isType =
            if (mInterAOA?.adUnitId == FOConfigs.splashConfig.adsSplashConfig.interHighId
                || mInterAOA?.adUnitId == FOConfigs.splashConfig.adsSplashConfig.interHighIdS2
            ) {
                "interHigh"
            } else {
                "interAll"
            }
        AdsLog.d(TAG, "showInterAOA: call show function $isType id = ${mInterAOA?.adUnitId}")


        mInterAOA?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                lastShowAdFull = System.currentTimeMillis()
                mInterAOA?.fullScreenContentCallback = null
                mInterAOA = null
                runNext(true)
                StatusShowAd.isInterstitialShown = false
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                onShown?.invoke()
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

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                mInterAOA?.fullScreenContentCallback = null
                AdsLog.d(TAG, "showInterAOA: onAdFailedToShowFullScreenContent $isType ")
                mInterAOA = null
                onShowFail?.invoke()
                runNext()
                StatusShowAd.isInterstitialShown = false
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
                    mInterAOA?.show(activity)
                    StatusShowAd.isInterstitialShown = true
                } else {
                    StatusShowAd.isInterstitialShown = false
                    mInterAOA?.fullScreenContentCallback = null
                    onShowFail?.invoke()
                    runNext()
                }
            }, delayShowLoading)
            AdsLog.d(TAG, "showInterAOA: show $isType id = ${mInterAOA?.adUnitId}")
        } catch (_: Exception) {
            StatusShowAd.isInterstitialShown = false
            mInterAOA?.fullScreenContentCallback = null
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
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity.applicationContext, currentModel.id, adRequest,
            object : InterstitialAdLoadCallback() {
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
                        "preLoadInter: onAdFailedToLoad $activity -- adModel = ${adModel.name}"
                    )
                }
            })
    }


    private val TAG = "Inter "
    private var lastShowAdFull = 0L

    private var mInterAOA: InterstitialAd? = null

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
            nextAction.invoke(onDismiss)
        }

        val onAdShownCallback: () -> Unit = {
            mainHandler.removeCallbacksAndMessages(null)
            onShown?.invoke()
        }

        if (SPF(activity).is_app_pro) {
            onLoadFailed?.invoke()
            mainHandler.postDelayed({
                complete()
            }, 2000)
            return
        }

        if (!canShowHigh && !canShowAll) {
            onLoadFailed?.invoke()
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
                onLoadFailed?.invoke()
                delay(3000.milliseconds)
                complete()
            }
        }

        if (strategy == LoadStrategy.SEQUENTIAL) {
            AdsLog.d(TAG, "loadAndShowInterSplashSequential")
            fun loadSingleAd(adType: String, result: ((success: Boolean) -> Unit)? = null) {
                val adId = if (adType == "high") idHigh else idAllPrice
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    activity.applicationContext,
                    adId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {

                            AdsLog.d(TAG, "loadAndShowInterSplashSequential onAdLoaded $adType")
                            result?.invoke(true)
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
                                "loadAndShowInterSplashSequential onAdFailedToLoad $adType"
                            )
                            result?.invoke(false)
                            if (adType != "high") {
                                if (!overTime) {
                                    mInterAOA = null
                                    onLoadFailed?.invoke()
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
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity.applicationContext, id, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    AdsLog.d(TAG, "preLoadInterAOAHigh onAdLoaded")
                    interstitialAd.setImmersiveMode(true)
                    mInterAOA = interstitialAd
                    result.invoke(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d(TAG, "preImpAOAHigh onAdFailedToLoad")
                    result.invoke(false)
                }
            })
    }

    private fun preLoadInterAOAAllPrice(activity: Activity, id: String, result: () -> Unit) {

        AdsLog.d(TAG, "preLoadInterAOAAllPrice")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity.applicationContext, id, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    AdsLog.d(TAG, "preLoadInterAOAAllPrice onAdLoaded")
                    interstitialAd.setImmersiveMode(true)
                    if (mInterAOA == null) mInterAOA = interstitialAd
                    result.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AdsLog.d(TAG, "preLoadInterAOAAllPrice onAdFailedToLoad")
                    result.invoke()
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
            DialogLoadingAds.dismissLoading(loadingDialog, activity)
            loadingDialog = null
        }

        fun complete(onDismiss: Boolean = false) {
            if (finished) return
            finished = true
            dismissLoading()
            mainHandler.removeCallbacksAndMessages(null)
            nextAction.invoke(onDismiss)
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

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    lastShowAdFull = System.currentTimeMillis()
                    ad.fullScreenContentCallback = null
                    complete(true)
                    StatusShowAd.isInterstitialShown = false
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    mainHandler.removeCallbacksAndMessages(null)
                    onShown?.invoke()
                    inters_ad_view.postFirebaseEvent()
                    StatusShowAd.isInterstitialShown = true
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                    ad.fullScreenContentCallback = null
                    onLoadFailed?.invoke()
                    complete()
                    StatusShowAd.isInterstitialShown = false
                }
            }

            trackingRevenueAd(ad)

            if (!activity.isFinishing && !activity.isDestroyed && !StatusShowAd.isInterstitialShown) {
                try {
                    StatusShowAd.isInterstitialShown = true
                    ad.show(activity)
                } catch (_: Exception) {
                    StatusShowAd.isInterstitialShown = false
                    ad.fullScreenContentCallback = null
                    onLoadFailed?.invoke()
                    complete()
                }
            } else {
                ad.fullScreenContentCallback = null
                onLoadFailed?.invoke()
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

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(
                activity.applicationContext,
                adId,
                adRequest,
                object : InterstitialAdLoadCallback() {
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
                        onLoadFailed?.invoke()
                        complete()
                    }
                }
            )
        } else {
            complete()
        }
    }

}