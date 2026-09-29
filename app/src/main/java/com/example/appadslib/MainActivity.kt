package com.example.appadslib

import android.view.LayoutInflater
import com.mobi.libraryads.FOConfigs.splashConfig
import com.mobi.libraryads.ads.banner_ads.Banner
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.utils.EnumAdsNamePosition
import com.mobi.libraryads.commons.firebasetracking.EventsNameFirebase.banner_splash_view
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule
import com.mobi.libraryads.commons.utils.Constants
import com.mobi.libraryads.commons.utils.clickOnce
import com.mobi.libraryads.commons.utils.openActivity
import com.mobi.libraryads.views.base.BaseActivity
import com.mobi.libraryads.views.language.LanguageActivity
import com.example.appadslib.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>() {
    override fun inflateVB(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initView() {
        binding.apply {
            btnNext.clickOnce {
                openActivity(LanguageActivity::class.java) {
                    putBoolean(Constants.FROM_SETTING, true)
                    putBoolean(Constants.CAN_SHOW_NATIVE_LANGUAGE_SETTING, true)
                    putString(
                        Constants.NAME_AD_NATIVE_LANGUAGE,
                        EnumAdsNamePosition.NATIVE_ALL.position
                    )
                }
            }
            btnTestAd.clickOnce {
                openActivity(TestAdActivity::class.java)
            }
            btnTestListAdapter.clickOnce {
                openActivity(TestListAdapterActivity::class.java)
            }
            btnReloadNative.clickOnce {
                reloadNative()
            }
        }
    }

    override fun showAds() {
        super.showAds()
        Banner.requestBanner(
            activity = this@MainActivity,
            id = splashConfig.adsSplashConfig.bannerId,
            adFrame = binding.layoutAds,
            onShown = {
                banner_splash_view.postFirebaseEvent()
            },
            canShowAd = ValueRemoteConfigModule.banner_splash
        )

//        Inter.preLoadInter(
//            activity = this,
//            adModel = InterAdModel(
//                name = "inter_all",
//                id = getString(R.string.inter_all)
//            ),
//            canShowAd = true
//        )
//
//        NativeManager.preloadNativeWithHigh(
//            context = this,
//            adName = EnumAdsNamePosition.NATIVE_ALL.position,
//            adId = getString(R.string.native_all),
//            idHigh = getString(R.string.native_edit),
//            strategy = LoadStrategy.PARALLEL,
//            canShowIdAll = true,
//            canShowIdHigh = true
//        )

        NativeManager.preloadNative(
            context = this,
            adName = EnumAdsNamePosition.NATIVE_ALL.position,
            adId = getString(R.string.native_all)
        )

//        NativeManager.preloadNativeWithHigh(
//            context = this,
//            adName = "native_full",
//            adId = getString(R.string.native_edit),
//            idHigh = getString(R.string.native_all),
//            strategy = LoadStrategy.PARALLEL
//        )


//        NativeManager.showNative(
//            adFrame = binding.layoutAds,
//            adName = EnumAdsNamePosition.NATIVE_ALL.position,
//            adLayout = R.layout.admob_layout_native_medium,
//            adId = getString(R.string.native_all),
//            isPreload = true,
//            canShowAd = true
//        )

    }

    private fun reloadNative() {
        NativeManager.showNative(
            adFrame = binding.layoutAds,
            adName = EnumAdsNamePosition.NATIVE_ALL.position,
            adId = getString(R.string.native_all),
            adLayout = R.layout.admob_layout_native_small,
            isPreload = false,
            canShowAd = true,
        )
    }

}