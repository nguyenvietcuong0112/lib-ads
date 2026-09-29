package com.mobi.libraryads.views.splash

import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mobi.libraryads.AdsApplication.Companion.isFirstOpen
import com.mobi.libraryads.AdsApplication.Companion.isSession2
import com.mobi.libraryads.AdsCoroutineScope
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.FOConfigs.isOrganic
import com.mobi.libraryads.FOConfigs.splashConfig
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.banner_ads.Banner
import com.mobi.libraryads.ads.inter_ads.Inter
import com.mobi.libraryads.ads.inter_ads.InterAdModel
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.consents.GoogleMobileAdsConsentManager
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.banner_splash_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule.inter_splash
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule.inter_splash_high
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.TaskCoordinator
import com.mobi.libraryads.commons.utils.isInternetConnected
import com.mobi.libraryads.commons.utils.openActivityAndClearApp
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible
import com.mobi.libraryads.databinding.FrameLayoutBinding
import com.mobi.libraryads.views.base.BaseActivity
import com.mobi.libraryads.views.language.LanguageActivity
import com.mobi.libraryads.views.onboarding.OnboardingActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds


class SplashActivity : BaseActivity<FrameLayoutBinding>() {

    override fun inflateVB(inflater: LayoutInflater): FrameLayoutBinding {
        return FrameLayoutBinding.inflate(inflater)
    }

    override fun preLoadData() {
        super.preLoadData()
        splashConfig.uiSplashConfig.activityCallBack?.onStartActivity(this, isSession2)
    }


    override fun initView() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Disable back
                }
            })

        val viewCustom = layoutInflater.inflate(
            splashConfig.uiSplashConfig.resLayout,
            binding.root,
            true
        )
        checkUMP()
        loadUISplash()
    }

    private var player: ExoPlayer? = null
    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.volume = 0f
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }

    @OptIn(UnstableApi::class)
    private fun loadUISplash() {
        val videoView = findViewById<PlayerView>(R.id.videoView) ?: return
        if (splashConfig.uiSplashConfig.videos.isEmpty()) {
            videoView.setGone()
            return
        }
        try {
            videoView.setVisible()
            val video = splashConfig.uiSplashConfig.videos.random()
            val uri = RawResourceDataSource.buildRawResourceUri(video)

            val renderersFactory = DefaultRenderersFactory(this@SplashActivity)
                .setEnableDecoderFallback(true)
            player = ExoPlayer.Builder(this, renderersFactory).build()
            videoView.setPlayer(player)
            player?.repeatMode = Player.REPEAT_MODE_ALL
            player?.setMediaItem(MediaItem.fromUri(uri))
            player?.prepare()
        } catch (_: Exception) {
        }
    }

    private var umpStarted = false
    private var loadAdsRep: Job? = null
    private fun checkUMP() {
        if (!isInternetConnected()) return
        if (isFinishing || isDestroyed) return
        if (umpStarted) return
        umpStarted = true
        val consentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        consentManager.gatherConsent(this@SplashActivity) {
            if (isFinishing || isDestroyed) return@gatherConsent
            val canRequestAds = consentManager.canRequestAds
            Log.d("AdMob", "gatherConsent done $canRequestAds")
            if (canRequestAds)
                loadAdsRep = lifecycleScope.launch {
                    repeat(10) {index ->
                        startLoadAds(index+1)
                        delay(1000.milliseconds)
                    }
                }
            else {
                mHandler.postDelayed({ goNext() }, 2000)
            }
            //showBanner
            showBannerSplash()
        }
    }

    private val isStartLoadAds = AtomicBoolean(false)
    private fun startLoadAds(index: Int) {
        if (!isInternetConnected()) return
        if (isStartLoadAds.getAndSet(true)) return
        loadAdsRep?.cancel()
        if (SPF(this).is_app_pro) {
            mHandler.postDelayed({ goNext() }, 2000)
            return
        }
        //preload Inter
        loadAndShowInterSplash()
        //preloadNativeFullInter
        loadNativeFullInter()
        //preloadAdsHome
        if (!FOConfigs.isLoadAdsPerScreen) {
            splashConfig.uiSplashConfig.activityCallBack?.onStartLoadAds(this, isSession2)
        }
        if (!FOConfigs.languageConfig.adsLanguageConfig.preloadAdsLater) preloadAds()
    }


    private var preloadAdsStarted = false
    private fun preloadAds() {
        if (FOConfigs.isLoadAdsPerScreen || !splashConfig.adsSplashConfig.preloadNextScreenAds) return
        if (isSession2 && !splashConfig.uiSplashConfig.showFOForever) return
        if (preloadAdsStarted) return
        preloadAdsStarted = true

        // preload Lang
        if (!isSession2
            || (splashConfig.uiSplashConfig.showFOForever
                    && FOConfigs.languageConfig.uiLanguageConfig.showInSession2)
        )
            preloadNativeLanguage()

        //preload OB
        if (!FOConfigs.obConfig.adsOBConfig.isLoadNativeOBInLanguage
            && !FOConfigs.obConfig.adsOBConfig.isLoadNativeOBInOnboarding
        ) {
            preloadNativeOB()
        }
    }

    private fun loadNativeFullInter() {
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
            awaitFetchConfig()
            val canShowInter = inter_splash || inter_splash_high
            if (!canShowInter || (ValueRemoteConfigModule.check_organic_user && isOrganic)) return@launch
            val nativeAllId = if (isFirstOpen) splashConfig.adsSplashConfig.nativeFullId
            else splashConfig.adsSplashConfig.nativeFullIdS2
            val nativeHighId = if (isFirstOpen) splashConfig.adsSplashConfig.nativeFullHighId
            else splashConfig.adsSplashConfig.nativeFullHighIdS2
            NativeManager.preloadNativeWithHigh(
                context = applicationContext,
                adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position,
                adId = nativeAllId,
                idHigh = nativeHighId,
                strategy = splashConfig.adsSplashConfig.loadNativeFullStrategy,
                canShowIdAll = ValueRemoteConfigModule.native_full_inter_splash,
                canShowIdHigh = ValueRemoteConfigModule.native_full_high_inter_splash
            )
        }
    }

    private fun loadAndShowInterSplash() {
        Log.d("AdMob", "Start loadAndShowInterSplash")
        Inter.loadAndShowInterSplash(
            activity = this,
            idHigh = if (isFirstOpen) splashConfig.adsSplashConfig.interHighId
            else splashConfig.adsSplashConfig.interHighIdS2,
            idAllPrice = if (isFirstOpen) splashConfig.adsSplashConfig.interAllId
            else splashConfig.adsSplashConfig.interAllIdS2,
            strategy = splashConfig.adsSplashConfig.loadInterStrategy,
            nextAction = {
                coordinator.onTaskADone()
                NativeManager.startFullscreenCountdown(EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position)
            },
            onShown = {
                coordinator.requireTaskB()
                val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
                if (!isOrganicUser)
                    mHandler.postDelayed(
                        { showNativeFullInter() },
                        1000
                    ) //k bị nháy native full nếu k có loadingview của inter
                else coordinator.onTaskBDone()
                // preload Lang
                if (FOConfigs.languageConfig.adsLanguageConfig.preloadAdsLater) preloadAds()
            },
            onShowFail = {
                // preload Lang
                if (FOConfigs.languageConfig.adsLanguageConfig.preloadAdsLater) preloadAds()
            },
            onLoadFailed = {
                // preload Lang
                if (FOConfigs.languageConfig.adsLanguageConfig.preloadAdsLater) preloadAds()
            }
        )
    }

    private fun showNativeFullInter() {
        NativeManager.showFullscreenNativeWithCountdownWithHigh(
            activity = this@SplashActivity,
            canShowIdAll = ValueRemoteConfigModule.native_full_inter_splash,
            canShowIdHigh = ValueRemoteConfigModule.native_full_high_inter_splash,
            adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_SPLASH.position,
            adLayoutRes = splashConfig.adsSplashConfig.nativeFullLayout,
            countdownSeconds = ValueRemoteConfigModule.countdown_native_full_inter,
            dismissOnAdClick = splashConfig.uiSplashConfig.dismissNativeFullOnAdClick,
            onDismissed = {
                coordinator.onTaskBDone()
            },
            startCountdownImmediately = false,
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String, isHighFloor: Boolean) {
                    EventsNameFirebase.native_full_inter_splash_view.postFirebaseEvent()
                    if (isHighFloor) EventsNameFirebase.native_full_inter_splash_high_view.postFirebaseEvent()
                }
            }
        )
    }

    private fun showBannerSplash() {
        val layoutAds = findViewById<FrameLayout>(R.id.layoutAds) ?: return
        AdsCoroutineScope.scope.launch(Dispatchers.Main) {
//            awaitFetchConfig()
            Banner.requestBanner(
                activity = this@SplashActivity,
                id = if (isFirstOpen) splashConfig.adsSplashConfig.bannerId
                else splashConfig.adsSplashConfig.bannerIdS2,
                adFrame = layoutAds,
                onShown = {
                    banner_splash_view.postFirebaseEvent()
                },
                canShowAd = ValueRemoteConfigModule.banner_splash
            )
        }
    }

    val coordinator = TaskCoordinator {
        goNext()
    }

    private fun goNext() {
        if (isFinishing || isDestroyed) return
        if (SPF(this).is_app_pro) {
            openActivityAndClearApp(splashConfig.uiSplashConfig.homeActivity)
            splashConfig.uiSplashConfig.activityCallBack?.onNextActivity(this, isSession2)
            return
        }
        val shouldShowLanguage = !isSession2
                || (splashConfig.uiSplashConfig.showFOForever
                && FOConfigs.languageConfig.uiLanguageConfig.showInSession2)

        if (shouldShowLanguage)
            openActivityAndClearApp(LanguageActivity::class.java)
        else {
            if (isSession2 && splashConfig.uiSplashConfig.showFOForever
                && !FOConfigs.languageConfig.uiLanguageConfig.showInSession2
            )
                openActivityAndClearApp(OnboardingActivity::class.java)
            else
                openActivityAndClearApp(splashConfig.uiSplashConfig.homeActivity)
        }
        splashConfig.uiSplashConfig.activityCallBack?.onNextActivity(this, isSession2)
//        finish()
    }

    private var onConnectNum = 0
    override fun onConnected() {
        super.onConnected()
        if (onConnectNum > 0) {
            checkUMP()
            fetchRemoteConfigAgain()
        }
        onConnectNum++
    }

    override fun onDisconnected() {
        super.onDisconnected()
        onConnectNum++
    }

    private var fetchOnce = false
    private fun fetchRemoteConfigAgain() {
        if (fetchOnce) return
        fetchOnce = true
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
            val fetchDone = awaitFetchConfig()
            if (fetchDone == null || !fetchDone) {
                FirebaseRemoteConfig.getInstance().fetchAndActivate()
            }
        }
    }


    private fun preloadNativeLanguage() {
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
            awaitFetchConfig()
            FOConfigs.languageConfig.adsLanguageConfig.apply {
                NativeManager.preloadNativeWithHigh(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_LANGUAGE.position,
                    adId = if (isFirstOpen) nativeLangId else nativeLangIdS2,
                    idHigh = if (isFirstOpen) nativeLangHighId else nativeLangHighIdS2,
                    strategy = loadStrategy,
                    canShowIdAll = ValueRemoteConfigModule.native_language,
                    canShowIdHigh = ValueRemoteConfigModule.native_language_high,
                )
                delay(200.milliseconds)
                NativeManager.preloadNativeWithHigh(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position,
                    adId = if (isFirstOpen) nativeLangClickId else nativeLangClickIdS2,
                    idHigh = if (isFirstOpen) nativeLangClickHighId else nativeLangClickHighIdS2,
                    strategy = loadStrategy,
                    canShowIdAll = ValueRemoteConfigModule.native_language_click,
                    canShowIdHigh = ValueRemoteConfigModule.native_language_high_click,
                )
            }
        }
    }

    private fun preloadNativeOB() {
        if (isSession2 && !splashConfig.uiSplashConfig.showFOForever) return
        val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
            awaitFetchConfig()
            FOConfigs.obConfig.adsOBConfig.apply {
                delay(200.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_1.position,
                    adId = if (isFirstOpen) nativeOB1Id else nativeOB1IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_1
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position,
                    adId = if (isFirstOpen) nativeOBFull12Id else nativeOBFull12IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_full_1_2
                            && !isOrganicUser
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_2.position,
                    adId = if (isFirstOpen) nativeOB2Id else nativeOB2IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_2
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position,
                    adId = if (isFirstOpen) nativeOBFull23Id else nativeOBFull23IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_full_2_3
                            && !isOrganicUser
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_3.position,
                    adId = if (isFirstOpen) nativeOB3Id else nativeOB3IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_3
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position,
                    adId = if (isFirstOpen) nativeOBFull34Id else nativeOBFull34IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_full_3_4
                            && !isOrganicUser
                )
                delay(100.milliseconds)
                NativeManager.preloadNative(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_ONBOARDING_4.position,
                    adId = if (isFirstOpen) nativeOB4Id else nativeOB4IdS2,
                    canShowAd = ValueRemoteConfigModule.native_onboarding_4
                )
            }
        }
        AdsCoroutineScope.scope.launch(Dispatchers.Main) {
            delay(200.milliseconds)
            Inter.preLoadInter(
                activity = applicationContext,
                adModel = InterAdModel(
                    name = EnumAdsNamePosition.INTER_ONBOARDING.position,
                    id = if (isFirstOpen) FOConfigs.obConfig.adsOBConfig.interOBId
                    else FOConfigs.obConfig.adsOBConfig.interOBIdS2
                ),
                canShowAd = ValueRemoteConfigModule.inter_onboarding && !isOrganicUser
            )
        }
    }
}