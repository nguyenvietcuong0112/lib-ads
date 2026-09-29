package com.mobi.libraryads.views.onboarding

import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.mobi.libraryads.AdsApplication.Companion.isFirstOpen
import com.mobi.libraryads.AdsApplication.Companion.isSession2
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.FOConfigs.isOrganic
import com.mobi.libraryads.FOConfigs.splashConfig
import com.mobi.libraryads.ads.inter_ads.Inter
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.databinding.ActivityOnboardingBinding
import com.mobi.libraryads.views.adapters.ViewPagerAdapter
import com.mobi.libraryads.views.base.BaseActivity
import com.mobi.libraryads.views.fragments.FragmentOnboarding1
import com.mobi.libraryads.views.fragments.FragmentOnboarding2
import com.mobi.libraryads.views.fragments.FragmentOnboarding3
import com.mobi.libraryads.views.fragments.FragmentOnboarding4
import com.mobi.libraryads.views.fragments.FragmentOnboardingAdFull
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.AdsCoroutineScope
import com.mobi.libraryads.ads.inter_ads.InterAdModel
import com.mobi.libraryads.commons.utils.TaskCoordinator
import com.mobi.libraryads.commons.utils.openActivityAndClearApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    override fun inflateVB(inflater: LayoutInflater): ActivityOnboardingBinding {
        return ActivityOnboardingBinding.inflate(inflater)
    }

    companion object {
        var isStartOB = false
    }

    override fun initView() {

        val shouldLoadOBOnThisScreen = FOConfigs.isLoadAdsPerScreen || FOConfigs.obConfig.adsOBConfig.isLoadNativeOBInOnboarding
        if (shouldLoadOBOnThisScreen) {
            loadOnboardingAds()
        }

        FOConfigs.obConfig.uiOBConfig.activityCallback?.onStartActivity(this, isSession2)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Disable back
                }
            })
        val mAdapter = ViewPagerAdapter(this)
        val hasFourSlides =
            FOConfigs.obConfig.uiOBConfig.resFragmentOB4 != FOConfigs.obConfig.uiOBConfig.resFragmentOB3

        val isAdValid12 = if (shouldLoadOBOnThisScreen) {
            !NativeManager.isAdFailed(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position)
        } else {
            NativeManager.isAdReady(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position)
                    || !NativeManager.isLoadDone(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2.position)
        }
        val showFullAd12 = ValueRemoteConfigModule.native_onboarding_full_1_2
                && ((FOConfigs.obConfig.adsOBConfig.nativeOBFull12Id.isNotBlank() && isFirstOpen)
                || (FOConfigs.obConfig.adsOBConfig.nativeOBFull12IdS2.isNotBlank() && !isFirstOpen))
                && (!ValueRemoteConfigModule.check_organic_user || (ValueRemoteConfigModule.check_organic_user && !isOrganic))
                && isAdValid12

        val isAdValid23 = if (shouldLoadOBOnThisScreen) {
            !NativeManager.isAdFailed(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position)
        } else {
            NativeManager.isAdReady(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position)
                    || !NativeManager.isLoadDone(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3.position)
        }
        val showFullAd23 = ValueRemoteConfigModule.native_onboarding_full_2_3
                && ((FOConfigs.obConfig.adsOBConfig.nativeOBFull23Id.isNotBlank() && isFirstOpen)
                || (FOConfigs.obConfig.adsOBConfig.nativeOBFull23IdS2.isNotBlank() && !isFirstOpen))
                && (!ValueRemoteConfigModule.check_organic_user || (ValueRemoteConfigModule.check_organic_user && !isOrganic))
                && isAdValid23

        val isAdValid34 = if (shouldLoadOBOnThisScreen) {
            !NativeManager.isAdFailed(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position)
        } else {
            NativeManager.isAdReady(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position)
                    || !NativeManager.isLoadDone(EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4.position)
        }
        val showFullAd34 = ValueRemoteConfigModule.native_onboarding_full_3_4
                && ((FOConfigs.obConfig.adsOBConfig.nativeOBFull34Id.isNotBlank() && isFirstOpen)
                || (FOConfigs.obConfig.adsOBConfig.nativeOBFull34IdS2.isNotBlank() && !isFirstOpen))
                && (!ValueRemoteConfigModule.check_organic_user || (ValueRemoteConfigModule.check_organic_user && !isOrganic))
                && isAdValid34

        val nextAction = {
            binding.viewPager2.setCurrentItem(
                binding.viewPager2.currentItem + 1,
                false
            )
        }

        if (hasFourSlides) {
            mAdapter.addFragment(FragmentOnboarding1.newInstance(nextAction))
            if (showFullAd12) addFullAd(mAdapter, EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2)
            mAdapter.addFragment(FragmentOnboarding2.newInstance(nextAction))
            if (showFullAd23) addFullAd(mAdapter, EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3)
            mAdapter.addFragment(FragmentOnboarding3.newInstance(nextAction))
            if (showFullAd34) addFullAd(mAdapter, EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4)
            mAdapter.addFragment(FragmentOnboarding4.newInstance { showInterOB() })
        } else {
            mAdapter.addFragment(FragmentOnboarding1.newInstance(nextAction))
            if (showFullAd12) addFullAd(mAdapter, EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2)
            mAdapter.addFragment(FragmentOnboarding2.newInstance(nextAction))
            if (showFullAd23) addFullAd(mAdapter, EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3)
            mAdapter.addFragment(FragmentOnboarding3.newInstance { showInterOB() })
        }


        mHandler.postDelayed({
            binding.viewPager2.isUserInputEnabled = true
        }, 1500)

        val autoNextRunnable = Runnable {
            if (currentFragment is FragmentOnboardingAdFull && !isFinishing && !isDestroyed) {
                binding.viewPager2.setCurrentItem(
                    binding.viewPager2.currentItem + 1,
                    false
                )
            }
        }
        binding.viewPager2.apply {
            adapter = mAdapter
            isUserInputEnabled = false
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    currentFragment = (adapter as ViewPagerAdapter).getFragment(position)
                    when (currentFragment) {
                        is FragmentOnboarding1 -> "onboard_view_1".postFirebaseEvent()
                        is FragmentOnboarding2 -> {
                            "onboard_view_2".postFirebaseEvent()
                            isStartOB = true
                        }

                        is FragmentOnboarding3 -> {
                            "onboard_view_3".postFirebaseEvent()
                            isStartOB = true
                        }

                        is FragmentOnboarding4 -> {
                            "onboard_view_4".postFirebaseEvent()
                            isStartOB = true
                        }

                        else -> {
                            mHandler.removeCallbacks(autoNextRunnable)
                            mHandler.postDelayed(autoNextRunnable, 15000)
                        }
                    }
                }
            })
        }
    }

    private var currentFragment: Fragment? = null

    private fun addFullAd(adapter: ViewPagerAdapter, position: EnumAdsNamePosition) {
        adapter.addFragment(
            FragmentOnboardingAdFull.newInstance(
                position,
                onAccept = {
                    binding.viewPager2.setCurrentItem(
                        binding.viewPager2.currentItem + 1,
                        false
                    )
                },
                onImpressNativeFull = {},
                onStartLoad = {},
                onLoadedAds = {}
            )
        )
    }


    val coordinator = TaskCoordinator {
        onNextScreen()
    }

    private fun onNextScreen() {
        val nextOB = FOConfigs.obConfig.uiOBConfig.nextOBActivity
        if (!isSession2 && nextOB != null) openActivityAndClearApp(nextOB)
        else if (nextOB == null) openActivityAndClearApp(splashConfig.uiSplashConfig.homeActivity)
        FOConfigs.obConfig.uiOBConfig.activityCallback?.onNextActivity(this, isSession2)
        SPF(this).is_second_time_open_app = true
        mHandler.postDelayed({
            finish()
        }, 200)
    }


    private fun showInterOB() {
        val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
        Inter.showInter(
            activity = this,
            adName = EnumAdsNamePosition.INTER_ONBOARDING.position,
            nextAction = {
                coordinator.onTaskADone()
                NativeManager.startFullscreenCountdown(EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_OB.position)
            },
            onShown = {
                coordinator.requireTaskB()
                if (!isOrganicUser) showNativeFullInter()
                else coordinator.onTaskBDone()
                EventsNameFirebase.inter_onboarding_view.postFirebaseEvent()
            },
            canShowAd = ValueRemoteConfigModule.inter_onboarding && !isOrganicUser,
            preload = false,
            checkCappingTime = ValueRemoteConfigModule.check_capping_time_inter_ob
        )
    }

    private fun showNativeFullInter() {
        NativeManager.showFullscreenNativeWithCountdown(
            activity = this@OnboardingActivity,
            canShowAd = ValueRemoteConfigModule.native_full_inter_onboarding,
            adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_OB.position,
            adLayoutRes = FOConfigs.obConfig.adsOBConfig.nativeFullInterOBLayout,
            countdownSeconds = ValueRemoteConfigModule.countdown_native_full_inter,
            startCountdownImmediately = false,
            onDismissed = {
                coordinator.onTaskBDone()
            },
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String) {
                    EventsNameFirebase.native_full_inter_ob_view.postFirebaseEvent()
                }
            }
        )
    }

    override fun loadAds() {
        super.loadAds()

        // preloadAds for Home
        FOConfigs.obConfig.uiOBConfig.activityCallback?.onStartLoadAds(this, isSession2)

        val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
        NativeManager.preloadNative(
            context = this,
            adName = EnumAdsNamePosition.NATIVE_FULL_AFTER_INTER_OB.position,
            adId = if (isFirstOpen) FOConfigs.obConfig.adsOBConfig.nativeFullInterOBId
            else FOConfigs.obConfig.adsOBConfig.nativeFullInterOBIdS2,
            canShowAd = ValueRemoteConfigModule.native_full_inter_onboarding
                    && ValueRemoteConfigModule.inter_onboarding
                    && !isOrganicUser
        )

    }

    private fun loadOnboardingAds() {
        val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
            awaitFetchConfig()
            FOConfigs.obConfig.adsOBConfig.apply {
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