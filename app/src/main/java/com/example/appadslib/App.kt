package com.example.appadslib

import android.app.Activity
import android.app.Application
import android.os.Handler
import com.mobi.libraryads.BuildConfig
import com.mobi.libraryads.AdsApplication
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy
import com.mobi.libraryads.data.AdsLanguageConfig
import com.mobi.libraryads.data.AdsOBConfig
import com.mobi.libraryads.data.AdsSplashConfig
import com.mobi.libraryads.data.LanguageConfig
import com.mobi.libraryads.data.OBConfig
import com.mobi.libraryads.data.OnActivityCallBack
import com.mobi.libraryads.data.SplashConfig
import com.mobi.libraryads.commons.tracking.impl.FirebaseAdTracker
import com.mobi.libraryads.commons.tracking.impl.AdjustAdTracker
import com.mobi.libraryads.data.UiLanguageConfig
import com.mobi.libraryads.data.UiOBConfig
import com.mobi.libraryads.data.UiSplashConfig

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val adsLibrary = AdsApplication(this, RemoteConfigs, BuildConfig.DEBUG)
        adsLibrary.initSdk(
            adjustAppToken = "",
            gsmAppId = "",
            loadAdsPerScreen = true, // Bật cấu hình ở màn nào load màn đấy (không preload trước sang màn khác)
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
//                    activityCallBack = object : OnActivityCallBack {
//                        override fun onStartLoadAds(activity: Activity, inSession2: Boolean) {
//                            super.onStartLoadAds(activity, inSession2)
////                            Handler().postDelayed({ adsLibrary.preloadAOA() }, 2000)
//                        }
//                    },
                    showFOForever = true,
                    homeActivity = MainActivity::class.java,
                    videos = listOf(R.raw.anim_anime1, R.raw.anim_anime2)
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
//                    nativeFullId = getString(R.string.native_all),
//                    nativeFullLayout = R.layout.layout_native_full,
                    admobAOAId = getString(R.string.resume_open_app),
//                    showLoadingAdFull = true,
//                    loadingAdFullTime = 1000,
//                isCheckOrganicUser = true
                )
            ),
            languageConfig =
                LanguageConfig(
                    uiLanguageConfig = UiLanguageConfig(
                        resLayout = R.layout.activity_language_app,
                        itemLangDefault = R.layout.item_select_language_default,
                        itemLangSelected = R.layout.item_select_language_selected,
                        listLanguage = EnumSelectLanguage.toLanguageModelList(),
                    ),
                    adsLanguageConfig = AdsLanguageConfig(
                        nativeLangId = getString(R.string.native_language),
                        nativeLangClickId = getString(R.string.native_language_alt),
                        layoutNative = R.layout.admob_layout_native_medium,
                        layoutNativeClick = R.layout.admob_layout_native_small,
                        preloadAdsLater = true
                    )
                ),
            obConfig =
                OBConfig(
                    uiOBConfig = UiOBConfig(
                        resFragmentOB1 = R.layout.fragment_ob1,
                        resFragmentOB2 = R.layout.fragment_ob2,
                        resFragmentOB3 = R.layout.fragment_ob3,
                        resFragmentOB4 = R.layout.fragment_ob4,
                        resFragmentOBAdFull = R.layout.fragment_ob_ad_full,
                        activityCallback = object : OnActivityCallBack {
                        }
                    ),
                    adsOBConfig = AdsOBConfig(
                        nativeOB1Id = getString(R.string.native_onboarding_1),
                        nativeOB2Id = getString(R.string.native_onboarding_3),
                        nativeOB3Id = getString(R.string.native_onboarding_3),
                        nativeOB4Id = getString(R.string.native_onboarding_3),
                        nativeOBFull12Id = getString(R.string.native_onboarding_full_1),
                        nativeOBFull23Id = getString(R.string.native_onboarding_full_1),
                        layoutNativeOB1 = R.layout.admob_layout_native_medium,
                        layoutNativeFullOB = R.layout.admob_layout_native_full,
                        nativeFullInterOBLayout = R.layout.layout_native_full,
                        nativeFullInterOBId = getString(R.string.native_onboarding_1),
                        interOBId = getString(R.string.inter_all)
                    )
                ),
            trackers = listOf(
                FirebaseAdTracker(), // Luôn log ad_impression lên Firebase Analytics
                AdjustAdTracker(appToken = "") // Nếu dùng Adjust thì truyền token, nếu dùng AppsFlyer thì truyền AppsFlyerAdTracker
            )
        )
    }
}