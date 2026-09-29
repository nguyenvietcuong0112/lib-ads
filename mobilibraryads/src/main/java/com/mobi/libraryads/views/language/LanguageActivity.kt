package com.mobi.libraryads.views.language

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobi.libraryads.BuildConfig
import com.mobi.libraryads.AdsApplication.Companion.isFirstOpen
import com.mobi.libraryads.AdsCoroutineScope
import com.mobi.libraryads.FOConfigs
import com.mobi.libraryads.FOConfigs.isOrganic
import com.mobi.libraryads.FOConfigs.languageConfig
import com.mobi.libraryads.FOConfigs.splashConfig
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.inter_ads.Inter
import com.mobi.libraryads.ads.inter_ads.InterAdModel
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.native_language_click_high_view
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.native_language_click_view
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.native_language_high_view
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.native_language_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.AdsLog
import com.mobi.libraryads.commons.utils.Constants
import com.mobi.libraryads.commons.utils.click
import com.mobi.libraryads.commons.utils.openActivityAndClearApp
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setInVisible
import com.mobi.libraryads.commons.utils.setVisible
import com.mobi.libraryads.databinding.FrameLayoutBinding
import com.mobi.libraryads.data.LanguageModel
import com.mobi.libraryads.views.adapters.OnAdapterClick
import com.mobi.libraryads.views.adapters.LanguageAdapter
import com.mobi.libraryads.views.base.BaseActivity
import com.mobi.libraryads.views.onboarding.OnboardingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class LanguageActivity : BaseActivity<FrameLayoutBinding>() {
    private var currentLanguage = ""

    val listLanguageModel = arrayListOf<LanguageModel>()


    override fun inflateVB(inflater: LayoutInflater): FrameLayoutBinding {
        return FrameLayoutBinding.inflate(inflater)
    }

    private lateinit var adapterSelectLanguage: LanguageAdapter

    private var btnBack: View? = null
    private var rcvLanguage: View? = null
    private var btnDone: View? = null
    private var btnDoneDisable: View? = null
    private var progressLoading: View? = null
    private var layoutAds: View? = null
    private var animTap: View? = null
    private var fromSetting = false
    private var nameAdsLanguageFromSetting = ""


    override fun preLoadData() {
        super.preLoadData()
        listLanguageModel.clear()
        listLanguageModel.addAll(languageConfig.uiLanguageConfig.listLanguage)
        fromSetting = intent.getBooleanExtra(Constants.FROM_SETTING, false)
        nameAdsLanguageFromSetting = intent.getStringExtra(Constants.NAME_AD_NATIVE_LANGUAGE) ?: ""

    }

    override fun initView() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (fromSetting) finish()
                }
            })
        val viewCustom = layoutInflater.inflate(
            languageConfig.uiLanguageConfig.resLayout,
            binding.root,
            true
        )

        btnBack = viewCustom.findViewById(R.id.btnBack)
        rcvLanguage = viewCustom.findViewById(R.id.rcvLanguage)
        btnDone = viewCustom.findViewById(R.id.btnDone)
        btnDoneDisable = viewCustom.findViewById(R.id.btnDoneDisable)
        progressLoading = viewCustom.findViewById(R.id.progressLoading)
        layoutAds = viewCustom.findViewById(R.id.layoutAds)
        animTap = viewCustom.findViewById(R.id.aniTap)


        lifecycleScope.launch {
            val loadingView = viewCustom.findViewById<View>(R.id.loadingView)
            loadingView?.setVisible()
            delay(1000.milliseconds)
            loadingView?.setGone()
        }


        if (fromSetting) {
            btnBack?.setVisible()
            animTap?.setGone()
            btnBack?.click {
                finish()
            }
            val savedLang = SPF(this).language_code_selected
            listLanguageModel.forEach { languageModel ->
                languageModel.isSelect = (languageModel.code == savedLang)
            }
            currentLanguage = savedLang
        } else {
            btnBack?.setInVisible()
            listLanguageModel.forEach { languageModel ->
                languageModel.isSelect = false
            }
            currentLanguage = ""
        }

        btnDoneDisable?.click() {
            if (currentLanguage == "")
                Toast.makeText(this, "Please select a language", Toast.LENGTH_SHORT).show()
        }

        btnDone?.click(600) {
            btnDone?.isEnabled = false
            if (currentLanguage != "") {
                onDoneClick()
            }
        }

        adapterSelectLanguage = LanguageAdapter(object : OnAdapterClick {
            @SuppressLint("NotifyDataSetChanged")
            override fun onSelect(data: Any) {
                val dataSelected = data as LanguageModel
                listLanguageModel.forEach { selectLanguageModel ->
                    selectLanguageModel.isSelect =
                        selectLanguageModel.code == dataSelected.code
                }
                currentLanguage = dataSelected.code
                adapterSelectLanguage.notifyDataSetChanged()
                btnDone?.isEnabled = true
                btnDoneDisable?.setGone()
                if (firstClick) {
                    btnDone?.setGone()
                    progressLoading?.setVisible()

                    mHandler.postDelayed({
                        btnDone?.setVisible()
                        progressLoading?.setGone()
                    }, 2000)
                    firstClick = false
                    showAdsClick()
                }

                if (animTap?.isVisible == true) animTap?.setGone()
            }
        })

        if (rcvLanguage == null) {
            AdsLog.d("Language initView error: rcvLanguage == null")
            return
        }

        (rcvLanguage as RecyclerView).apply {
            layoutManager = LinearLayoutManager(this@LanguageActivity)
            adapter = adapterSelectLanguage
            adapterSelectLanguage.submitList(listLanguageModel)
        }

        viewCustom.setOnClickListener {
            countClick++
            if (countClick == 1) startTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - startTime > 2000) {
                countClick = 0
                return@setOnClickListener
            }
            if (countClick > 10) {
                Toast.makeText(
                    this, "${BuildConfig.LIB_ARTIFACT}:${BuildConfig.LIB_VERSION}",
                    Toast.LENGTH_LONG
                ).show()
                countClick = 0
            }
        }
    }

    private var countClick = 0
    private var startTime = 0L
    private var firstClick = true

    override fun loadAds() {
        super.loadAds()
        val canLoadOBInLanguage = !FOConfigs.isLoadAdsPerScreen
                && !FOConfigs.obConfig.adsOBConfig.isLoadNativeOBInOnboarding
                && FOConfigs.obConfig.adsOBConfig.isLoadNativeOBInLanguage
        if (!fromSetting && canLoadOBInLanguage) {
            onLoadNativeOB()
        }

        if (!fromSetting) {
            loadLanguageAds()
        }
    }

    private fun loadLanguageAds() {
        val adsConfig = languageConfig.adsLanguageConfig
        if (!NativeManager.isAdReady(EnumAdsNamePosition.NATIVE_LANGUAGE.position)) {
            NativeManager.preloadNativeWithHigh(
                context = applicationContext,
                adName = EnumAdsNamePosition.NATIVE_LANGUAGE.position,
                adId = if (isFirstOpen) adsConfig.nativeLangId else adsConfig.nativeLangIdS2,
                idHigh = if (isFirstOpen) adsConfig.nativeLangHighId else adsConfig.nativeLangHighIdS2,
                strategy = adsConfig.loadStrategy,
                canShowIdAll = ValueRemoteConfigModule.native_language,
                canShowIdHigh = ValueRemoteConfigModule.native_language_high,
            )
        }
        if (!NativeManager.isAdReady(EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position)) {
            AdsCoroutineScope.scope.launch(Dispatchers.IO) {
                delay(200.milliseconds)
                NativeManager.preloadNativeWithHigh(
                    context = applicationContext,
                    adName = EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position,
                    adId = if (isFirstOpen) adsConfig.nativeLangClickId else adsConfig.nativeLangClickIdS2,
                    idHigh = if (isFirstOpen) adsConfig.nativeLangClickHighId else adsConfig.nativeLangClickHighIdS2,
                    strategy = adsConfig.loadStrategy,
                    canShowIdAll = ValueRemoteConfigModule.native_language_click,
                    canShowIdHigh = ValueRemoteConfigModule.native_language_high_click,
                )
            }
        }
    }

    override fun showAds() {
        super.showAds()
        if (layoutAds == null) {
            AdsLog.d("Language showAds error: layoutAds == null")
            return
        }
        if (fromSetting) {  //truyền name ad native lang setting qua intent, layout dùng của native lang click
            if (nameAdsLanguageFromSetting.isNotEmpty()) {
                val adsConfig = languageConfig.adsLanguageConfig
                NativeManager.showNative(
                    adFrame = layoutAds as FrameLayout,
                    adName = nameAdsLanguageFromSetting,
                    adLayout = adsConfig.layoutNativeClick,
                    isPreload = true,
                    callback = object : INativeAdCallback {
                        override fun onAdShown(adName: String) {
                            layoutAds?.setVisible()
                            "native_language_setting_view".postFirebaseEvent()
                        }

                        override fun onAdShowFailed(adName: String, reason: String) {
                            layoutAds?.setGone()
                        }
                    },
                    canShowAd = ValueRemoteConfigModule.native_language_setting
                )
            }
        } else {
            val adsConfig = languageConfig.adsLanguageConfig
            NativeManager.showNative(
                adFrame = layoutAds as FrameLayout,
                adName = EnumAdsNamePosition.NATIVE_LANGUAGE.position,
                adLayout = adsConfig.layoutNative,
                isPreload = false,
                callback = object : INativeAdCallback {
                    override fun onAdShown(adName: String, isHighFloor: Boolean) {
                        super.onAdShown(adName, isHighFloor)
                        layoutAds?.setVisible()
                        native_language_view.postFirebaseEvent()
                        AdsLog.d("EnumAdsNamePosition.NATIVE_LANGUAGE.position: isHighFloor $isHighFloor")
                        if (isHighFloor) native_language_high_view.postFirebaseEvent()
                    }

                    override fun onAdShowFailed(adName: String, reason: String) {
                        layoutAds?.setGone()
                    }
                },
                canShowAd = ValueRemoteConfigModule.native_language || ValueRemoteConfigModule.native_language_high
            )
        }

    }


    private fun showAdsClick() {
        if (layoutAds == null || fromSetting) return
        val adsConfig = languageConfig.adsLanguageConfig
        NativeManager.showNative(
            adFrame = layoutAds as FrameLayout,
            adName = EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position,
            adLayout = adsConfig.layoutNativeClick,
            isPreload = false,
            callback = object : INativeAdCallback {
                override fun onAdShown(adName: String, isHighFloor: Boolean) {
                    super.onAdShown(adName, isHighFloor)
                    layoutAds?.setVisible()
                    native_language_click_view.postFirebaseEvent()
                    AdsLog.d("EnumAdsNamePosition.NATIVE_LANGUAGE_CLICK.position: isHighFloor $isHighFloor")
                    if (isHighFloor) native_language_click_high_view.postFirebaseEvent()
                }

                override fun onAdShowFailed(adName: String, reason: String) {
                    layoutAds?.setGone()
                }
            },
            canShowAd = ValueRemoteConfigModule.native_language_click || ValueRemoteConfigModule.native_language_high_click
        )
    }


    private fun onDoneClick() {
        SPF(this).language_code_selected = currentLanguage
        if (fromSetting) {
            languageConfig.uiLanguageConfig.languageSetting?.onDone(this)
        } else {
            openActivityAndClearApp(OnboardingActivity::class.java)
        }
        finish()
    }

    private fun onLoadNativeOB() {
        val isOrganicUser = ValueRemoteConfigModule.check_organic_user && isOrganic
        AdsCoroutineScope.scope.launch(Dispatchers.IO) {
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