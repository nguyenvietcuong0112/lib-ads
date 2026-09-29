package com.mobi.libraryads.ads.reward_ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.mobi.libraryads.ads.utils.StatusShowAd
import com.mobi.libraryads.commons.adjust.trackingRevenueAd
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.reward_ad_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.views.dialogs.DialogLoadingAds
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object Reward {

    private var mRewardAd: RewardedAd? = null
    private var mRewardAdId: String = ""
    private var lastShowAdFull = 0L
    private var isLoadingReward = false
    private var retry = 0

    fun rewardAdAlready(): Boolean = mRewardAd != null
    fun loadRewardAd(
        activity: Activity,
        rewardId: String = "",
        onResult: ((success: Boolean) -> Unit)? = null
    ) {
        if (rewardId == "" || mRewardAd != null || isLoadingReward) return
        if (!activity.isInternetConnected() || SPF(activity).is_app_pro) {
            onResult?.invoke(false)
            return
        }
        if (!ValueRemoteConfigModule.reward_ad) {
            onResult?.invoke(false)
            return
        }

        isLoadingReward = true
        mRewardAdId = rewardId
        if (retry >= 3) {
            retry = 0
            return
        }
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity.applicationContext, rewardId, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    rewardedAd.setImmersiveMode(true)
                    mRewardAd = rewardedAd
                    retry = 0
                    isLoadingReward = false
                    onResult?.invoke(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    retry++
                    isLoadingReward = false
                    val delayMillis = TimeUnit.SECONDS.toMillis(
                        2.0.pow(6.coerceAtMost(retry)).toLong()
                    )
                    if (retry >= 3) onResult?.invoke(false)
                    Handler(Looper.getMainLooper())
                        .postDelayed({ loadRewardAd(activity, rewardId) }, delayMillis)
                }
            })
    }

    fun showRewardAd(
        activity: Activity,
        nextAction: () -> Unit,
        onShowSuccess: (() -> Unit)? = null,
        reload: Boolean = false,
        onUserEarnedReward: (() -> Unit)? = null
    ) {
        var handled = false
        fun runNext() {
            if (handled) return
            handled = true
            nextAction.invoke()
        }

        if (SPF(activity).is_app_pro) {
            runNext()
            return
        }
        if (!ValueRemoteConfigModule.reward_ad) {
            runNext()
            return
        }
        val rewardAd = mRewardAd
        if (rewardAd != null) {
            rewardAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    lastShowAdFull = System.currentTimeMillis()
                    rewardAd.fullScreenContentCallback = null
                    mRewardAd = null
                    if (reload) loadRewardAd(activity, mRewardAdId)
                    runNext()
                    StatusShowAd.isRewardAdsShown = false
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    onShowSuccess?.invoke()
                    reward_ad_view.postFirebaseEvent()
                    StatusShowAd.isRewardAdsShown = true
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                    rewardAd.fullScreenContentCallback = null
                    mRewardAd = null
                    runNext()
                    StatusShowAd.isRewardAdsShown = false
                }
            }

            mRewardAd?.let {
                trackingRevenueAd(it)
            }

            if (!activity.isFinishing && StatusShowAd.canShowRewardAd()) {
                try {
                    StatusShowAd.isRewardAdsShown = true
                    rewardAd.show(activity) { rewardItem ->
                        onUserEarnedReward?.invoke()
                    }
                } catch (_: Exception) {
                    StatusShowAd.isRewardAdsShown = false
                    rewardAd.fullScreenContentCallback = null
                    runNext()
                }
            } else {
                rewardAd.fullScreenContentCallback = null
                runNext()
            }
        } else {
            if (reload) loadRewardAd(activity, mRewardAdId)
            runNext()
        }
    }

    fun loadAndShowRewardAd(
        activity: Activity,
        rewardId: String,
        timeOut: Long = 0L,
        onStartLoading: (() -> Unit)? = null,
        onFinishLoading: (() -> Unit)? = null,
        nextAction: () -> Unit,
        onShowSuccess: (() -> Unit)? = null,
        onUserEarnedReward: (() -> Unit)? = null,
        onLoadFailed: (() -> Unit)? = null,
        reload: Boolean = false,
        showLoading: Boolean = true
    ) {
        var handled = false
        var loadingDialog: DialogLoadingAds? = null

        fun dismissLoading() {
            DialogLoadingAds.dismissLoading(loadingDialog, activity)
            loadingDialog = null
        }

        fun runNext() {
            if (handled) return
            handled = true
            dismissLoading()
            nextAction.invoke()
        }

        onStartLoading?.invoke()

        if (SPF(activity).is_app_pro || !activity.isInternetConnected() || !ValueRemoteConfigModule.reward_ad || rewardId.isEmpty()) {
            onFinishLoading?.invoke()
            onLoadFailed?.invoke()
            runNext()
            return
        }

        mRewardAdId = rewardId

        if (mRewardAd != null) {
            onFinishLoading?.invoke()
            showRewardAd(
                activity = activity,
                nextAction = { runNext() },
                onShowSuccess = onShowSuccess,
                reload = reload,
                onUserEarnedReward = onUserEarnedReward
            )
            return
        }

        if (showLoading) {
            loadingDialog = DialogLoadingAds.showLoading(activity)
        }

        if (timeOut > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!handled && mRewardAd == null) {
                    dismissLoading()
                    onFinishLoading?.invoke()
                    onLoadFailed?.invoke()
                    runNext()
                }
            }, timeOut)
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity.applicationContext,
            rewardId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    dismissLoading()
                    rewardedAd.setImmersiveMode(true)
                    mRewardAd = rewardedAd
                    retry = 0
                    onFinishLoading?.invoke()
                    showRewardAd(
                        activity = activity,
                        nextAction = { runNext() },
                        onShowSuccess = onShowSuccess,
                        reload = reload,
                        onUserEarnedReward = onUserEarnedReward
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    dismissLoading()
                    retry++
                    mRewardAd = null
                    onFinishLoading?.invoke()
                    onLoadFailed?.invoke()
                    runNext()
                }
            }
        )
    }
}