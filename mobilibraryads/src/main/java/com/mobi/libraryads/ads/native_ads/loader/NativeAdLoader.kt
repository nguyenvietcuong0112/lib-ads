package com.mobi.libraryads.ads.native_ads.loader

import android.content.Context
import com.mobi.libraryads.ads.native_ads.model.AdLoadState
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy
import com.mobi.libraryads.ads.native_ads.model.NativeAdEntry
import com.mobi.libraryads.ads.native_ads.repository.NativeAdRepository
import com.mobi.libraryads.commons.utils.AdsLog
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class NativeAdLoader(private val repository: NativeAdRepository) {

    private val TAG = "NativeAdLoader"
    private val loadCallbacksMap = ConcurrentHashMap<String, CopyOnWriteArrayList<(Boolean) -> Unit>>()

    /**
     * Preload 1 native ad thường (chỉ dùng entry.id)
     */
    fun preloadNative(
        context: Context,
        entry: NativeAdEntry,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (entry.state == AdLoadState.LOADED && entry.nativeAd != null) {
            callback?.invoke(true)
            return
        }

        val callbacks = loadCallbacksMap.computeIfAbsent(entry.name) { CopyOnWriteArrayList() }
        if (callback != null) {
            callbacks.add(callback)
        }

        if (entry.id.isBlank()) {
            AdsLog.w(TAG, "preloadNative: ID is empty for ${entry.name}. Rejecting load.")
            repository.updateState(entry.name, AdLoadState.ERROR)
            triggerCallbacks(entry.name, false)
            return
        }

        if (entry.state == AdLoadState.LOADING) {
            AdsLog.d(TAG, "preloadNative: Ad ${entry.name} is currently loading. Queueing callback.")
            return
        }

        repository.updateState(entry.name, AdLoadState.LOADING)
        AdsLog.d(TAG, "preloadNative: Start loading single ad for ${entry.name} with id: ${entry.id}")

        performLoad(context, entry.id) { ad ->
            val success = ad != null
            if (ad != null) {
                val adapterClass = ad.responseInfo?.mediationAdapterClassName ?: ""
                repository.updateState(entry.name, AdLoadState.LOADED, ad, adapterClass)
                AdsLog.d(TAG, "preloadNative: Successfully loaded ${entry.name} with adapter: $adapterClass")
            } else {
                repository.updateState(entry.name, AdLoadState.ERROR)
                AdsLog.d(TAG, "preloadNative: Failed to load ${entry.name}")
            }
            triggerCallbacks(entry.name, success)
        }
    }

    /**
     * Preload native ad có high floor
     */
    fun preloadNativeWithHigh(
        context: Context,
        entry: NativeAdEntry,
        strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
        remoteConfigNormal: Boolean = true,
        remoteConfigHigh: Boolean = true,
        callback: ((Boolean) -> Unit)? = null
    ) {
        if (entry.idHigh.isBlank()) {
            AdsLog.d(TAG, "preloadNativeWithHigh: idHigh is empty for ${entry.name}, falling back to preloadNative.")
            if (remoteConfigNormal) {
                preloadNative(context, entry, callback)
            } else {
                AdsLog.d(TAG, "preloadNativeWithHigh: idHigh is empty and remoteConfigNormal is false for ${entry.name}. Rejecting load.")
                if (callback != null) {
                    val callbacks = loadCallbacksMap.computeIfAbsent(entry.name) { CopyOnWriteArrayList() }
                    callbacks.add(callback)
                }
                repository.updateState(entry.name, AdLoadState.ERROR)
                triggerCallbacks(entry.name, false)
            }
            return
        }

        if (entry.state == AdLoadState.LOADED && entry.nativeAd != null) {
            callback?.invoke(true)
            return
        }

        val callbacks = loadCallbacksMap.computeIfAbsent(entry.name) { CopyOnWriteArrayList() }
        if (callback != null) {
            callbacks.add(callback)
        }

        if (entry.state == AdLoadState.LOADING) {
            AdsLog.d(TAG, "preloadNativeWithHigh: Ad ${entry.name} is currently loading. Queueing callback.")
            return
        }

        repository.updateState(entry.name, AdLoadState.LOADING)
        AdsLog.d(TAG, "preloadNativeWithHigh: Start loading for ${entry.name} using strategy: $strategy")

        if (strategy == LoadStrategy.SEQUENTIAL) {
            if (remoteConfigHigh && entry.idHigh.isNotBlank()) {
                // Load High first
                AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Loading high floor: ${entry.idHigh}")
                performLoad(context, entry.idHigh) { adHigh ->
                    if (adHigh != null) {
                        val adapterClass = adHigh.responseInfo?.mediationAdapterClassName ?: ""
                        repository.updateState(entry.name, AdLoadState.LOADED, adHigh, adapterClass, isHighFloor = true)
                        AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: High floor loaded successfully for ${entry.name}")
                        triggerCallbacks(entry.name, true)
                    } else {
                        AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: High floor failed.")
                        if (remoteConfigNormal && entry.id.isNotBlank()) {
                            AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Loading low floor: ${entry.id}")
                            performLoad(context, entry.id) { adNormal ->
                                val success = adNormal != null
                                if (adNormal != null) {
                                    val adapterClass = adNormal.responseInfo?.mediationAdapterClassName ?: ""
                                    repository.updateState(entry.name, AdLoadState.LOADED, adNormal, adapterClass, isHighFloor = false)
                                    AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Normal floor loaded successfully for ${entry.name}")
                                } else {
                                    repository.updateState(entry.name, AdLoadState.ERROR)
                                    AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Both floors failed for ${entry.name}")
                                }
                                triggerCallbacks(entry.name, success)
                            }
                        } else {
                            AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Normal floor disabled or ID empty.")
                            repository.updateState(entry.name, AdLoadState.ERROR)
                            triggerCallbacks(entry.name, false)
                        }
                    }
                }
            } else if (remoteConfigNormal && entry.id.isNotBlank()) {
                AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: High floor disabled. Loading low floor: ${entry.id}")
                performLoad(context, entry.id) { adNormal ->
                    val success = adNormal != null
                    if (adNormal != null) {
                        val adapterClass = adNormal.responseInfo?.mediationAdapterClassName ?: ""
                        repository.updateState(entry.name, AdLoadState.LOADED, adNormal, adapterClass, isHighFloor = false)
                        AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Normal floor loaded successfully for ${entry.name}")
                    } else {
                        repository.updateState(entry.name, AdLoadState.ERROR)
                        AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Normal floor failed for ${entry.name}")
                    }
                    triggerCallbacks(entry.name, success)
                }
            } else {
                AdsLog.d(TAG, "preloadNativeWithHigh [SEQUENTIAL]: Both floors disabled or empty.")
                repository.updateState(entry.name, AdLoadState.ERROR)
                triggerCallbacks(entry.name, false)
            }
        } else {
            // PARALLEL Loading strategy
            AdsLog.d(TAG, "preloadNativeWithHigh [PARALLEL]: Loading high (${entry.idHigh}) and normal (${entry.id}) in parallel")

            val runHigh = remoteConfigHigh && entry.idHigh.isNotBlank()
            val runNormal = remoteConfigNormal && entry.id.isNotBlank()

            if (!runHigh && !runNormal) {
                AdsLog.d(TAG, "preloadNativeWithHigh [PARALLEL]: Both floors disabled or empty.")
                repository.updateState(entry.name, AdLoadState.ERROR)
                triggerCallbacks(entry.name, false)
                return
            }

            var highDone = !runHigh
            var highAd: NativeAd? = null
            var normalDone = !runNormal
            var normalAd: NativeAd? = null
            var callbackTriggered = false

            fun evaluate() {
                if (callbackTriggered) {
                    highAd?.destroy()
                    highAd = null
                    normalAd?.destroy()
                    normalAd = null
                    return
                }

                if (highDone && highAd != null) {
                    callbackTriggered = true
                    val adToUse = highAd
                    highAd = null

                    if (normalDone) {
                        normalAd?.destroy()
                        normalAd = null
                    }

                    val adapterClass = adToUse?.responseInfo?.mediationAdapterClassName ?: ""
                    repository.updateState(entry.name, AdLoadState.LOADED, adToUse, adapterClass, isHighFloor = true)
                    AdsLog.d(TAG, "preloadNativeWithHigh [PARALLEL]: High ad loaded first/preferred for ${entry.name}")
                    triggerCallbacks(entry.name, true)
                    return
                }

                if (highDone && normalDone) {
                    callbackTriggered = true
                    if (normalAd != null) {
                         val adToUse = normalAd
                         normalAd = null
                         val adapterClass = adToUse?.responseInfo?.mediationAdapterClassName ?: ""
                         repository.updateState(entry.name, AdLoadState.LOADED, adToUse, adapterClass, isHighFloor = false)
                         AdsLog.d(TAG, "preloadNativeWithHigh [PARALLEL]: Normal ad loaded for ${entry.name} (High floor failed)")
                        triggerCallbacks(entry.name, true)
                    } else {
                        repository.updateState(entry.name, AdLoadState.ERROR)
                        AdsLog.d(TAG, "preloadNativeWithHigh [PARALLEL]: Both high and normal failed for ${entry.name}")
                        triggerCallbacks(entry.name, false)
                    }
                }
            }

            if (runHigh) {
                performLoad(context, entry.idHigh) { adHigh ->
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        highDone = true
                        highAd = adHigh
                        evaluate()
                    }
                }
            }

            if (runNormal) {
                performLoad(context, entry.id) { adNormal ->
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        normalDone = true
                        normalAd = adNormal
                        evaluate()
                    }
                }
            } else {
                evaluate()
            }
        }
    }

    /**
     * Load nội bộ 1 ad ID
     */
    private fun performLoad(
        context: Context,
        adId: String,
        onResult: (NativeAd?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val adOptions = NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)


            val builder = AdLoader.Builder(context.applicationContext, adId)
                .forNativeAd { nativeAd ->
                    onResult(nativeAd)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        AdsLog.w(TAG, "performLoad: onAdFailedToLoad: ID = $adId, message = ${loadAdError.message}")
                        onResult(null)
                    }
                })
                .withNativeAdOptions(adOptions.build())

            builder.build().loadAd(AdRequest.Builder().build())
        }
    }

    private fun triggerCallbacks(name: String, success: Boolean) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val callbacks = loadCallbacksMap.remove(name)
            callbacks?.forEach { it.invoke(success) }
        }
    }
}
