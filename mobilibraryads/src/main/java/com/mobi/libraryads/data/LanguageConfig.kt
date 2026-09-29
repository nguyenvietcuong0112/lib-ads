package com.mobi.libraryads.data

import android.app.Activity
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy

data class LanguageConfig(
    val uiLanguageConfig: UiLanguageConfig,
    val adsLanguageConfig: AdsLanguageConfig,
)

data class UiLanguageConfig(
    val resLayout: Int,
    val itemLangDefault: Int,
    val itemLangSelected: Int,
    val listLanguage: ArrayList<LanguageModel> = arrayListOf<LanguageModel>(),
    val languageSetting: LanguageSetting? = null,
    val showInSession2: Boolean = true
)

data class AdsLanguageConfig(
    val nativeLangHighId: String = "",
    val nativeLangId: String = "",
    val nativeLangClickHighId: String = "",
    val nativeLangClickId: String = "",
    val layoutNative: Int,
    val layoutNativeClick: Int = layoutNative,
    val loadStrategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    val preloadAdsLater: Boolean = false,
    val nativeLangHighIdS2: String = nativeLangHighId,
    val nativeLangIdS2: String = nativeLangId,
    val nativeLangClickHighIdS2: String = nativeLangClickHighId,
    val nativeLangClickIdS2: String = nativeLangClickId,
)

interface LanguageSetting {
    fun onDone(activity: Activity)
}