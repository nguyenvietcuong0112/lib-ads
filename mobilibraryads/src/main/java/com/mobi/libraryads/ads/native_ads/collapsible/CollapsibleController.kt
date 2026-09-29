package com.mobi.libraryads.ads.native_ads.collapsible

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mobi.libraryads.ads.utils.StatusShowAd
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.commons.utils.AdsLog
import java.util.concurrent.ConcurrentHashMap

class CollapsibleController {

    private val TAG = "CollapsibleController"
    private val tasks = ConcurrentHashMap<Activity, RefreshTask>()

    fun start(
        activity: AppCompatActivity,
        refreshTime: Long,
        adName: String,
        onRefreshNeeded: () -> Unit
    ) {
        stop(activity)

        val handler = Handler(Looper.getMainLooper())
        var lastResumeTime = System.currentTimeMillis()

        fun canRefreshAd(): Boolean {
            return !StatusShowAd.isOpenAdShown && 
                   !StatusShowAd.isInterstitialShown && 
                   !StatusShowAd.isRewardAdsShown &&
                   !NativeManager.isFullscreenAdShowing(adName)
        }

        val runnable = object : Runnable {
            override fun run() {
                if (canRefreshAd()) {
                    AdsLog.d(TAG, "Collapsible refresh timer triggered. Refreshing ad...")
                    onRefreshNeeded()
                    if (refreshTime > 0) {
                        handler.postDelayed(this, refreshTime)
                    }
                } else {
                    AdsLog.d(TAG, "Collapsible refresh delayed because another ad (Inter/Open/Reward) is currently shown.")
                    // Retry checking after 5 seconds
                    handler.postDelayed(this, 5000L)
                }
            }
        }

        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                AdsLog.d(TAG, "Activity onPause: Pausing collapsible refresh timer.")
                handler.removeCallbacks(runnable)
            }

            override fun onResume(owner: LifecycleOwner) {
                handler.removeCallbacks(runnable)
                val currentTime = System.currentTimeMillis()
                
                AdsLog.d(TAG, "Activity onResume: Checking for collapsible refresh.")
                
                if (currentTime - lastResumeTime > 5000L) {
                    if (canRefreshAd()) {
                        AdsLog.d(TAG, "onResume: Triggering refresh immediately (last resume was >5s ago).")
                        onRefreshNeeded()
                        if (refreshTime > 0) {
                            handler.postDelayed(runnable, refreshTime)
                        }
                    } else {
                        AdsLog.d(TAG, "onResume: Delaying refresh because another ad is shown.")
                        handler.postDelayed(runnable, 5000L)
                    }
                } else {
                    if (refreshTime > 0) {
                        handler.postDelayed(runnable, refreshTime)
                    }
                }
                lastResumeTime = currentTime
            }

            override fun onDestroy(owner: LifecycleOwner) {
                AdsLog.d(TAG, "Activity onDestroy: Cleaning up collapsible controller.")
                stop(activity)
            }
        }

        activity.lifecycle.addObserver(observer)
        tasks[activity] = RefreshTask(handler, runnable, observer)

        // Run the first time
        if (canRefreshAd()) {
            onRefreshNeeded()
        }
    }

    fun stop(activity: Activity) {
        tasks.remove(activity)?.let { task ->
            task.handler.removeCallbacks(task.runnable)
            if (activity is AppCompatActivity) {
                activity.lifecycle.removeObserver(task.observer)
            }
        }
    }

    fun stopAll() {
        for (activity in tasks.keys) {
            stop(activity)
        }
        tasks.clear()
    }

    private data class RefreshTask(
        val handler: Handler,
        val runnable: Runnable,
        val observer: DefaultLifecycleObserver
    )
}
