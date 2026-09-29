package com.mobi.libraryads

import com.mobi.libraryads.data.LanguageConfig
import com.mobi.libraryads.data.OBConfig
import com.mobi.libraryads.data.SplashConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object FOConfigs {
    private var _splashConfig: SplashConfig? = null
    private var _languageConfig: LanguageConfig? = null
    private var _obConfig: OBConfig? = null

    val splashConfig: SplashConfig
        get() = requireNotNull(_splashConfig)
    val languageConfig: LanguageConfig
        get() = requireNotNull(_languageConfig)
    val obConfig: OBConfig
        get() = requireNotNull(_obConfig)

    var isOrganic = false
    var isLoadAdsPerScreen = false

    fun init(
        splashConfig: SplashConfig,
        languageConfig: LanguageConfig,
        obConfig: OBConfig,
        loadAdsPerScreen: Boolean = false
    ) {
        _splashConfig = splashConfig
        _languageConfig = languageConfig
        _obConfig = obConfig
        isLoadAdsPerScreen = loadAdsPerScreen
    }
}

object AdsCoroutineScope {
    private val supervisorJob = SupervisorJob()
    val scope = CoroutineScope(
        supervisorJob + Dispatchers.Default
    )
}