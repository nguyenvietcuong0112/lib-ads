package com.mobi.libraryads.commons.remote

import android.util.Log
import com.mobi.libraryads.commons.remote.ValueRemoteConfigModule.inter_splash
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


object RemoteKonfig {

    private val defaultValues: MutableMap<String, Any> = HashMap()

    class Builder {

        /**
         * Initialize Remote Config SDK with parameters.
         * See [Firebase Docs](https://firebase.google.com/docs/reference/android/com/google/firebase/remoteconfig/FirebaseRemoteConfigSettings.Builder.html)
         */
        var minimumFetchIntervalInSeconds: Long? = null

        /**
         * Initialize Remote Config SDK with parameters.
         * See [Firebase Docs](https://firebase.google.com/docs/reference/android/com/google/firebase/remoteconfig/FirebaseRemoteConfigSettings.Builder.html)
         */
        var fetchTimeoutInSeconds: Long? = null

        /**
         * Register KonfigModel to set default value to Remote Config
         *
         * @param model KonfigModel to register
         */
        @Suppress("unused")
        fun registerModels(vararg model: KonfigModel) = Unit
    }

    fun initialize(
        block: Builder.() -> Unit,
        fetchDone: ((success: Boolean) -> Unit)? = null
    ): Task<Boolean> {
        val builder = Builder()
        block(builder)

        Firebase.remoteConfig.apply {
            val config = remoteConfigSettings {
                builder.minimumFetchIntervalInSeconds?.let { minimumFetchIntervalInSeconds = it }
                builder.fetchTimeoutInSeconds?.let { fetchTimeoutInSeconds = it }
            }
            return setConfigSettingsAsync(config)
                .continueWithTask {
                    setDefaultsAsync(defaultValues)
                }
                .continueWithTask { fetchAndActivate() }
                .addOnCompleteListener { task -> fetchDone?.invoke(task.isSuccessful) }
        }
    }

    internal fun register(clazz: Class<out Any>, key: String, default: Any) {
        if (defaultValues.containsKey(key)) {
            Log.w("RemoteKonfig", "Key $key of ${clazz.simpleName} is already registered.")
        } else {
            defaultValues[key] = default
        }
    }

    /**
     * Fetch remote values from Remote Config
     */
    suspend fun fetch() = suspendCoroutine<Unit> { continuation ->
        Firebase.remoteConfig.fetch()
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
            }
    }

    /**
     * Activate fetched remote values
     */
    suspend fun activate() = suspendCoroutine<Boolean> { continuation ->
        Firebase.remoteConfig.activate()
            .addOnSuccessListener {
                continuation.resume(it)
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
            }
    }
}
