package com.mobi.libraryads.commons.remote

object ValueRemoteConfigModule : KonfigModel {

    val resume_open_app by konfig("resume_open_app", true)
    val banner_splash by konfig("banner_splash", true)
    val inter_splash_high by konfig("inter_splash_high", true)
    val inter_splash by konfig("inter_splash", true)

    val native_full_inter_splash by konfig("native_full_inter_splash", true)
    val native_full_high_inter_splash by konfig("native_full_high_inter_splash", true)

    val native_language by konfig("native_language", true)
    val native_language_high by konfig("native_language_high", true)
    val native_language_click by konfig("native_language_click", true)
    val native_language_high_click by konfig("native_language_high_click", true)

    val native_onboarding_1 by konfig("native_onboarding_1", true)
    val native_onboarding_2 by konfig("native_onboarding_2", false)
    val native_onboarding_3 by konfig("native_onboarding_3", true)
    val native_onboarding_4 by konfig("native_onboarding_4", true)
    val native_onboarding_full_1_2 by konfig("native_onboarding_full_1_2", true)
    val native_onboarding_full_2_3 by konfig("native_onboarding_full_2_3", true)
    val native_onboarding_full_3_4 by konfig("native_onboarding_full_3_4", true)

    val inter_onboarding by konfig("inter_onboarding", true)
    val native_full_inter_onboarding by konfig("native_full_inter_onboarding", false)

    val reward_ad by konfig("reward_ad", true)

    val apply_remove_shortcut by konfig("apply_remove_shortcut", true)

    val ad_full_capping_time by konfig("ad_full_capping_time", 20000)
    val countdown_native_full_inter by konfig("countdown_native_full_inter", 3)
    val delay_button_close_native_full by konfig("delay_button_close_native_full", 0)

    val native_language_setting by konfig("native_language_setting", true)
    val forceUpdate by konfig("forceUpdate", false)
    val showUpdate by konfig("showUpdate", false)
    val lastVersionCode by konfig("lastVersionCode", 1L)
    val timeout_splash by konfig("timeout_splash", 30L)
    val check_organic_user by konfig("check_organic_user", true)
    val check_capping_time_inter_ob by konfig("check_capping_time_inter_ob", false)
}