package com.mobi.libraryads.data

import android.app.Activity
import com.mobi.libraryads.R

data class OBConfig(
    val uiOBConfig: UiOBConfig,
    val adsOBConfig: AdsOBConfig,
)

data class UiOBConfig(
    val resLayout: Int? = null,
    val resFragmentOB1: Int,
    val resFragmentOB2: Int,
    val resFragmentOB3: Int,
    val resFragmentOB4: Int = resFragmentOB3,
    val resFragmentOBAdFull: Int = R.layout.fragment_onboarding_ad_full,
    val nextOBActivity: Class<out Activity>? = null,
    val activityCallback: OnActivityCallBack? = null
)

data class AdsOBConfig(
    val nativeOB1Id: String = "",
    val nativeOB2Id: String = "",
    val nativeOB3Id: String = "",
    val nativeOB4Id: String = "",
    val nativeOBFull12Id: String = "",
    val nativeOBFull23Id: String = "",
    val nativeOBFull34Id: String = "",
    val layoutNativeOB1: Int,
    val layoutNativeOB2: Int = layoutNativeOB1,
    val layoutNativeOB3: Int = layoutNativeOB1,
    val layoutNativeOB4: Int = layoutNativeOB1,
    val layoutNativeFullOB: Int = R.layout.admob_native_full_onboarding,

    val interOBId: String = "",
    val nativeFullInterOBId: String = "",
    val nativeFullInterOBLayout: Int = R.layout.layout_native_full_inter,
    val isLoadNativeOBInLanguage: Boolean = false,
    val isLoadNativeOBInOnboarding: Boolean = false,

    val nativeOB1IdS2: String = nativeOB1Id,
    val nativeOB2IdS2: String = nativeOB2Id,
    val nativeOB3IdS2: String = nativeOB3Id,
    val nativeOB4IdS2: String = nativeOB4Id,
    val nativeOBFull12IdS2: String = nativeOBFull12Id,
    val nativeOBFull23IdS2: String = nativeOBFull23Id,
    val nativeOBFull34IdS2: String = nativeOBFull34Id,

    val interOBIdS2: String = interOBId,
    val nativeFullInterOBIdS2: String = nativeFullInterOBId,
)
