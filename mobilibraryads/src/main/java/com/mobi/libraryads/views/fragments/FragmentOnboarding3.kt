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
import com.mobi.libraryads.databinding.FrameLayoutBinding
import com.mobi.libraryads.views.base.BaseFragment

class FragmentOnboarding3 : BaseFragment<FrameLayoutBinding>() {
    private var onAccept: (() -> Unit)? = null
    override fun inflateVB(inflater: LayoutInflater, container: ViewGroup?) =
        FrameLayoutBinding.inflate(inflater)

    override fun initView() {
        val viewCustom = layoutInflater.inflate(
            obConfig.uiOBConfig.resFragmentOB3,
            binding.root,
            true
        )
    }

    override fun showAds() {
        super.showAds()
        val adFrame = binding.root.findViewById<FrameLayout>(R.id.layoutAds) ?: return
        NativeManager.showNative(
            adFrame = adFrame,
            canShowAd = ValueRemoteConfigModule.native_onboarding_3,
            adName = EnumAdsNamePosition.NATIVE_ONBOARDING_3.position,
            adLayout = obConfig.adsOBConfig.layoutNativeOB3,
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String) {
                    EventsNameFirebase.native_ob_3_view.postFirebaseEvent()
                }
            }
        )
    }

    override fun clickView() {
        super.clickView()
        val btnNext = binding.root.findViewById<View>(R.id.btnNext)
        btnNext?.setOnClickListener {
            onAccept?.invoke()
        }
    }


    companion object {
        fun newInstance(onAccept: () -> Unit): FragmentOnboarding3 {
            return FragmentOnboarding3().apply {
                this.onAccept = onAccept
            }
        }
    }
}