package com.mobi.libraryads.views.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.mobi.libraryads.FOConfigs.obConfig
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setInVisible
import com.mobi.libraryads.commons.utils.setVisible
import com.mobi.libraryads.databinding.FrameLayoutBinding
import com.mobi.libraryads.views.base.BaseFragment
import com.mobi.libraryads.views.onboarding.OnboardingActivity.Companion.isStartOB

class FragmentOnboarding1 : BaseFragment<FrameLayoutBinding>() {
    private var onAccept: (() -> Unit)? = null
    override fun inflateVB(inflater: LayoutInflater, container: ViewGroup?) =
        FrameLayoutBinding.inflate(inflater)

    override fun initView() {
        val viewCustom = layoutInflater.inflate(
            obConfig.uiOBConfig.resFragmentOB1,
            binding.root,
            true
        )
        btnNext = binding.root.findViewById(R.id.btnNext)
    }

    override fun showAds() {
        super.showAds()
        val adFrame = binding.root.findViewById<FrameLayout>(R.id.layoutAds) ?: return
        showLoadingNext(true)
        NativeManager.showNative(
            adFrame = adFrame,
            canShowAd = ValueRemoteConfigModule.native_onboarding_1,
            adName = EnumAdsNamePosition.NATIVE_ONBOARDING_1.position,
            adLayout = obConfig.adsOBConfig.layoutNativeOB1,
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String) {
                    mHandler.postDelayed({
                        if (isAdded) {
                            showLoadingNext(false)
                        }
                    }, 1500)
                    EventsNameFirebase.native_ob_1_view.postFirebaseEvent()
                }

                override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                    showLoadingNext(false)
                }

                override fun onAdShowFailed(adName: String, reason: String) {
                    showLoadingNext(false)
                }
            }
        )
    }

    private var originalNextText: CharSequence? = null
    private val timeoutRunnable = Runnable {
        showLoadingNext(false)
    }

    private fun showLoadingNext(isLoading: Boolean) {
        if (!isAdded || view == null) return
        val tvNext = binding.root.findViewById<TextView>(R.id.tvNext)
        val progressLoading = binding.root.findViewById<View>(R.id.progressLoading)

        if (isStartOB) {
            mHandler.removeCallbacks(timeoutRunnable)
            tvNext?.text = originalNextText ?: getString(R.string.intro_next)
            tvNext?.isClickable = false
            btnNext?.isClickable = true
            btnNext?.isEnabled = true
            progressLoading?.visibility = View.GONE
            return
        }

        if (isLoading) {
            tvNext?.setInVisible()
            progressLoading?.setVisible()
            btnNext?.isClickable = false
            btnNext?.isEnabled = false
            mHandler.removeCallbacks(timeoutRunnable)
            mHandler.postDelayed(timeoutRunnable, 3000)
        } else {
            mHandler.removeCallbacks(timeoutRunnable)
            tvNext?.setVisible()
            progressLoading?.setGone()
            btnNext?.isClickable = true
            btnNext?.isEnabled = true
        }
    }

    private var btnNext: View? = null
    override fun clickView() {
        super.clickView()
        btnNext?.setOnClickListener {
            onAccept?.invoke()
        }
    }

    companion object {
        fun newInstance(onAccept: () -> Unit): FragmentOnboarding1 {
            return FragmentOnboarding1().apply {
                this.onAccept = onAccept
            }
        }
    }
}