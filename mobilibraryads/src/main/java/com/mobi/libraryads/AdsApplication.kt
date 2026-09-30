package com.mobi.libraryads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.mobi.libraryads.commons.tracking.AdTrackingManager
import com.mobi.libraryads.commons.tracking.IAdTracker
import com.mobi.libraryads.commons.tracking.impl.AdjustAdTracker
import com.mobi.libraryads.commons.tracking.impl.FirebaseAdTracker
import com.mobi.libraryads.FOConfigs.isOrganic
import com.mobi.libraryads.FOConfigs.languageConfig
import com.mobi.libraryads.FOConfigs.splashConfig
import com.mobi.libraryads.ads.open_ads.OpenAds
import com.mobi.libraryads.commons.GSM.GSMUtil
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.KonfigModel
import com.mobi.libraryads.commons.remote.RemoteKonfig
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.InternetConnectionObserver
import com.mobi.libraryads.commons.utils.getAppVersion
import com.mobi.libraryads.data.LanguageConfig
import com.mobi.libraryads.data.OBConfig
import com.mobi.libraryads.data.SplashConfig
import com.mobi.libraryads.views.language.LanguageActivity
import com.mobi.libraryads.views.splash.SplashActivity
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.firebase.Firebase
import com.google.firebase.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdsApplication(
    private val globalClass: Application,
    var remoteKeys: KonfigModel? = null,
    var isDebug: Boolean = false,
    var appId: String = ""
) : Application.ActivityLifecycleCallbacks,
    LifecycleObserver {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val resolvedAppId = if (appId.isNotBlank()) {
                appId
            } else {
                try {
                    val appInfo = globalClass.packageManager.getApplicationInfo(
                        globalClass.packageName,
                        android.content.pm.PackageManager.GET_META_DATA
                    )
                    appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID") ?: ""
                } catch (_: Exception) {
                    ""
                }
            }
            val config = InitializationConfig.Builder(resolvedAppId)
                .setNativeValidatorDisabled()
                .build()
            MobileAds.initialize(globalClass, config) {
                Log.d("AdMob", "MobileAds Next-Gen initialized")
            }
        }
        AdsLog.init(globalClass, isDebug)

        globalClass.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        initFirebase()
        try {
            InternetConnectionObserver.init(globalClass)
        } catch (_: Exception) {
        }
    }


    private fun loginGSM(gsmAppId: String) {
        if (gsmAppId.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            GSMUtil.retryLoginGSM = 0
            GSMUtil.getConfig(globalClass, gsmAppId)
            GSMUtil.login(globalClass, gsmAppId, getAppVersion(globalClass))
        }
    }

    companion object {
        private val _fetchConfigDone = MutableStateFlow<Boolean?>(null)
        val fetchConfigDone = _fetchConfigDone.asStateFlow()
        var isSession2 = false
        var isFirstOpen = true
    }

    private fun initFirebase() {
        Firebase.initialize(globalClass)
        _fetchConfigDone.value = null
        RemoteKonfig.initialize(
            block = {
                minimumFetchIntervalInSeconds = 60
                registerModels(ValueRemoteConfigModule)
                remoteKeys?.let {
                    registerModels(it)
                }
            },
            fetchDone = { success ->
                _fetchConfigDone.value = success
            })
    }

    private var OpenAds: OpenAds? = null
    fun initSdk(
        adjustAppToken: String = "",
        gsmAppId: String = "",
        splashConfig: SplashConfig,
        languageConfig: LanguageConfig,
        obConfig: OBConfig,
        loadAdsPerScreen: Boolean = false,
        trackers: List<IAdTracker>? = null
    ) {
        val resolvedTrackers = trackers ?: run {
            val list = mutableListOf<IAdTracker>()
            list.add(FirebaseAdTracker())
            if (adjustAppToken.isNotBlank()) {
                list.add(AdjustAdTracker(adjustAppToken))
            }
            list
        }
        AdTrackingManager.setTrackers(resolvedTrackers)
        AdTrackingManager.init(globalClass, isDebug)

        loginGSM(gsmAppId)
        FOConfigs.init(splashConfig, languageConfig, obConfig, loadAdsPerScreen)
        FOConfigs.isOrganic = SPF(globalClass).is_organic
        OpenAds = OpenAds(globalClass)
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        if (p0 is SplashActivity) {
            val session = SPF(globalClass).count_session_app
            isFirstOpen = session == 0L
            isSession2 = SPF(globalClass).is_second_time_open_app
            SPF(globalClass).count_session_app++

            val sessionBundle = Bundle().apply {
                putLong("session_count", session + 1)
            }
            "app_open_session".postFirebaseEvent(sessionBundle)
            Log.d("AdMob", "AdsApplication session_${session + 1}")
            if (isSession2 &&
                (!splashConfig.uiSplashConfig.showFOForever
                        || !languageConfig.uiLanguageConfig.showInSession2)
            ) {
                OpenAds?.preloadAOA()
            }
        }
        if (p0 is LanguageActivity) {
            if (!isSession2 ||
                (splashConfig.uiSplashConfig.showFOForever
                        && languageConfig.uiLanguageConfig.showInSession2)
            ) {
                OpenAds?.preloadAOA()
            }
        }
    }

    override fun onActivityStarted(p0: Activity) {
    }

    override fun onActivityResumed(p0: Activity) {
        AdTrackingManager.onActivityResume(p0)
    }

    override fun onActivityPaused(p0: Activity) {
        AdTrackingManager.onActivityPause(p0)
    }

    override fun onActivityStopped(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(p0: Activity) {

    }
}