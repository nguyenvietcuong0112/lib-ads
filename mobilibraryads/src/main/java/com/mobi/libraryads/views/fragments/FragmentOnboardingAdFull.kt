package com.mobi.libraryads.views.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.mobi.libraryads.FOConfigs.obConfig
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.utils.clickOnce
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setVisible
import com.mobi.libraryads.databinding.FrameLayoutBinding
import com.mobi.libraryads.views.base.BaseFragment

class FragmentOnboardingAdFull :
    BaseFragment<FrameLayoutBinding>() {

    private var onAccept: (() -> Unit)? = null
    private var onImpressNativeFull: (() -> Unit)? = null
    private var onStartLoad: (() -> Unit)? = null
    private var onLoadedAds: (() -> Unit)? = null
    private var orderAds: EnumAdsNamePosition = EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2

    companion object {
        fun newInstance(
            order: EnumAdsNamePosition,
            onAccept: () -> Unit,
            onImpressNativeFull: () -> Unit,
            onStartLoad: () -> Unit,
            onLoadedAds: () -> Unit
        ): FragmentOnboardingAdFull {
            return FragmentOnboardingAdFull().apply {
                this.orderAds = order
                this.onAccept = onAccept
                this.onImpressNativeFull = onImpressNativeFull
                this.onStartLoad = onStartLoad
                this.onLoadedAds = onLoadedAds
            }
        }
    }

    override fun inflateVB(inflater: LayoutInflater, container: ViewGroup?) =
        FrameLayoutBinding.inflate(inflater)

    override fun initView() {
        val viewCustom = layoutInflater.inflate(
            obConfig.uiOBConfig.resFragmentOBAdFull,
            binding.root,
            true
        )

        val btnNext = viewCustom.findViewById<View>(R.id.btnNext) ?: return
        val btnNextLoading = viewCustom.findViewById<View>(R.id.btnNextLoading)
        btnNext.setGone()
        btnNextLoading?.setVisible()
        val delay = ValueRemoteConfigModule.delay_button_close_native_full
        mHandler.postDelayed({
            btnNextLoading?.setGone()
            btnNext.setVisible()
        }, delay * 1000L + 50)

        btnNext.clickOnce {
            onAccept?.invoke()
        }
    }

    override fun showAds() {
        super.showAds()
        val adFrame = binding.root.findViewById<FrameLayout>(R.id.layoutAds) ?: return
        val isCanShowAd = when (orderAds) {
            EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2 -> ValueRemoteConfigModule.native_onboarding_full_1_2
            EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3 -> ValueRemoteConfigModule.native_onboarding_full_2_3
            EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4 -> ValueRemoteConfigModule.native_onboarding_full_3_4
            else -> false
        }

        NativeManager.showNative(
            adFrame = adFrame,
            canShowAd = isCanShowAd,
            adName = orderAds.position,
            adLayout = obConfig.adsOBConfig.layoutNativeFullOB,
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String) {
                    onImpressNativeFull?.invoke()
                    val eventName = when (orderAds) {
                        EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_1_2 -> EventsNameFirebase.native_ob_full_1_2_view
                        EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_2_3 -> EventsNameFirebase.native_ob_full_2_3_view
                        EnumAdsNamePosition.NATIVE_ONBOARDING_FULL_3_4 -> EventsNameFirebase.native_ob_full_3_4_view
                        else -> ""
                    }
                    eventName.postFirebaseEvent()
                }

                override fun onAdShowFailed(adName: String, reason: String) {
                    super.onAdShowFailed(adName, reason)
                    onAccept?.invoke()
                }
            }
        )
    }
}