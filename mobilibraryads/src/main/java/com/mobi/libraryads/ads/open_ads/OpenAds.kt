package com.mobi.libraryads.ads.open_ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.ads.utils.StatusShowAd
import com.mobi.libraryads.ads.utils.StatusShowAd.canShowOpenAd
import com.mobi.libraryads.ads.utils.StatusShowAd.ignoreAOA
import com.mobi.libraryads.ads.utils.StatusShowAd.isOpenAdShown
import com.mobi.libraryads.commons.adjust.trackingRevenueAd
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.resume_open_app_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.sharepreference.SPF
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback

class OpenAds(private val globalClass: Application) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var mAppOpenAd: AppOpenAd? = null

    private var currentActivity: Activity? = null


    init {
        globalClass.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    fun disableShowAOA() {
        ignoreAOA = true
    }

    fun enableShowAOA() {
        ignoreAOA = false
    }

    private var isLoading = false

    fun preloadAOA() {
        if (SPF(globalClass).is_app_pro
            || !ValueRemoteConfigModule.resume_open_app
            || FOConfigs.splashConfig.adsSplashConfig.admobAOAId == ""
            || isAdAvailable()
            || isLoading
        ) return

        val request: AdRequest = getAdRequest()
        val loadCallback: AppOpenAdLoadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                ad.setImmersiveMode(true)
                mAppOpenAd = ad
                isLoading = false
                Log.d("AdMob", "preloadAOA onAdLoaded")
            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                isLoading = false
                Log.d("AdMob", "preloadAOA onAdFailedToLoad")
            }
        }
        isLoading = true

        AppOpenAd.load(
            globalClass,
            FOConfigs.splashConfig.adsSplashConfig.admobAOAId,
            request,
            loadCallback
        )

        Log.d("AdMob", "preloadAOA startload")
    }


    private fun showAOA(activity: Activity) {
        if (SPF(globalClass).is_app_pro
            || !ValueRemoteConfigModule.resume_open_app
            || !canShowOpenAd()
            || activity.localClassName.lowercase().contains("splash")
        ) return
        if (!isAdAvailable()) {
            preloadAOA()
            return
        }

        mAppOpenAd?.let {
            trackingRevenueAd(it)
        }

        mAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                mAppOpenAd = null
                StatusShowAd.resetAdStatuses()
                preloadAOA()
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                super.onAdFailedToShowFullScreenContent(p0)
                StatusShowAd.resetAdStatuses()
                preloadAOA()
            }

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                isOpenAdShown = true
                resume_open_app_view.postFirebaseEvent()
            }
        }
        mAppOpenAd?.show(activity)

    }

    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    private fun isAdAvailable(): Boolean {
        return mAppOpenAd != null
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (activity.application !== globalClass) {
            Log.d(
                "AOA",
                "Ignore foreign Activity: ${activity.packageName}"
            )
            return
        }
        if (!isOpenAdShown) currentActivity = activity
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {

    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                showAOA(activity)
            }
        }
    }
}