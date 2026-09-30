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
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

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
        val aoaId = FOConfigs.splashConfig.adsSplashConfig.admobAOAId
        if (SPF(globalClass).is_app_pro
            || !ValueRemoteConfigModule.resume_open_app
            || aoaId.isBlank()
            || isAdAvailable()
            || isLoading
        ) return

        val request = AdRequest.Builder(aoaId).build()
        isLoading = true

        AppOpenAd.load(
            request,
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.setImmersiveMode(true)
                    mAppOpenAd = ad
                    isLoading = false
                    Log.d("AdMob", "preloadAOA onAdLoaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    Log.d("AdMob", "preloadAOA onAdFailedToLoad: ${error.message}")
                }
            }
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

        val appOpenAd = mAppOpenAd ?: return

        appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                mAppOpenAd = null
                StatusShowAd.resetAdStatuses()
                preloadAOA()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                StatusShowAd.resetAdStatuses()
                preloadAOA()
            }

            override fun onAdShowedFullScreenContent() {
                isOpenAdShown = true
                resume_open_app_view.postFirebaseEvent()
            }

            override fun onAdPaid(value: AdValue) {
                trackingRevenueAd(appOpenAd, value)
            }
        }
        appOpenAd.show(activity)
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