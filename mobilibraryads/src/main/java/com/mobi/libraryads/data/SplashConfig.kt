package com.mobi.libraryads.data

import android.app.Activity
import com.mobi.libraryads.R
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy


data class SplashConfig(
    val uiSplashConfig: UiSplashConfig,
    val adsSplashConfig: AdsSplashConfig
)

data class UiSplashConfig(
    val resLayout: Int,
    val homeActivity: Class<out Activity>,
    val activityCallBack: OnActivityCallBack? = null,
    val showFOForever: Boolean = false,
    val timeout: Long = 30_000,
    val videos: List<Int> = listOf(),
    val dismissNativeFullOnAdClick: Boolean = false
)

data class AdsSplashConfig(
    val bannerId: String = "",
    val interHighId: String = "",
    val interAllId: String = "",
    val nativeFullId: String = "",
    val nativeFullHighId: String = "",
    val nativeFullLayout: Int = R.layout.layout_native_full_inter,
    val loadInterStrategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    val loadNativeFullStrategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    val admobAOAId: String = "",
    val isCheckOrganicUser: Boolean = false,
    val showLoadingAdFull: Boolean = false,
    val loadingAdFullLayout: Int = R.layout.dialog_loading_ads,
    val loadingAdFullTime: Long = 2_000,
    val bannerIdS2: String = bannerId,
    val interHighIdS2: String = interHighId,
    val interAllIdS2: String = interAllId,
    val nativeFullIdS2: String = nativeFullId,
    val nativeFullHighIdS2: String = nativeFullHighId,
    val preloadNextScreenAds: Boolean = true,
)

interface OnActivityCallBack {
    //se
    fun onStartActivity(activity: Activity, inSession2: Boolean) {}
    fun onNextActivity(activity: Activity, inSession2: Boolean) {}
    fun onStartLoadAds(activity: Activity, inSession2: Boolean) {}
}