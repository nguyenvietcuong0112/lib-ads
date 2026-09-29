package com.mobi.libraryads.data


interface SplashConfigProvider {
    fun provideSplashConfig(): SplashConfig
}
interface LanguageConfigProvider {
    fun provideLanguageConfig(): LanguageConfig
}

interface OBConfigProvider {
    fun provideOnboardConfig(): OBConfig
}